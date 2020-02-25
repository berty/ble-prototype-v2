package com.example.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.List;

// TODO : implementation of onBatchScanResults to improve performances
// see https://stackoverflow.com/questions/27040086/onbatchscanresults-is-not-called-in-android-ble

public class Scanner extends ScanCallback {
    private static final String TAG = Scanner.class.getSimpleName();

    private Context mContext;
    private DeviceManager mDeviceManager;

    public Scanner (Context context, DeviceManager deviceManager) {
        mContext = context;
        mDeviceManager = deviceManager;
        mContext.registerReceiver(mBroadcastReceiver, buildIntentFilter());
    }

    static ScanSettings BuildScanSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
    }

    static ScanFilter buildScanFilter() {
        return new ScanFilter.Builder()
                .setServiceUuid(BleDriver.P_SERVICE_UUID)
                .build();
    }

    static IntentFilter buildIntentFilter() {
        IntentFilter intent = new IntentFilter();
        intent.addAction(PeerDevice.ACTION_STATE_CONNECTED);
        intent.addAction(PeerDevice.ACTION_STATE_DISCONNECTED);
        return intent;
    }

    @Override
    protected void finalize() {
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onScanFailed(int errorCode) {
        String errorString;
        boolean scanning = false;

        switch(errorCode) {
            case SCAN_FAILED_ALREADY_STARTED: errorString = "SCAN_FAILED_ALREADY_STARTED";
                scanning = true;
                break;

            case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED: errorString = "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";
                break;

            case SCAN_FAILED_INTERNAL_ERROR: errorString = "SCAN_FAILED_INTERNAL_ERROR";
                break;

            case SCAN_FAILED_FEATURE_UNSUPPORTED: errorString = "SCAN_FAILED_FEATURE_UNSUPPORTED";
                break;

            default: errorString = "UNKNOWN SCAN FAILURE (" + errorCode + ")";
                break;
        }
        Log.e(TAG, "onScanFailed: " + errorString);
        BleDriver.setScanningState(scanning);
        super.onScanFailed(errorCode);
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        Log.d(TAG, "onScanResult called with result: " + result);
        parseResult(result);
        super.onScanResult(callbackType, result);
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        Log.v(TAG, "onBatchScanResult() called with results: " + results);

        for (ScanResult result:results) {
            parseResult(result);
        }
        super.onBatchScanResults(results);
    }

    private void parseResult(ScanResult result) {
        Log.v(TAG, "parseResult() called with device: " + result.getDevice());

        BluetoothDevice device = result.getDevice();
        PeerDevice peerDevice = mDeviceManager.get(device.getAddress());

        if (peerDevice == null) {
            Log.i(TAG, "parseResult() scanned a new device: " + device.getAddress());
            peerDevice = new PeerDevice(mContext, device);
            mDeviceManager.addDevice(peerDevice);

            // Everything is handled in this method: GATT connection/reconnection and handshake if necessary
            peerDevice.asyncConnectionToDevice("parseResult(), unknown device");
        } else if (!peerDevice.isConnected()) {
            peerDevice.asyncConnectionToDevice("parseResult(), known device");
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String macAddress = intent.getStringExtra(BleDriver.EXTRA_DATA);
            PeerDevice device;
            if ((device = mDeviceManager.get(macAddress)) == null) {
                Log.e(TAG, "onReceive error: unknown device");
                return ;
            }
            if (action.equals(PeerDevice.ACTION_STATE_CONNECTED)) {
                device.setState(BluetoothProfile.STATE_CONNECTED);
            } else if (action.equals(PeerDevice.ACTION_STATE_DISCONNECTED)) {
                device.setState(BluetoothProfile.STATE_DISCONNECTED);
            }
        }
    };
}
