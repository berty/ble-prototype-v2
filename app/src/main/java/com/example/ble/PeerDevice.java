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

public class PeerDevice {
    private static final String TAG = PeerDevice.class.getSimpleName();

    public static final String ACTION_STATE_CONNECTED = "peerDevice.STATE_CONNECTED";
    public static final String ACTION_STATE_DISCONNECTED = "peerDevice.STATE_DISCONNECTED";

    private int mState = BluetoothProfile.STATE_DISCONNECTED;

    private Context mContext;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;

    private Runnable mRunnable;

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

    public boolean asyncConnectionToDevice(String caller) {
        Log.d(TAG, "asyncConnectionToDevice: caller: " + caller);
        if (!isConnected()) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false,
                            mGattCallback);
                }
            };
            mRunnable.run();
        }
        return false;
    }

    public boolean isConnected() {
        return mState == BluetoothProfile.STATE_CONNECTED;
    }

    public void setState(int state) {
        mState = state;
    }

    private BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    Log.d(TAG, "onConnectionStateChange() called in thread " + Thread.currentThread().getName());
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "onConnectionStateChange(): connected");
                        setState(BluetoothProfile.STATE_CONNECTED);
                        mBluetoothGatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "onConnectionStateChange(): disconnected");
                        setState(BluetoothProfile.STATE_DISCONNECTED);
                    } else {
                        Log.e(TAG, "onConnectionStateChange(): unknown state");
                    }
                }
                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    Log.d(TAG, "onServicesDiscovered(): called");
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
                                }
                            }
                            break ;
                        }
                    }
                }
            };
}
