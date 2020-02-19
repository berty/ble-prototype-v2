package com.example.ble;

import android.bluetooth.le.ScanCallback;
import android.util.Log;

// TODO : implementation of onBatchScanResults to improve performances
// see https://stackoverflow.com/questions/27040086/onbatchscanresults-is-not-called-in-android-ble

public class ScanResults extends ScanCallback {
    private static final String TAG = ScanResults.class.getSimpleName();

    public void onScanFailed(int errorCode) {
        Log.e(TAG, "ble scanning couldn't be started");
    }

    public void onScanResult(int callbackType, ScanResults result) {
        Log.d(TAG, "scanResult processing");
    }
}
