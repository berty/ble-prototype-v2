package com.example.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class PeerDevice {
    private static final String TAG = PeerDevice.class.getSimpleName();

    public static final String ACTION_STATE_CONNECTED = "peerDevice.STATE_CONNECTED";
    public static final String ACTION_STATE_DISCONNECTED = "peerDevice.STATE_DISCONNECTED";

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTED = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_DISCONNECTING = 3;
    private int mState = STATE_DISCONNECTED;

    private Context mContext;
    private BluetoothDevice mBluetoothDevice;
    private PeerManager mPeerManager;
    private BluetoothGatt mBluetoothGatt;

    private Thread mThread;
    private final Object mLockState = new Object();
    private final Object mLockReadPeerID = new Object();

    private BluetoothGattService mBertyService;
    private BluetoothGattCharacteristic mPeerIDCharacteristic;
    private BluetoothGattCharacteristic mWriterCharacteristic;

    private UUID mPeerID;
    private boolean mHasReadServerPeerID;
    private boolean mHasReadClientPeerID;

    public PeerDevice(@NonNull Context context, @NonNull BluetoothDevice bluetoothDevice,
                      PeerManager peerManager) {
        mContext = context;
        mBluetoothDevice = bluetoothDevice;
        mPeerManager = peerManager;
    }

    public String getMACAddress() {
        return mBluetoothDevice.getAddress();
    }

    @NonNull
    @Override
    public java.lang.String toString() {
        return getMACAddress();
    }

    // Use TRANSPORT_LE for connections to remote dual-mode devices. This is a solution to prevent the error
    // status 133 in GATT connections:
    // https://android.jlelse.eu/lessons-for-first-time-android-bluetooth-le-developers-i-learned-the-hard-way-fee07646624
    // API level 23
    public boolean asyncConnectionToDevice() {
        Log.d(TAG, "asyncConnectionToDevice() called");

        if (!isConnected()) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    setBluetoothGatt(mBluetoothDevice.connectGatt(mContext, false,
                            mGattCallback, BluetoothDevice.TRANSPORT_LE));
                }
            });
            mThread.start();
        }
        return false;
    }

    public boolean isConnected() {
        return getState() == STATE_CONNECTED;
    }

    public boolean isDisconnected() {
        return getState() == STATE_DISCONNECTED;
    }

    // setters and getters are accessed by the DeviceManager thread et this thread so we need to
    // synchronize them.
    public void setState(int state) {
        synchronized (mLockState) {
            mState = state;
        }
    }

    public int getState() {
        synchronized (mLockState) {
            return mState;
        }
    }

    public void setBluetoothGatt(BluetoothGatt gatt) {
        synchronized (mLockState) {
            mBluetoothGatt = gatt;
        }
    }

    public BluetoothGatt getBluetoothGatt() {
        synchronized (mLockState) {
            return mBluetoothGatt;
        }
    }

    public void setPeerIDCharacteristic(BluetoothGattCharacteristic peerID) {
        mPeerIDCharacteristic = peerID;
    }

    public BluetoothGattCharacteristic getPeerIDCharacteristic() {
        return mPeerIDCharacteristic;
    }

    public void setWriterCharacteristic(BluetoothGattCharacteristic write) {
        mWriterCharacteristic = write;
    }

    public BluetoothGattCharacteristic getWriterCharacteristic() {
        return mWriterCharacteristic;
    }

    public BluetoothGattService getBertyService() {
        return mBertyService;
    }

    public void setBertyService(BluetoothGattService service) {
        mBertyService = service;
    }

    public void setPeerID(UUID peerID) {
        mPeerID = peerID;
    }

    public UUID getPeerID() {
        return mPeerID;
    }

    public void setReadClientPeerID(boolean value) {
        Log.d(TAG, "setReadClientPeerID called: " + value);
        synchronized (mLockReadPeerID) {
            mHasReadClientPeerID = value;
            if (mHasReadServerPeerID) {
                mPeerManager.set(getPeerID(), true);
            } else {
                mPeerManager.set(getPeerID(), false);
            }
        }
    }

    public boolean hasReadClientPeerID() {
        synchronized (mLockReadPeerID) {
            return mHasReadClientPeerID;
        }
    }

    public void setReadServerPeerID(boolean value) {
        Log.d(TAG, "setReadServerPeerID called: " + value);
        synchronized (mLockReadPeerID) {
            mHasReadServerPeerID = value;
            if (mHasReadClientPeerID) {
                mPeerManager.set(getPeerID(), true);
            }
        }
    }

    public boolean hasReadServerPeerID() {
        synchronized (mLockReadPeerID) {
            return mHasReadServerPeerID;
        }
    }

    public void close() {
        if (isConnected()) {
            getBluetoothGatt().close();
            setState(STATE_DISCONNECTING);
        }
    }

    private boolean takeBertyService(List<BluetoothGattService> services) {
        Log.d(TAG, "takeBertyService() called");
        for (BluetoothGattService service : services) {
            if (service.getUuid().equals(BleDriver.SERVICE_UUID)) {
                Log.d(TAG, "takeBertyService(): found");
                setBertyService(service);
                return true;
            }
        }
        return false;
    }

    private boolean checkCharacteristicProperties(BluetoothGattCharacteristic characteristic,
                                                  int properties) {
        if (characteristic.getProperties() == properties) {
            Log.d(TAG, "checkCharacteristicProperties() match");
            return true;
        }
        Log.e(TAG, "checkCharacteristicProperties() doesn't match: " + characteristic.getProperties() + " / " + properties);
        return false;
    }

    private boolean takeBertyCharacteristics() {
        int nbOfFoundCharacteristics = 0;
        List<BluetoothGattCharacteristic> characteristics = mBertyService.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristics) {
            if (characteristic.getUuid().equals(BleDriver.PEER_ID_UUID)) {
                Log.d(TAG, "takeBertyCharacteristic(): peerID characteristic found");
                if (checkCharacteristicProperties(characteristic,
                        BluetoothGattCharacteristic.PROPERTY_READ)) {
                    setPeerIDCharacteristic(characteristic);
                    nbOfFoundCharacteristics++;
                }
            } else if (characteristic.getUuid().equals(BleDriver.WRITER_UUID)) {
                Log.d(TAG, "takeBertyCharacteristic(): writer characteristic found");
                if (checkCharacteristicProperties(characteristic,
                        BluetoothGattCharacteristic.PROPERTY_WRITE)) {
                    setWriterCharacteristic(characteristic);
                    nbOfFoundCharacteristics++;
                }
            }
            if (nbOfFoundCharacteristics == 2) {
                return true;
            }
        }
        return false;
    }

    // Asynchronous operation that will be reported to the BluetoothGattCallback#onCharacteristicRead
    // callback.
    public boolean requestPeerIDValue() {
        Log.v(TAG, "requestPeerIDValue() called");
        if (!mBluetoothGatt.readCharacteristic(getPeerIDCharacteristic())) {
            Log.e(TAG, "requestPeerIDValue() error");
            return false;
        }
        return true;
    }

    private BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    Log.d(TAG, "onConnectionStateChange() called by device " + gatt.getDevice().getAddress());
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "onConnectionStateChange(): connected");
                        setState(STATE_CONNECTED);
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "onConnectionStateChange(): disconnected");
                        setState(STATE_DISCONNECTED);
                        setBluetoothGatt(null);
                    } else {
                        Log.e(TAG, "onConnectionStateChange(): unknown state");
                        setState(STATE_DISCONNECTED);
                        close();
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    Log.d(TAG, "onServicesDiscovered(): called");
                    if (takeBertyService(gatt.getServices())) {
                        if (takeBertyCharacteristics()) {
                            requestPeerIDValue();
                        }
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    Log.d(TAG, "onCharacteristicRead() called");
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        Log.e(TAG, "onCharacteristicRead(): read error");
                        gatt.close();
                    }
                    if (characteristic.getUuid().equals(BleDriver.PEER_ID_UUID)) {
                        String peerID;
                        if ((peerID = characteristic.getStringValue(0)) == null
                                || peerID.length() == 0) {
                            Log.e(TAG, "takePeerID() error: peerID is null");
                        } else {
                            setPeerID(UUID.fromString(peerID));
                            Log.i(TAG, "takePeerID(): peerID is " + peerID);
                            setReadClientPeerID(true);
                        }
                    }
                }
            };
}
