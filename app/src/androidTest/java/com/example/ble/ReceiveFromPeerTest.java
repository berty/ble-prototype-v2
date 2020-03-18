package com.example.ble;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class ReceiveFromPeerTest {

    @Before
    public void init() {
        assertNotNull(BleDriver.StartBleDriver(UUID.randomUUID().toString()));
        testUtils.mSleep(testUtils.WAIT_FOR_RECEIVE);
        assertNotNull(MainActivity.dataArray.get(0));
        assertNotNull(testUtils.parse(MainActivity.dataArray.get(0), JavaToGo.PEER_ID_PREFIX));
    }

    @Test
    public void ReceiveFromPeerTest() {
        assertNotNull(testUtils.parse(MainActivity.dataArray.get(1), JavaToGo.DATA_PREFIX));
    }
}
