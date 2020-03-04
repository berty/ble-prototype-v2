package com.example.ble;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;
import java.util.UUID;

public class PeerManager {
    private static final String TAG = PeerManager.class.getSimpleName();

    private static HashMap<UUID, Peer> mPeers = new HashMap<>();
    private static Context mContext;

    public PeerManager(Context context) {
        mContext = context;
    }


    public static synchronized void set(UUID key, boolean ready, PeerDevice peerDevice) {
        Log.d(TAG, "set() called");
        Peer peer;

        if ((peer = mPeers.get(key)) == null) {
            Log.d(TAG, "set(): peer unknown");
            peer = new Peer(key, ready, peerDevice);
            mPeers.put(key, peer);
            sendMessage(peerDevice.getPeerID().toString());
        } else {
            Log.d(TAG, "set(): peer known");
            peer.setIsReady(ready);
            peer.setPeerDevice(peerDevice);
        }
    }

    public static synchronized Peer get(UUID key) {
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
