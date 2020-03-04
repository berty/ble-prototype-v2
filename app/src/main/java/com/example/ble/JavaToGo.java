package com.example.ble;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class JavaToGo {
    private static final String TAG = JavaToGo.class.getSimpleName();

    public static final String INTERFACE_FOUND_PEER = "BleDriver.INTERFACE_FOUND_PEER";
    public static final String INTERFACE_EXTRA_DATA = "BleDriver.INTERFACE_EXTRA_DATA";

    private static Context mContext;

    public static boolean handleFoundPeer(String peerID) {
        Log.d(TAG, "handleFoundPeer() called");

        Intent intent = new Intent(INTERFACE_FOUND_PEER);
        intent.putExtra(INTERFACE_EXTRA_DATA, peerID);
        mContext.sendBroadcast(intent);
        BleDriver.SendToPeer(peerID, "Hello World!".getBytes());
        return true;
    }

    public static void setContext(Context context) {
        mContext = context;
    }
}
