package com.ldt.vrview.transform;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.ldt.vrview.gesture.ViewGestureAttacher;

import java.util.ArrayList;
import java.util.logging.LogRecord;

public final class TransformManager extends BaseTransformer implements TransformListener {
    private static final String TAG = "TransformManager";
    public static final int GESTURE_TRANSFORMER = 1;
    public static final int SENSOR_TRANSFORMER = 2;
    public static final int TRANSFORM_MANAGER = 3;

    private ArrayList<BaseTransformer> mTransformers = new ArrayList<>();
    private SensorTransformer mSensorTransformer;
    private GestureTransformer mGestureTransformer;
    private ValueAnimator mResetAnimator;

    public TransformManager(int id) {
        super(id);
        mGestureTransformer = new GestureTransformer(GESTURE_TRANSFORMER);
        mSensorTransformer = new SensorTransformer(SENSOR_TRANSFORMER);
        mTransformers.add(mGestureTransformer);
        mTransformers.add(mSensorTransformer);
        initResetAnimator();
    }

    private float[] mSavedValues = new float[] {0,0,0,1};

    private void initResetAnimator() {
        if(mResetAnimator!=null) return;
        mResetAnimator = ValueAnimator.ofFloat(0,1);
        mResetAnimator.setDuration(550);
        //mResetAnimator.setInterpolator(new AccelerateInterpolator());
        mResetAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animated = (float)animation.getAnimatedValue();
                mValues[0] = mSavedValues[0] *(1 - animated);
                mValues[1] = mSavedValues[1] *(1 - animated);
                mValues[2] = mSavedValues[2] *(1 - animated);

                // scale
                mValues[3] = 1 + (1- animated)*(mSavedValues[3] - 1);
                notifyTransformChanged();

            }
        });

        mResetAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                System.arraycopy(mValues,0,mSavedValues,0,4);
                for (int i = 0; i < mTransformers.size(); i++) {
                    mTransformers.get(i).reset();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                for (int i = 0; i < mTransformers.size(); i++) {
                    mTransformers.get(i).reset();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    @Override
    public void setViewSize(int width, int height) {
        for (int i = 0; i < mTransformers.size(); i++) {
           mTransformers.get(i).setViewSize(width,height);
        }
    }

    @Override
    public void setTextureSize(float width, float height) {
        super.setTextureSize(width, height);
    }


    @Override
    public void updateTransform() {
        // ignore child value if reset animator is running
        if(mResetAnimator.isRunning()) return;
        float[] v = new float[] {0,0,0,1};
        for (int i = 0; i < mTransformers.size(); i++) {
            v[0]+=mTransformers.get(i).mValues[0];
            v[1]+=mTransformers.get(i).mValues[1];
            v[2]+=mTransformers.get(i).mValues[2];
            v[3]+= mTransformers.get(i).mValues[3] - 1;
        }


        v[0] %= 360;
        v[1] %= 360;
        v[2] %= 360;
        System.arraycopy(v,0,mValues,0,4);
    }

    @Override
    public void reset() {
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(() -> {
            if(!mResetAnimator.isRunning()) mResetAnimator.start();
        });
    }

    @Override
    public void attach(View view) {
        for (int i = 0; i < mTransformers.size(); i++) {
            mTransformers.get(i).setTransformListener(this);
            mTransformers.get(i).attach(view);
        }
    }

    @Override
    public void detach() {
        for (int i = 0; i < mTransformers.size(); i++) {
            mTransformers.get(i).setTransformListener(null);
            mTransformers.get(i).detach();
        }
    }

    public ViewGestureAttacher getGestureAttacher() {
        if(mGestureTransformer!=null) return mGestureTransformer.getGestureAttacher();
        return null;
    }

    @Override
    public void onTransformChanged(int which, float[] values4) {
        updateTransform();
        notifyTransformChanged(which);
    }

    public void notifyTransformChanged(int which) {
        if(getTransformListener() != null) getTransformListener().onTransformChanged(which,mValues);
    }
}
