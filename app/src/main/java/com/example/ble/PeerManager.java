package com.example.ble;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;

public class PeerManager {
    private static final String TAG = "PeerManager";

    private static HashMap<String, Peer> mPeers = new HashMap<>();
    private static Context mContext;

    public PeerManager(Context context) {
        mContext = context;
    }


    public static synchronized void set(String key, boolean ready, PeerDevice peerDevice) {
        Log.d(TAG, "set() called");
        Peer peer;

        if ((peer = mPeers.get(key)) == null) {
            Log.d(TAG, "set(): peer unknown");
            peer = new Peer(key, ready, peerDevice);
            mPeers.put(key, peer);
        } else {
            Log.d(TAG, "set(): peer known");
            peer.setIsReady(ready);
            peer.setPeerDevice(peerDevice);
        }
        if (ready && !peer.isAlreadyFound()) {
            peer.setAlreadyFound(true);
            sendMessage(peerDevice.getPeerID().toString());
        }
    }

    public static synchronized Peer get(String key) {
        return mPeers.get(key);
    }

    private static void sendMessage(String peerID) {
        // example of signal
        Log.d(TAG, "sendMessage() ok: " + peerID);
        Intent intent = new Intent(BleDriver.ACTION_PEER_FOUND);
        intent.putExtra(BleDriver.EXTRA_DATA, peerID);
        mContext.sendBroadcast(intent);
    }
}
