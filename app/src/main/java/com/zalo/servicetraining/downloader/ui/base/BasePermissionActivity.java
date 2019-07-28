package com.zalo.servicetraining.downloader.ui.base;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zalo.servicetraining.ui.base.AbsListActivity;

public abstract class BasePermissionActivity extends AbsListActivity {
    private static final String TAG = "BasePermissionActivity";

    private static final int PERMISSION_WRITE_STORAGE = 1;
    private static final String PERMISSION_RESULT = "permission_result";
    private Intent mRequestIntent = null;

    public void executeWriteStorageAction(Intent intent) {
        if(intent==null) return;
        mRequestIntent = intent;
        requestPermission();
    }

    public void onPermissionResult(Intent intent, boolean granted) {

    }

    private void onPermissionResult(boolean granted) {
        if(mRequestIntent!=null) {
            mRequestIntent.putExtra(PERMISSION_RESULT, granted);
            Intent intent = mRequestIntent;
            mRequestIntent = null;
            onPermissionResult(intent, granted);
        }
    }

    public boolean checkSelfPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_WRITE_STORAGE);

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        PERMISSION_WRITE_STORAGE);

            }
        } else onPermissionResult(true);
    }

/*    @Override
    public final void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResult) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case PERMISSION_WRITE_STORAGE:
                if (grantResult.length > 0) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        // Granted
                        onPermissionResult(true);
                    } else onPermissionResult(false);
                }
                break;
        }
    }*/

    public final void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResult) {
        switch (requestCode) {
            case PERMISSION_WRITE_STORAGE:
                if(permissions.length >0&& grantResult.length>0)
                        onPermissionResult(grantResult[0]==PackageManager.PERMISSION_GRANTED);
                break;
        }
    }
}
