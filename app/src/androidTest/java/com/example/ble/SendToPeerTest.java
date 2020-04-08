package com.example.ble;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class SendToPeerTest {

    public static final String data = "Hello World";

    private String mPeerID;

    @Before
    public void init() {
        assertNotNull(BleDriver.StartBleDriver(UUID.randomUUID().toString()));
        testUtils.mSleep(testUtils.WAIT_FOR_SCANNING);
        assertNotNull(MainActivity.dataArray.get(0));
        assertNotNull(mPeerID = testUtils.parse(MainActivity.dataArray.get(0), JavaToGo.PEER_ID_PREFIX));
    }

    @Test
    public void SendToPeerTest() {
       BleDriver.SendToPeer(mPeerID, data.getBytes());
       //testUtils.mSleep(testUtils.WAIT_FOR_SCANNING);
    }
}
