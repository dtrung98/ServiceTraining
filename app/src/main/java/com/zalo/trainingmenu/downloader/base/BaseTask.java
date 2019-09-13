package com.zalo.trainingmenu.downloader.base;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.zalo.trainingmenu.downloader.model.DownloadItem;

import java.lang.ref.WeakReference;

public abstract class BaseTask<T extends BaseTaskController> implements Task {
    private static final String TAG = "BaseTask";

    private int mMode = EXECUTE_MODE_NEW_DOWNLOAD;
    private final int mId;
    private String mMessage = "";

    private final T mTaskManager;

    private float mProgress = 0;

    private long mDownloadedInBytes = 0;
    private long mFileContentLength = -1;
    private final long mCreatedTime;

    private long mFirstExecutedTime = -1;

    @Override
    public long getLastExecutedTime() {
        return mLastExecutedTime;
    }

    private long mLastExecutedTime = -1;

    private long mFinishedTime = -1;
    private long mRunningTime = 0;
    private String mFileTitle ="";
    private String mDirectory = "";
    private final String mURLString;

    private float mSpeedInBytes = 0;

    protected void setFileTitle(String name) {
        mFileTitle = name;
    }

    protected void setDirectory(String directory) {
        mDirectory = directory;
    }

    @Override
    public long getCreatedTime() {
        return mCreatedTime;
    }

    @Override
    public long getFirstExecutedTime() {
        return mFirstExecutedTime;
    }

    @Override
    public long getFinishedTime() {
        return mFinishedTime;
    }

    public void setFinishedTime(long finishedTime) {
        mFinishedTime = finishedTime;
    }

    @Override
    public long getRunningTime() {
        if(!mStopped) return mRunningTime + (System.currentTimeMillis() - mLastExecutedTime);
        return mRunningTime;
    }

    public void setRunningTime(long runningTime) {
        mRunningTime = runningTime;
    }

    @Override
    public String getFileTitle() {
        return mFileTitle;
    }

    @Override
    public String getDirectory() {
        return mDirectory;
    }

    @Override
    public String getURLString() {
        return mURLString;
    }

    public BaseTask(final int id, T t, DownloadItem item) {
        mId = id;
        mTaskManager = t;

        mURLString = item.getUrlString();
        mCreatedTime = System.currentTimeMillis();

        mFileTitle = item.getFileTitle();
        mDirectory = item.getDirectoryPath();
        mIsAutoGeneratedTitle = item.isAutogeneratedTitle();
        mIsAutoGeneratedPath = item.isAutogeneratedPath();
    }

    private final boolean mIsAutoGeneratedTitle;
    private final boolean mIsAutoGeneratedPath;

    @Override
    public boolean isAutogeneratedTitle() {
        return mIsAutoGeneratedTitle;
    }

    @Override
    public boolean isAutogeneratedPath() {
        return mIsAutoGeneratedPath;
    }

    protected BaseTask(final int id, T t, String directory, String url, long createdTime, String fileTitle, boolean isAutoGeneratedTitle, boolean isAutoGeneratedPath) {
        mId = id;
        mTaskManager = t;
        mDirectory = directory;
        mURLString = url;
        mCreatedTime = createdTime;
        mFileTitle = fileTitle;
        mIsAutoGeneratedPath = isAutoGeneratedPath;
        mIsAutoGeneratedTitle = isAutoGeneratedTitle;
    }

    public T getTaskManager() {
        return mTaskManager;
    }



    protected synchronized void setState(int mState) {
        this.mState = mState;
        setMessage("");
    }

    protected synchronized void setState(int state, String message) {
        this.mState = state;
        setMessage(message);
        //Log.d(TAG, "setState with message: "+ message);
    }

    private int mState = PENDING;

    @Override
    public synchronized float getProgress() {
        return mProgress;
    }

    @Override
    public synchronized int getProgressInteger() {
        return (int) (mProgress*100);
    }

    /**
     * This class must not be called or override by subclasses
     * <br>Please use {@link #runTask} instead
     */
    @Override
    public void run() {
        recordProperties();
        startNotifier();

        runTask();

        stopRecord();
        releaseSafely();
    }

    public abstract void runTask();

    private void recordProperties() {
        mLastExecutedTime = System.currentTimeMillis();

        switch (getMode()) {
            case EXECUTE_MODE_NEW_DOWNLOAD:
            case EXECUTE_MODE_RESTART:
                mFirstExecutedTime = mLastExecutedTime;
                mRunningTime = 0;
                break;
            case EXECUTE_MODE_RESUME:
                if (mFirstExecutedTime == -1) mFirstExecutedTime = mLastExecutedTime;
        }
    }

    private void stopRecord() {
        mFinishedTime = System.currentTimeMillis();
        mRunningTime += (mFinishedTime - mLastExecutedTime);
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized int getId() {
        return mId;
    }
    private boolean mFirstTime = true;
    private long mLastUpdatedSpeedTime = 0;
    private long mLastUpdatedSpeedDownloaded = 0;
    private boolean mProgressUpdateFlag = false;
    public synchronized void lockUpdateFlag() {
        mProgressUpdateFlag = true;
    }
    public synchronized void unlockUpdateFlag() {
        mProgressUpdateFlag = false;
    }
    public synchronized boolean isUpdateFlagLocked() {
        return mProgressUpdateFlag;
    }



    protected synchronized void notifyTaskChanged(){

        if(mNotifyHandler==null) {
            Log.d(TAG, "notify handler is null, let's update task directly");
            getTaskManager().notifyTaskChanged(this);
        }
        else {
            // Nếu chưa có order nào, thì hãy đợi 500s sau, t sẽ gửi
            if (!isUpdateFlagLocked()) {
                mNotifyHandler.sendEmptyMessageDelayed(TASK_CHANGED, 550);
                lockUpdateFlag();
                Log.d(TAG, "thread id "+Thread.currentThread().getId()+": task id " + mId + " orders to update, plz wait for 1250ms");
            } else if (mFirstTime) {
                mFirstTime = false;
                mNotifyHandler.sendEmptyMessage(TASK_CHANGED);
                Log.d(TAG, "thread id "+Thread.currentThread().getId()+": task id "+ mId+" run the first time, update now");
            } else {
                // Nếu đã có order
                // bỏ qua
                Log.d(TAG, "thread id "+Thread.currentThread().getId()+": task id " + mId + " is ignored, task will update soon");
            }
        }
    }
    private long mReleaseCommandTime = 0;

    private synchronized void notifyThenRelease(){
        if(mNotifyHandler!=null)
        {
          //  mNotifyHandler.removeCallbacksAndMessages(null);
            mReleaseCommandTime = System.currentTimeMillis();
            mNotifyHandler.sendEmptyMessageDelayed(TASK_RELEASE, 1250);
        }
    }

    private boolean mStopped = true;
    private synchronized void releaseSafely() {
        mStopped = true;
        notifyThenRelease();
    }

    private synchronized boolean shouldRelease() {
        return mStopped;
    }

    private void release() {
        if (mNotifyHandler!= null) {
            final Looper looper = mNotifyHandler.getLooper();
            looper.quitSafely();
            mNotifyHandler = null;
        }

        if(mNotifyThread!=null) {
            mNotifyThread.quitSafely();
            mNotifyThread = null;
        }

    }
    private HandlerThread mNotifyThread;
    private NotifyHandler mNotifyHandler;

    private void startNotifier(){
        release();
        unlockUpdateFlag();
        mStopped = false;
        mNotifyThread = new HandlerThread("HandlerThread"+getId());
        mNotifyThread.start();
        mNotifyHandler = new NotifyHandler(this, mNotifyThread.getLooper());
    }


    public synchronized String getMessage() {
        return mMessage;
    }

    public synchronized void setMessage(String message) {
        mMessage = message;
    }

    public synchronized boolean isProgressSupport() {
        return mFileContentLength >0;
    }

    private boolean mUserCancelledFlag = false;

    private boolean mUserPauseFlag = false;

    private synchronized boolean isPausedOrCancelled() {
        return !mUserPauseFlag && !mUserCancelledFlag;
    }

    private void clearUserFlag() {
        mUserCancelledFlag = false;
        mUserPauseFlag = false;
    }

    final void pauseByUser() {
        if(isPausedOrCancelled()) mUserPauseFlag = true;
        if(getState()==PENDING) {
            setState(PAUSED);
            notifyTaskChanged();
        }
    }

    final void cancelByUser() {
        if(isPausedOrCancelled()) mUserCancelledFlag = true;
        if(getState()==PENDING) {
            setState(CANCELLED);
            notifyTaskChanged();
        }
    }

    public synchronized final boolean isStopByUser() {
         if(mUserCancelledFlag) {
            setState(CANCELLED);
            notifyTaskChanged();
            return true;
        }
        if(mUserPauseFlag) {
             setState(PAUSED);
             notifyTaskChanged();
             return true;
         }

        return false;
    }

    @Override
    public synchronized long getDownloadedInBytes() {
        return mDownloadedInBytes;
    }

    public synchronized void setDownloadedInBytes(long downloadedInBytes) {
        mDownloadedInBytes = downloadedInBytes;
    }

    public synchronized void appendDownloadedBytes(long bytes) {
        setDownloadedInBytes(getDownloadedInBytes()+ bytes);
        updateProgress();
    }

    protected synchronized void updateProgress() {
        if(isProgressSupport()) {
            float newProgress = (mDownloadedInBytes+0.0f)/mFileContentLength;
            if(newProgress>1) newProgress = 0.99f;
            else if(newProgress<0) newProgress = 0f;

            if(newProgress!=mProgress) {
                mProgress = newProgress;
                if (mProgress == 1) setState(SUCCESS);
                notifyTaskChanged();
            }
        }
    }

    @Override
    public synchronized long getFileContentLength() {
        return mFileContentLength;
    }

    public synchronized void setFileContentLength(long fileContentLength) {
        mFileContentLength = fileContentLength;
    }

    @Override
    public synchronized int getMode() {
        return mMode;
    }

    public synchronized void setMode(int mode) {
        mMode = mode;
    }

    public synchronized void resumeByUser() {
        if(getState()==PAUSED) {
            setMode(EXECUTE_MODE_RESUME);
            setState(PENDING);
            clearUserFlag();
            getTaskManager().executeExistedTask(this);
        }
    }

    public synchronized void restartByUser() {
        setMode(EXECUTE_MODE_RESTART);
        setDownloadedInBytes(0);
        setState(PENDING);
        clearUserFlag();
        getTaskManager().executeExistedTask(this);
    }

    public synchronized float getSpeedInBytes() {
        return mSpeedInBytes;
    }


    protected void restoreProgress(float progress) {
        mProgress = progress;
    }

    protected void restoreFirstExecutedTime(long firstExecutedTime) {
        mFirstExecutedTime = firstExecutedTime;
    }

    protected void restoreLastExecutedTime(long lastExecutedTime) {
        mLastExecutedTime = lastExecutedTime;
    }

    private static class NotifyHandler extends Handler {
        private final WeakReference<BaseTask> mWeakRefTask;
        NotifyHandler(BaseTask task, Looper looper) {
            super(looper);
            mWeakRefTask = new WeakReference<>(task);
        }

        @Override
        public void handleMessage(Message msg) {
            BaseTask task = mWeakRefTask.get();
            if(task==null) return;
            switch (msg.what) {
                case TASK_CHANGED:
                   task.unlockUpdateFlag();
                    Log.d(TAG, "thread id "+Thread.currentThread().getId()+": task id "+task.mId+" is updating with progress "+task.getProgress());
                    // run in HandlerThread
                    task.getTaskManager().notifyTaskChanged(task);
                    break;
                case TASK_RELEASE:

                    task.unlockUpdateFlag();
                    Log.d(TAG, "thread id "+Thread.currentThread().getId()+": task id "+task.mId+" will be released with progress "+task.getProgress()+", delayed "+(System.currentTimeMillis() - task.mReleaseCommandTime));
                    // run in HandlerThread
                    task.getTaskManager().notifyTaskChanged(task);
                       task.release();
            }
        }
    }


    public synchronized void initSpeed() {
        mLastUpdatedSpeedTime = System.currentTimeMillis();
        mLastUpdatedSpeedDownloaded = getDownloadedInBytes();
        mSpeedInBytes = 0;
    }

    public synchronized void calculateSpeed() {

        long currentDownloaded = getDownloadedInBytes();
        long currentTime = System.currentTimeMillis();

        if(currentTime-mLastUpdatedSpeedTime<800) return;

         mSpeedInBytes = (currentDownloaded - mLastUpdatedSpeedDownloaded +0.0f)/(currentTime- mLastUpdatedSpeedTime)*1000;
         mLastUpdatedSpeedTime = currentTime;
         mLastUpdatedSpeedDownloaded = currentDownloaded;
    }
}
