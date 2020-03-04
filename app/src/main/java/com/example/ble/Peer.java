package com.example.ble;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.UUID;

public class Peer {
    private static final String TAG = Peer.class.getSimpleName();

    private UUID mPeerID;
    private boolean mIsReady = false;
    private PeerDevice mPeerDevice;

    public Peer(UUID peerID, boolean ready, PeerDevice peerDevice) {
        mPeerID = peerID;
        mIsReady = ready;
        mPeerDevice = peerDevice;
    }

    public synchronized UUID getPeerID() {
        return mPeerID;
    }

    public synchronized boolean isReady() {
        return (mIsReady == true);
    }

    public synchronized void setPeerDevice(PeerDevice peerDevice) {
        mPeerDevice = peerDevice;
    }

    public synchronized PeerDevice getPeerDevice() {
        return mPeerDevice;
    }

    // if the device is ready, send a signal to HandlePeerFound
    public synchronized void setIsReady(boolean ready) {
        Log.d(TAG, "setIsReady() called: " + ready);
        if (ready && !mIsReady) {
            mIsReady = true;
        }
    }
}
