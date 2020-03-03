package com.example.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

public class GattServerCallback extends BluetoothGattServerCallback {
    private static final String TAG = GattServerCallback.class.getSimpleName();

    private Context mContext;
    private GattServer mGattServer;

    // Static variable used to check if the whole peerID was read by remote device
    static int readCount = 0;

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
        if (characteristic.getUuid().equals(GattServer.PEER_ID_UUID)) {
            String peerID = characteristic.getStringValue(0);
            Log.d(TAG, "onCharacteristicReadRequest(): peerID is " + peerID);
            byte[] value = Arrays.copyOfRange(peerID.getBytes(), offset, peerID.length());
            Log.d(TAG, "onCharacteristicReadRequest(): offset: " + offset + " value: " + value);
            mGattServer.getGattServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            PeerDevice peerDevice = DeviceManager.get(device.getAddress());
            if (peerDevice == null) {
                Log.e(TAG, "onCharacteristicReadRequest error: device not found in the DeviceManager");
                return ;
            }
            peerDevice.setReadServerPeerID(true);
        }
    }
}
