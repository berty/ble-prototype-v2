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

    // Scanning
    // API level 21
    // Scanner is the implementation of the ScanCallback abstract class
    private ScanFilter mScanFilter = Scanner.buildScanFilter();
    private ScanSettings mScanSettings = Scanner.BuildScanSettings();
    private Scanner mScanner = new Scanner();
    private BluetoothLeScanner mBluetoothLeScanner;
    private static boolean mScanning;

    // Advertising
    // API level 21
    // Advertiser is the implementation of the AdvertiseCallback abstract class
    private AdvertiseSettings mAdvertiseSettings = Advertiser.buildAdvertiseSettings();
    private AdvertiseData mAdvertiseData = Advertiser.buildAdvertiseData();
    private Advertiser mAdvertiser = new Advertiser();
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private static boolean mAdvertising;

    private boolean mDriverState;

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
        if (mDriverState) {
            Log.d(TAG, "driver is already on");
            return false;
        }
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "StartBleDriver: no bluetooth adapter found");
            return false;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "StartBleDriver: bluetooth is disabled");
            return false;
        }
        mLocalPeerID = localPeerID;
        setAdvertising(true);
        setScanning(true);
        mDriverState = true;
        return true;
    }

    public void StopBleDriver() {
        if (!mDriverState) {
            Log.d(TAG, "driver is already off");
            return ;
        }
        setAdvertising(false);
        setScanning(false);
        mDriverState = false;
    }

    // Method only for test purposes
    public void StartScanning() {
        if (mScanning) {
            Log.d(TAG, "scanning is already on");
            return ;
        }
        setScanning(true);
    }

    // Method only for test purposes
    public void StopScanning() {
        if (!mScanning) {
            Log.d(TAG, "scanning is already off");
            return ;
        }
        setScanning(false);
    }

    // Method only for test purposes
    public void StartAdvertising() {
        if (mAdvertising) {
            Log.d(TAG, "advertising already on");
            return ;
        }
        setAdvertising(true);
    }

    // Method only for test purposes
    public void StopAdvertising() {
        if (!mAdvertising) {
            Log.d(TAG, "advertising already off");
            return ;
        }
        setAdvertising(false);
    }

    // Android only provides a way to know if startScan has failed so we set the scanning state
    // to true and ScanCallback will set it to false in case of failure.
    private void setScanning(boolean enable) {
        if (enable && !getScanningState()) {
            setScanningState(true);
            mBluetoothLeScanner.startScan(Collections.singletonList(mScanFilter), mScanSettings, mScanner);
        } else if (!enable && getScanningState()) {
            setScanningState(false);
            mBluetoothLeScanner.stopScan(mScanner);
        }
    }

    public static void setScanningState(boolean state) {
        mScanning = state;
    }

    // Return the status of the scanner
    // true: scanning is enabled
    // false: scanning is disabled
    public static boolean getScanningState() {
        return mScanning;
    }

    private void setAdvertising(boolean enable) {
        if (enable && !getAdvertisingState()) {
            mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, mAdvertiser);
        } else if (!enable && getAdvertisingState()) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiser);
        }
    }

    public static void setAdvertisingState(boolean state) {
        mAdvertising = state;
    }

    // Return the status of the advertiser
    // true: advertising is enabled
    // false: advertising is disabled
    public static boolean getAdvertisingState() {
        return mAdvertising;
    }

    private BluetoothGatt connectOnDevice(BluetoothDevice device) {
        Log.d(TAG, "connectOnDevice: connecting");
        BluetoothGatt bluetoothGatt = device.connectGatt(mContext, false,
                bluetoothGattCallback);
        return bluetoothGatt;
    }
}
