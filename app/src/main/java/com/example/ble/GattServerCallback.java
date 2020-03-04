package com.example.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.GeneratedAdapter;

import java.util.Arrays;
import java.util.UUID;

public class GattServerCallback extends BluetoothGattServerCallback {
    private static final String TAG = GattServerCallback.class.getSimpleName();

    private Context mContext;
    private GattServer mGattServer;

    public GattServerCallback(Context context, GattServer gattServer) {
        mContext = context;
        mGattServer = gattServer;
    }

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
        Log.d(TAG, "onServiceAdded() called in thread " + Thread.currentThread().getName());
        super.onServiceAdded(status, service);
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "onServiceAdded error: failed to add service " + service);
            mGattServer.stop();
        }
        // Set the status server state to true (enabled)
        mGattServer.setState(true);
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        Log.v(TAG, "onConnectionStateChange() called with device: " + device + " with newState: " + newState);

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.v(TAG, "connected");
            PeerDevice peerDevice = DeviceManager.get(device.getAddress());

            if (peerDevice == null) {
                Log.i(TAG, "onConnectionStateChange(): a new device is connected: " + device.getAddress());
                peerDevice = new PeerDevice(mContext, device);
                DeviceManager.addDevice(peerDevice);
            }
            if (peerDevice.isDisconnected()) {
                peerDevice.setState(PeerDevice.STATE_CONNECTING);
                // Everything is handled in this method: GATT connection/reconnection and handshake if necessary
                peerDevice.asyncConnectionToDevice();
            }
        }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device,
                                            int requestId,
                                            int offset,
                                            BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "onCharacteristicReadRequest() called");
        boolean full = false;
        PeerDevice peerDevice;
        byte[] value;
        int length = 0;

        if ((peerDevice = DeviceManager.get(device.getAddress())) == null) {
            Log.e(TAG, "onCharacteristicReadRequest() error: device not found");
            mGattServer.getGattServer().sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE,
                    0, null);
            return ;
        }
        if (characteristic.getUuid().equals(GattServer.PEER_ID_UUID)) {
            String peerID = characteristic.getStringValue(0);
            if ((peerID.length() - offset) <= peerDevice.getMtu() - 1) {
                Log.d(TAG, "onCharacteristicReadRequest(): mtu is big enough");
                full = true;
            } else {
                Log.d(TAG, "onCharacteristicReadRequest(): mtu is too small");
            }
            value = Arrays.copyOfRange(peerID.getBytes(), offset, peerID.length());
            mGattServer.getGattServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            if (full) {
                peerDevice.setReadServerPeerID(true);
            }
        } else {
            mGattServer.getGattServer().sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE,
                    0, null);
        }
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device,
                                             int requestId,
                                             BluetoothGattCharacteristic characteristic,
                                             boolean prepareWrite,
                                             boolean responseNeeded,
                                             int offset,
                                             byte[] value) {
        super.onCharacteristicWriteRequest(device, requestId, characteristic, prepareWrite,
                responseNeeded, offset, value);
        PeerDevice peerDevice;

        if ((peerDevice = DeviceManager.get(device.getAddress())) == null) {
            Log.e(TAG, "onCharacteristicWriteRequest() error: device not found");
            if (responseNeeded) {
                mGattServer.getGattServer().sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE,
                        0, null);
            }
            return ;
        }
        if (characteristic.getUuid().equals(GattServer.WRITER_UUID)) {
            Log.d(TAG, "onCharacteristicWriteRequest(): value is " + new String(value) + " and characteristic value is " + characteristic.getStringValue(0));
        }
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        Log.d(TAG, "onMtuChanged() called: " + mtu);
        PeerDevice peerDevice;
        if ((peerDevice = DeviceManager.get(device.getAddress())) == null) {
            Log.e(TAG, "onMtuChanged() error: device not found");
            return ;
        }
        peerDevice.setMtu(mtu);
    }
}
