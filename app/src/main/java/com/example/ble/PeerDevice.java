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
    private BluetoothGatt mBluetoothGatt;

    private Thread mThread;
    private final Object mLockState = new Object();

    public PeerDevice(@NonNull Context context, @NonNull BluetoothDevice bluetoothDevice) {
        mContext = context;
        mBluetoothDevice = bluetoothDevice;
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
    public boolean asyncConnectionToDevice(String caller) {
        Log.d(TAG, "asyncConnectionToDevice: caller: " + caller);
        if (!isConnected()) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setName(mBluetoothDevice.getAddress());
                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false,
                            mGattCallback, BluetoothDevice.TRANSPORT_LE);
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

    public void setState(int state) {
        synchronized (mLockState) {
            mState = state;
        }
    }

    public int getState() {
        final int state;
        synchronized (mLockState) {
            state = mState;
        }
        return state;
    }

    public void close() {
        if (isConnected()) {
            mBluetoothGatt.close();
        }
    }

    private BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    Log.d(TAG, "onConnectionStateChange() called in thread " + Thread.currentThread().getName());
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "onConnectionStateChange(): connected");
                        setState(STATE_CONNECTED);
                        mBluetoothGatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "onConnectionStateChange(): disconnected");
                        setState(STATE_DISCONNECTED);
                        mBluetoothGatt = null;
                    } else {
                        Log.e(TAG, "onConnectionStateChange(): unknown state");
                        mBluetoothGatt.close();
                        mBluetoothGatt = null;
                    }
                }
                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    Log.d(TAG, "onServicesDiscovered(): called in thread " + Thread.currentThread().getName());
                    List<BluetoothGattService> services = gatt.getServices();
                    Log.d(TAG, "onServicesDiscovered(): services discovered: " + services);
                    for (BluetoothGattService service : services) {
                        Log.d(TAG, "onServicesDiscovered(): service named " + service.getUuid());
                        if (service.getUuid().equals(BleDriver.SERVICE_UUID)) {
                            Log.i(TAG, "onServicesDiscovered(): Berty service found!");
                            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                            for (BluetoothGattCharacteristic characteristic : characteristics) {
                                if (characteristic.getUuid().equals(BleDriver.PEER_ID_UUID)) {
                                    Log.i(TAG, "onServicesDiscovered(): peerID is " + characteristic.getUuid());
                                    Intent intent = new Intent(BleDriver.ACTION_PEER_FOUND);
                                    intent.putExtra(BleDriver.EXTRA_DATA, mBluetoothDevice.getAddress());
                                    mContext.sendBroadcast(intent);
                                    mBluetoothGatt.close();
                                }
                            }
                            break ;
                        }
                    }
                }
            };
}
