package com.example.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

public class GattServer extends BluetoothGattServerCallback {
    private static final String TAG = GattServer.class.getSimpleName();

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "onServiceAdded error: failed to add service " + service);
        }
        super.onServiceAdded(status, service);
    }
}
