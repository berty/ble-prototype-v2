package com.example.ble;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class JavaToGo {
    private static final String TAG = "JavaToGo";

    public static final String INTERFACE_UPDATE_DATA = "BleDriver.INTERFACE_UPDATE_DATA";

    private static Context mContext;

    public static boolean HandleFoundPeer(String peerID) {
        Log.d(TAG, "handleFoundPeer() called");

        MainActivity.dataArray.add("peerID: " + peerID);
        sendUpdateSignal();
        //BleDriver.SendToPeer(peerID, "Hello World!!!!!! If this callback is invoked while a reliable write transaction is in progress, the value of the characteristic represents the value reported by the remote device. An application should compare this value to the desired value to be written. If the values don't match, the application must abort the reliable write transaction.".getBytes());
        //BleDriver.SendToPeer(peerID, "Hello World!!!!!!!ab".getBytes());
        return true;
    }

    public static void ReceiveFromPeer(String remotePID, byte[] payload) {
        Log.d(TAG, "ReceiveFromPeer() called");

        String data = new String(payload);
        MainActivity.dataArray.add("data: " + data);
        Log.d(TAG, "ReceiveFromPeer(): payload=" + data);
        sendUpdateSignal();
    }

    public static void setContext(Context context) {
        mContext = context;
    }

    private static void sendUpdateSignal() {
        Intent intent = new Intent(INTERFACE_UPDATE_DATA);
        mContext.sendBroadcast(intent);
    }
}
