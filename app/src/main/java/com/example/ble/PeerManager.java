package com.example.ble;

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

    private static class Peer {
        private UUID mPeerID;
        private boolean mIsReady = false;

        public Peer(UUID peerID) {
            mPeerID = peerID;
        }

        public UUID getPeerID() {
            return mPeerID;
        }

        public boolean isReady() {
            return (mIsReady == true);
        }

        // if the device is ready, send a signal to HandlePeerFound
        public void setIsReady(boolean ready) {
            Log.d(TAG, "setIsReady() called: " + ready);
            if (ready && !mIsReady) {
                mIsReady = true;
                // example of signal
                Log.d(TAG, "setIsReady() ok: " + getPeerID());
                Intent intent = new Intent(BleDriver.ACTION_PEER_FOUND);
                intent.putExtra(BleDriver.EXTRA_DATA, getPeerID().toString());
                mContext.sendBroadcast(intent);
            }
        }
    }

    public static synchronized void set(UUID key, boolean ready) {
        Log.d(TAG, "set() called");
        Peer peer;

        if ((peer = mPeers.get(key)) == null) {
            Log.d(TAG, "set(): peer unknown");
            peer = new Peer(key);
            mPeers.put(key, peer);
        } else {
            Log.d(TAG, "set(): peer known");
        }
        peer.setIsReady(ready);
    }
}
