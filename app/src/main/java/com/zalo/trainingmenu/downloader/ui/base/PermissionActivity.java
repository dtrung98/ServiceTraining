package com.zalo.trainingmenu.downloader.ui.base;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zalo.trainingmenu.mainui.base.AbsListActivity;

import es.dmoral.toasty.Toasty;

public abstract class PermissionActivity extends AppCompatActivity implements PermissionRequestDialog.RequestResultCallback {
    private static final String TAG = "PermissionActivity";

    private static final int PERMISSION_STORAGE = 1;
    private static final String PERMISSION_RESULT = "permission_result";
    private Intent mRequestIntent = null;
    private boolean mPermissionShown = false;

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
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission() {
        if (!checkSelfPermission()) {

            if (!mPermissionShown && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                PermissionRequestDialog.newInstance().setRequestResultCallback(this).show(getSupportFragmentManager(),PermissionRequestDialog.TAG);
            } else {
                onRequestResult(true);
            }
        } else onPermissionResult(true);
    }

/*    @Override
    public final void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResult) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case PERMISSION_STORAGE:
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
            case PERMISSION_STORAGE:
                if(permissions.length >0&& grantResult.length>0)
                        onPermissionResult(grantResult[0]==PackageManager.PERMISSION_GRANTED);
                break;
        }
    }

    @Override
    public void onRequestResult(boolean result) {

        if(result) {
            mPermissionShown = true;
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    PERMISSION_STORAGE);
        } else {
            Log.d(TAG, "false result");
        }
    }
}
