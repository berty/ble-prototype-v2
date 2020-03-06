package com.example.ble;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.Collections;

public class BleDriver {
    private static final String TAG = "BleDriver";

    static final String ACTION_PEER_FOUND = "BleDriver.ACTION_PEER_FOUND";
    static final String EXTRA_DATA = "BleDriver.EXTRA_DATA";

    private Context mAppContext;
    private BluetoothAdapter mBluetoothAdapter;

    //private final DeviceManager mDeviceManager = new DeviceManager();
    private PeerManager mPeerManager;

    // GATT server
    private GattServer mGattServer;
    private GattServerCallback mGattServerCallback;

    // Scanning
    // API level 21
    // Scanner is the implementation of the ScanCallback abstract class
    private ScanFilter mScanFilter = Scanner.buildScanFilter();
    private ScanSettings mScanSettings = Scanner.BuildScanSettings();
    private Scanner mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private static boolean mScanning;

    // Advertising
    // API level 21
    // Advertiser is the implementation of the AdvertiseCallback abstract class
    private AdvertiseSettings mAdvertiseSettings = Advertiser.buildAdvertiseSettings();
    private AdvertiseData mAdvertiseData = Advertiser.buildAdvertiseData();
    private Advertiser mAdvertiseCallback = new Advertiser();
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private static boolean mAdvertising;

    private enum DRIVER_STATE {
        STOPPED,
        STARTED
    }

    private static final int DRIVER_STATE_NOT_INIT = 0;
    private static final int DRIVER_STATE_INIT = 1;
    private static final int DRIVER_STATE_STARTED = 2;
    private static final int DRIVER_STATE_STOPPED = 3;
    private int mDriverState = DRIVER_STATE_NOT_INIT;

    /*
    Get Context by a hacking way
     */
    public BleDriver() {
        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Application application = (Application) activityThreadClass.getMethod("currentApplication").invoke(null);
            mAppContext = application.getApplicationContext();
        } catch (Exception e) {
            Log.e(TAG, "constructor: context not found");
            return ;
        }
        if ((mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) == null) {
            Log.e(TAG, "constructor: bluetooth not supported on this hardware platform");
            return ;
        } else {
            Log.d(TAG, "constructor: bluetooth is supported on this hardware platform");
        }

        // Setup context dependant objects
        JavaToGo.setContext(mAppContext);
        mPeerManager = new PeerManager(mAppContext);
        mGattServer = new GattServer(mAppContext);
        mGattServerCallback = new GattServerCallback(mAppContext, mGattServer);

        if ((mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner()) == null) {
            Log.i(TAG, "BleDriver constructor: scanning mode not supported");
        }
        if ((mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser()) == null) {
            Log.i(TAG, "BleDriver constructor: advertising mode not supported");
        }
        // is the right place? Context must not be null!
        mScanCallback = new Scanner(mAppContext);
        // Init ok, set driver to the ready state
        mDriverState = DRIVER_STATE_INIT;
    }

    public boolean StartBleDriver(String localPeerID) {
        Log.d(TAG, "StartBleDriver() called in thread " + Thread.currentThread().getName());
        if ((mDriverState != DRIVER_STATE_INIT) && (mDriverState != DRIVER_STATE_STOPPED)) {
            Log.e(TAG, "StartBleDriver(): BLE driver isn't init so it can't be started");
        }
        if (mDriverState == DRIVER_STATE_STARTED) {
            Log.d(TAG, "StartBleDriver(): driver is already on");
            return false;
        }
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "StartBleDriver(): no bluetooth adapter found");
            return false;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "StartBleDriver(): device's bluetooth module is disabled");
            return false;
        }
        if (!mGattServer.start(localPeerID, mGattServerCallback)) {
            Log.e(TAG, "StartBleDriver() error: setup Gatt server");
            return false;
        }
        mAppContext.registerReceiver(mBroadcastReceiver, makeIntentFilter());
        //setAdvertising(true);
        //setScanning(true);
        mDriverState = DRIVER_STATE_STARTED;
        Log.d(TAG, "StartBleDriver: init completed");
        return true;
    }

    public void StopBleDriver() {
        if (mDriverState != DRIVER_STATE_STARTED) {
            Log.d(TAG, "driver isn't started");
            return ;
        }
        mAppContext.unregisterReceiver(mBroadcastReceiver);
        setAdvertising(false);
        setScanning(false);
        DeviceManager.closeAllDeviceConnections();
        mGattServer.stop();
        mDriverState = DRIVER_STATE_STOPPED;
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
        if (mBluetoothLeScanner == null) {
            Log.d(TAG, "setScanning(): abort");
            return ;
        }
        if (enable && !getScanningState()) {
            Log.d(TAG, "setScanning(): enabled");
            mBluetoothLeScanner.startScan(Collections.singletonList(mScanFilter), mScanSettings, mScanCallback);
            setScanningState(true);
        } else if (!enable && getScanningState()) {
            Log.d(TAG, "setScanning(): disabled");
            mBluetoothLeScanner.stopScan(mScanCallback);
            setScanningState(false);
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
        if (mBluetoothLeAdvertiser == null) {
            Log.d(TAG, "setAdvertising(): abort");
            return ;
        }
        if (enable && !getAdvertisingState()) {
            Log.d(TAG, "setAdvertising(): enabled");
            mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, mAdvertiseCallback);
            setAdvertisingState(true);
        } else if (!enable && getAdvertisingState()) {
            Log.d(TAG, "setAdvertising(): disabled");
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            setAdvertisingState(false);
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

    private static IntentFilter makeIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PEER_FOUND);
        return filter;
    }

    public static boolean SendToPeer(String remotePID, byte[] payload) {
        Log.d(TAG, "SendToPeer() called");
        PeerDevice peerDevice;
        BluetoothGattCharacteristic writer;
        BluetoothGatt gatt;

        try {
            peerDevice = PeerManager.get(remotePID).getPeerDevice();
            writer = peerDevice.getWriterCharacteristic();
            writer.setValue(payload);
            gatt = peerDevice.getBluetoothGatt();
            gatt.writeCharacteristic(writer);
        } catch (NullPointerException e) {
            Log.e(TAG, "Failed to get BluetoothGatt for peer: " + remotePID);
            return false;
        }
        return true;
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String peerID = intent.getStringExtra(BleDriver.EXTRA_DATA);
            Log.d(TAG, "onReceive() called: " + action);
            JavaToGo.handleFoundPeer(peerID);
        }
    };
}
