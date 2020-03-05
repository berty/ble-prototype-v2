package com.example.ble;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class JavaToGo {
    private static final String TAG = JavaToGo.class.getSimpleName();

    public static final String INTERFACE_FOUND_PEER = "BleDriver.INTERFACE_FOUND_PEER";
    public static final String INTERFACE_RECEIVE_FROM_PEER = "BleDriver.INTERFACE_RECEIVE_FROM_PEER";
    public static final String INTERFACE_EXTRA_DATA = "BleDriver.INTERFACE_EXTRA_DATA";
    public static final String INTERFACE_EXTRA_DATA_PID = "BleDriver.INTERFACE_EXTRA_DATA_PID";

    private static Context mContext;

    public static boolean handleFoundPeer(String peerID) {
        Log.d(TAG, "handleFoundPeer() called");

        Intent intent = new Intent(INTERFACE_FOUND_PEER);
        intent.putExtra(INTERFACE_EXTRA_DATA_PID, peerID);
        mContext.sendBroadcast(intent);
        BleDriver.SendToPeer(peerID, "Hello World!!!!!! If this callback is invoked while a reliable write transaction is in progress, the value of the characteristic represents the value reported by the remote device. An application should compare this value to the desired value to be written. If the values don't match, the application must abort the reliable write transaction.".getBytes());
        return true;
    }

    public static void ReceiveFromPeer(String remotePID, byte[] payload) {
        Log.d(TAG, "ReceiveFromPeer() called");

        String data = new String(payload);
        Log.d(TAG, "ReceiveFromPeer(): payload=" + data);
        Intent intent = new Intent(INTERFACE_RECEIVE_FROM_PEER);
        intent.putExtra(INTERFACE_EXTRA_DATA_PID, remotePID);
        intent.putExtra(INTERFACE_EXTRA_DATA, data);
        mContext.sendBroadcast(intent);
    }

    public static void setContext(Context context) {
        mContext = context;
    }
}
