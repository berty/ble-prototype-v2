package com.example.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;
import static android.content.Context.BLUETOOTH_SERVICE;

public class GattServer {
    private final String TAG = GattServer.class.getSimpleName();

    // GATT service UUID
    static final UUID SERVICE_UUID = UUID.fromString("A06C6AB8-886F-4D56-82FC-2CF8610D668D");
    static final UUID PEER_ID_UUID = UUID.fromString("0EF50D30-E208-4315-B323-D05E0A23E6B5");
    static final UUID WRITER_UUID = UUID.fromString("000CBD77-8D30-4EFF-9ADD-AC5F10C2CC1B");
    static final ParcelUuid P_SERVICE_UUID = new ParcelUuid(SERVICE_UUID);

    // GATT service objects
    private final BluetoothGattService mService =
            new BluetoothGattService(SERVICE_UUID, SERVICE_TYPE_PRIMARY);
    private final BluetoothGattCharacteristic mPeerIDCharacteristic =
            new BluetoothGattCharacteristic(PEER_ID_UUID, PROPERTY_READ, PERMISSION_READ);
    private final BluetoothGattCharacteristic mWriterCharacteristic =
            new BluetoothGattCharacteristic(WRITER_UUID, PROPERTY_WRITE, PERMISSION_WRITE);

    private Context mContext;
    private BluetoothGattServer mBluetoothGattServer;
    private Thread mGattServerThread;
    private boolean mState;
    private String mPeerID;

    public GattServer(Context context) {
        mContext = context;
    }

    // After adding a new service, the success of this operation will be given to the callback
    // BluetoothGattServerCallback#onServiceAdded. It's only after this callback that the server
    // will be ready.
    public boolean start(final String peerID, final GattServerCallback gattServerCallback) {
        Log.d(TAG, "setupGattServer() called in thread " + Thread.currentThread().getName());
        final BluetoothManager bluetoothManager;

        if (mBluetoothGattServer != null)
            return (true);
        if ((bluetoothManager = (BluetoothManager)mContext.getSystemService(BLUETOOTH_SERVICE)) == null) {
            Log.e(TAG, "setupGattServer(): cannot get the bluetoothManager");
            return false;
        }
        setPeerID(peerID);
        mGattServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mBluetoothGattServer = bluetoothManager.openGattServer(mContext, gattServerCallback);
                if (!setupGattService()) {
                    return ;
                }
                if (!mBluetoothGattServer.addService(mService)) {
                    Log.e(TAG, "setupGattServer() error: cannot add a new service");
                    mBluetoothGattServer = null;
                }
            }
        });
        mGattServerThread.start();
        return true;
    }

    public BluetoothGattServer getGattServer() {
        return mBluetoothGattServer;
    }

    public void setState(boolean state) {
        mState = state;
    }

    public boolean isRun() {
        return mState == true;
    }

    public void stop() {
        if (mBluetoothGattServer != null) {
            mBluetoothGattServer.close();
            mBluetoothGattServer = null;
        }
        setState(false);
    }

    private void setPeerID(String peerID) {
        mPeerID = peerID;
    }

    private String getPeerID() {
        return mPeerID;
    }

    private boolean setupGattService() {
        Log.d(TAG, "setupGattService() called");
        mPeerIDCharacteristic.setValue(getPeerID());
        if (!mService.addCharacteristic(mPeerIDCharacteristic)
                || !mService.addCharacteristic(mWriterCharacteristic)) {
            Log.e(TAG, "setupService() failed: can't add characteristics to service");
            return false;
        }
        return true;
    }

}
