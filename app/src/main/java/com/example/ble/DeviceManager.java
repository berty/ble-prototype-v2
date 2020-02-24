package com.example.ble;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;

public class DeviceManager {
    private static final String TAG = DeviceManager.class.getSimpleName();

    private HashMap<String, PeerDevice> peerDevices = new HashMap<>();

    public PeerDevice put(String key, PeerDevice value) {
        Log.d(TAG, "put() called");
        PeerDevice peerDevice = peerDevices.get(key);
        if (peerDevice == null) {
            Log.d(TAG, "put(): device unknown");
            return peerDevices.put(key, value);
        } else {
            Log.d(TAG, "put(): device already known");
            return peerDevice;
        }
    }

    public PeerDevice get(String key) {
        Log.d(TAG, "get() called");
        PeerDevice peerDevice = peerDevices.get(key);
        if (peerDevice != null) {
            Log.d(TAG, "get(): device found");
        } else {
            Log.d(TAG, "get(): device not found");
        }
        return peerDevice;
    }

    public PeerDevice addDevice(@NonNull PeerDevice peerDevice) {
        String key = peerDevice.getMACAddress();
        return put(key, peerDevice);
    }
}
