package com.example.ble;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;
import java.util.UUID;

public class PeerManager {
    private static final String TAG = PeerManager.class.getSimpleName();

    private HashMap<UUID, Peer> mPeers = new HashMap<>();
    private Context mContext;

    public PeerManager(Context context) {
        mContext = context;
    }

    private class Peer {
        private UUID mPeerID;
        private boolean mIsReady;

        public Peer(UUID peerID, boolean ready) {
            mPeerID = peerID;
            mIsReady = ready;
        }

        public UUID getPeerID() {
            return mPeerID;
        }

        public boolean isReady() {
            return (mIsReady == true);
        }

        // if the device is ready, send a signal to HandlePeerFound
        public void setIsReady(boolean ready) {
            mIsReady = ready;
            if (ready) {
                // example of signal
                Intent intent = new Intent(BleDriver.ACTION_PEER_FOUND);
                intent.putExtra(BleDriver.EXTRA_DATA, getPeerID().toString());
                mContext.sendBroadcast(intent);
            }
        }
    }

    public synchronized Peer put(UUID key, Peer value) {
        Log.d(TAG, "put() called");
        Peer peer = mPeers.get(key);
        if (peer == null) {
            Log.d(TAG, "put(): peer unknown");
            return mPeers.put(key, value);
        } else {
            Log.d(TAG, "put(): peer already known");
            return peer;
        }
    }

    public synchronized void set(UUID key, boolean ready) {
        Log.d(TAG, "set() called");
        Peer peer;

        if ((peer = mPeers.get(key)) == null) {
            Log.d(TAG, "set(): peer unknown");
            peer = new Peer(key, ready);
            mPeers.put(key, peer);
        } else {
            Log.d(TAG, "set(): peer known");
            peer.setIsReady(ready);
        }
    }

    public synchronized Peer get(UUID key) {
        Log.d(TAG, "get() called");
        Peer peer = mPeers.get(key);
        if (peer != null) {
            Log.d(TAG, "get(): peer found");
        } else {
            Log.d(TAG, "get(): peer not found");
        }
        return peer;
    }
}
