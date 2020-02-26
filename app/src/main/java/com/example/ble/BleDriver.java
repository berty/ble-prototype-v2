package com.example.ble;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Collections;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;
import static android.content.Context.BLUETOOTH_SERVICE;

public class BleDriver {
    private final String TAG = BleDriver.class.getSimpleName();

    static final UUID SERVICE_UUID = UUID.fromString("A06C6AB8-886F-4D56-82FC-2CF8610D668D");
    static final UUID PEER_ID_UUID = UUID.fromString("0EF50D30-E208-4315-B323-D05E0A23E6B5");
    static final UUID WRITER_UUID = UUID.fromString("000CBD77-8D30-4EFF-9ADD-AC5F10C2CC1B");
    static final ParcelUuid P_SERVICE_UUID = new ParcelUuid(SERVICE_UUID);

    static final String ACTION_PEER_FOUND = "BleDriver.ACTION_PEER_FOUND";
    static final String EXTRA_DATA = "BleDriver.EXTRA_DATA";

    private Context mAppContext;
    private BluetoothAdapter mBluetoothAdapter;

    private final DeviceManager mDeviceManager = new DeviceManager();

    // GATT service
    private final BluetoothGattService mService =
            new BluetoothGattService(SERVICE_UUID, SERVICE_TYPE_PRIMARY);
    private final BluetoothGattCharacteristic mPeerIDCharacteristic =
            new BluetoothGattCharacteristic(PEER_ID_UUID, PROPERTY_READ, PERMISSION_READ);
    private final BluetoothGattCharacteristic mWriterCharacteristic =
            new BluetoothGattCharacteristic(WRITER_UUID, PROPERTY_WRITE, PERMISSION_WRITE);

    // GATT server
    private BluetoothGattServer mBluetoothGattServer;
    private boolean mGattServerState;
    private Thread mGattServerThread;

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
            Log.e(TAG, "BleDriver constructor: context not found");
            return ;
        }
        if ((mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) == null) {
            Log.d(TAG, "BleDriver constructor: bluetooth not supported on this hardware platform");
            return ;
        } else {
            Log.d(TAG, "BleDriver constructor: bluetooth is supported on this hardware platform");
        }
        if ((mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner()) == null) {
            Log.i(TAG, "BleDriver constructor: scanning mode not supported");
        }
        if ((mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser()) == null) {
            Log.i(TAG, "BleDriver constructor: advertising mode not supported");
        }
        // is the right place? Context must not be null!
        JavaToGo.setContext(mAppContext);
        mScanCallback = new Scanner(mAppContext, mDeviceManager);
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
        if (!setupGattService(localPeerID)) {
            Log.e(TAG, "StartBleDriver() error: setup Gatt service");
            return false;
        }
        if (!setupGattServer()) {
            Log.e(TAG, "StartBleDriver() error: setup Gatt server");
            return false;
        }
        mAppContext.registerReceiver(mBroadcastReceiver, makeIntentFilter());
        setAdvertising(true);
        setScanning(true);
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
        mDeviceManager.closeAllDeviceConnections();
        closeGattServer();
        mDriverState = DRIVER_STATE_STOPPED;
    }

    // test only

    public String getMACAddress() {
        return mBluetoothAdapter.getAddress();
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

    private boolean setupGattService(String localPeerID) {
        Log.d(TAG, "setupGattService() called");
        setLocalPeerID(localPeerID);
        if (((mService.getCharacteristic(PEER_ID_UUID) == null)
                && !mService.addCharacteristic(mPeerIDCharacteristic)) ||
                ((mService.getCharacteristic(WRITER_UUID) == null)
                        && !mService.addCharacteristic(mWriterCharacteristic))) {
            Log.e(TAG, "setupService() failed: can't add characteristics to service");
            return false;
        }
        return true;
    }

    // After adding a new service, the success of this operation will be given to the callback
    // BluetoothGattServerCallback#onServiceAdded. It's only after this callback that the server
    // will be ready.
    private boolean setupGattServer() {
        final BluetoothManager bluetoothManager;

        Log.d(TAG, "setupGattServer() called in thread " + Thread.currentThread().getName());
        if (mBluetoothGattServer != null)
            return (true);
        if ((bluetoothManager = (BluetoothManager)mAppContext.getSystemService(BLUETOOTH_SERVICE)) == null) {
            Log.e(TAG, "setupGattServer(): cannot get the bluetoothManager");
            return false;
        }
        mGattServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mBluetoothGattServer = bluetoothManager.openGattServer(mAppContext, mBluetoothGattServerCallback);
                if (!mBluetoothGattServer.addService(mService)) {
                    Log.e(TAG, "setupGattServer() error: cannot add a new service");
                    mBluetoothGattServer = null;
                }
            }
        });
        mGattServerThread.start();
        return true;
    }

    private void closeGattServer() {
        mBluetoothGattServer.close();
        mBluetoothGattServer = null;
    }

    public void setGattServerState(boolean state) {
        mGattServerState = state;
    }

    public boolean getGattServerState() {
        return mGattServerState;
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

    private void setLocalPeerID(String localPeerID) { mPeerIDCharacteristic.setValue(localPeerID); }

    private String getLocalPeerID() { return mPeerIDCharacteristic.getStringValue(0); }

    private static IntentFilter makeIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PEER_FOUND);
        return filter;
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

    private BluetoothGattServerCallback mBluetoothGattServerCallback =
            new BluetoothGattServerCallback() {
                @Override
                public void onServiceAdded(int status, BluetoothGattService service) {
                    Log.d(TAG, "onServiceAdded() called in thread " + Thread.currentThread().getName());
                    super.onServiceAdded(status, service);
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        Log.e(TAG, "onServiceAdded error: failed to add service " + service);
                        closeGattServer();
                    }
                }
            };
}
