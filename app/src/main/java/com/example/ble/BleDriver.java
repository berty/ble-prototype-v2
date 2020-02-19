package com.example.ble;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Collections;
import java.util.UUID;

public class BleDriver {
    private final String TAG = BleDriver.class.getSimpleName();

    static final UUID SERVICE_UUID = UUID.fromString("A06C6AB8-886F-4D56-82FC-2CF8610D668D");
    static final UUID PEER_ID_UUID = UUID.fromString("0EF50D30-E208-4315-B323-D05E0A23E6B5");
    static final UUID WRITER_UUID = UUID.fromString("000CBD77-8D30-4EFF-9ADD-AC5F10C2CC1B");
    static final ParcelUuid P_SERVICE_UUID = new ParcelUuid(SERVICE_UUID);

    private String mLocalPeerID;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;

    // API required is level 21 because we call BluetoothLeScanner instead of BluetoothAdapter.startLeScan
    // Scan
    private ScanFilter mScanFilter = new ScanFilter.Builder().setServiceUuid(P_SERVICE_UUID).build();
    private ScanSettings mScanSettings =
            new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
    private ScanResults mScanResults = new ScanResults();
    private BluetoothLeScanner mBluetoothLeScanner;
    // Advertise
    private AdvertiseSettings mAdvertiseSettings = new AdvertiseSettings.Builder()
            .setConnectable(true)
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setTimeout(0)
            .build();
    private AdvertiseData mAdvertiseDate = new AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(BleDriver.P_SERVICE_UUID)
            .build();
    private AdvertiseResults mAdvertiseResults = new AdvertiseResults();
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private boolean mScanning;
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.d(TAG, "onLeScan: one device found");
                    connectOnDevice(device);
                }
            };

    private BluetoothGattCallback bluetoothGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "bluetoothGattCallback: device connected");
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "bluetoothGattCallback: device disconnected");
                    }
                }
            };

    /*
    Get Context by a hacking way
     */
    public BleDriver() {
        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Application application = (Application) activityThreadClass.getMethod("currentApplication").invoke(null);
            mContext = application.getApplicationContext();
        } catch (Exception e) {
            Log.e(TAG, "BleDriver constructor: context not found");
            return ;
        }
        if ((mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) == null) {
            Log.d(TAG, "BleDriver constructor: bluetooth not supported on this hardware platform");
            return ;
        } else {
            Log.d(TAG, "BleDriver constructor: bluetooth is supported on this hardware platform");
        }
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    }

    public boolean StartBleDriver(String localPeerID) {
       if (mBluetoothAdapter == null) {
           Log.d(TAG, "StartBleDriver: no bluetooth adapter found");
           return false;
       }
       if (!mBluetoothAdapter.isEnabled()) {
           Log.d(TAG, "StartBleDriver: bluetooth is disabled");
           return false;
       }
       setScanning(true);
       return true;
    }

    public void StopBleDriver() {
        setScanning(false);
    }

    private void setScanning(boolean enable) {
        if (enable) {
            mScanning = true;
            mBluetoothLeScanner.startScan(Collections.singletonList(mScanFilter), mScanSettings, mScanResults);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanResults);
        }
    }

    private void setAdvertising(boolean enable) {

    }

    private BluetoothGatt connectOnDevice(BluetoothDevice device) {
        Log.d(TAG, "connectOnDevice: connecting");
        BluetoothGatt bluetoothGatt = device.connectGatt(mContext, false,
                bluetoothGattCallback);
        return bluetoothGatt;
    }
}
