package com.zalo.servicetraining.downloader.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.zalo.servicetraining.App;
import com.zalo.servicetraining.downloader.base.BaseTask;
import com.zalo.servicetraining.downloader.base.BaseTaskManager;
import com.zalo.servicetraining.downloader.model.DownloadItem;
import com.zalo.servicetraining.downloader.model.TaskInfo;
import com.zalo.servicetraining.downloader.service.notification.DownloadNotificationManager;
import com.zalo.servicetraining.downloader.task.simple.SimpleTaskManager;

import java.util.ArrayList;

public class DownloaderService extends Service implements BaseTaskManager.CallBack, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "DownloaderService";

    public static final String PACKAGE_NAME = "com.zalo.servicetraining.downloader.mService";

    private static final int UPDATE_FROM_TASK = 2;

    public static int DOWNLOAD_MODE_SIMPLE = 0;
    public static int DOWNLOAD_MODE_PARTIAL = 1;

    public static final String ACTION_TASK_CHANGED = "action_task_changed";
    public static final String ACTION_TASK_CLEAR = "action_task_clear";
    public static final String ACTION_TASK_MANAGER_CHANGED = "action_task_manager_changed";

    private BaseTaskManager mDownloadManager;
    private DownloadNotificationManager mNotificationManager;

    public void initManager() {
        if(mDownloadManager==null) {
            mDownloadManager = new SimpleTaskManager();
            mDownloadManager.init(this);
            mDownloadManager.restoreInstance();
        }
    }

    public void addNewTask(DownloadItem item) {
        if(mDownloadManager==null) initManager();
        mDownloadManager.addNewTask(item);
    }

    @Override
    public void onUpdateTaskManager(BaseTaskManager manager) {
        Intent intent = new Intent();
        intent.setAction(ACTION_TASK_MANAGER_CHANGED);
        Log.d(TAG, "service sends action task manager changed");
        LocalBroadcastManager.getInstance(App.getInstance().getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public void onUpdateTask(BaseTask task) {
       mNotificationManager.notifyTaskNotificationChanged(task);
       Intent intent = new Intent();
       intent.setAction(ACTION_TASK_CHANGED);
        Log.d(TAG, "service sends action task id"+task.getId()+" changed");
        intent.putExtra(BaseTask.EXTRA_TASK_ID,task.getId());
        intent.putExtra(BaseTask.EXTRA_STATE,task.getState());
        intent.putExtra(BaseTask.EXTRA_PROGRESS,task.getProgress());
        intent.putExtra(BaseTask.EXTRA_PROGRESS_SUPPORT, task.isProgressSupport());
        intent.putExtra(BaseTask.EXTRA_DOWNLOADED_IN_BYTES,task.getDownloadedInBytes());
        intent.putExtra(BaseTask.EXTRA_FILE_CONTENT_LENGTH,task.getFileContentLength());
        intent.putExtra(BaseTask.EXTRA_SPEED,task.getSpeedInBytes());
        LocalBroadcastManager.getInstance(App.getInstance().getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public void onClearTask(int id) {
        mNotificationManager.notifyTaskClear(id);
        Intent intent = new Intent();
        intent.setAction(ACTION_TASK_CLEAR);
        intent.putExtra(BaseTask.EXTRA_TASK_ID, id);
        Log.d(TAG, "service sends action task id"+id+" deleted");
        LocalBroadcastManager.getInstance(App.getInstance().getApplicationContext())
                .sendBroadcast(intent);
    }

    public BaseTaskManager getDownloadManager() {
        return mDownloadManager;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        PreferenceManager.getDefaultSharedPreferences(App.getInstance().getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        initManager();
        initNotification();
    }

    private void initNotification() {
        if(mNotificationManager ==null) {
            mNotificationManager = new DownloadNotificationManager();
            mNotificationManager.init(this);
        }
    }

    public void stopForegroundThenStopSelf() {
        stopForeground(true);
        stopSelf();

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mNotificationManager.cancelAll();
        mDownloadManager.destroy();
        mNotificationManager = null;
        mDownloadManager = null;
        PreferenceManager.getDefaultSharedPreferences(App.getInstance().getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null) Log.d(TAG, "onStartCommand: null intent");
        else if(intent.getAction()==null)
        Log.d(TAG, "onStartCommand : receive intent with no action");
        else Log.d(TAG, "onStartCommand: receive intent with action :["+intent.getAction()+"]");
        return super.onStartCommand(intent, flags, startId);
    }
    public void stopIfModeBackground() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O &&mNotificationManager!=null && mNotificationManager.getNotifyMode()== DownloadNotificationManager.NOTIFY_MODE_BACKGROUND)
            stopSelf();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");

        return super.onUnbind(intent);
    }


    public ArrayList<TaskInfo> getSessionTaskList() {
        return mDownloadManager.getSessionTaskList().getList();
    }

    public TaskInfo getTaskInfoWithId(int id) {
       return mDownloadManager.getTaskInfo(id);
    }

    public void pauseTaskWithTaskId(int id) {
        mDownloadManager.pauseTaskFromUser(id);
    }

    public void cancelTaskWithTaskId(int id) {
        mDownloadManager.cancelTaskFromUser(id);
    }

    public void resumeTaskWithTaskId(int id) {
        mDownloadManager.resumeTaskByUser(id);
    }

    public void restartTaskWithTaskId(int id) {
        mDownloadManager.restartTaskByUser(id) ;
    }

    public IBinder mBinder = new Binder();

    public void clearTask(int id) {
        if(mDownloadManager!=null) mDownloadManager.clearTask(id);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }


    public class Binder extends android.os.Binder {
        @NonNull
        public DownloaderService getService() {
            return DownloaderService.this;
        }
    }

}
