package com.example.ble;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class BleDriverStartStopTest {

    BleDriver mBleDriver;

    @Before
    public void init() {
        mBleDriver = BleDriver.getInstance();
    }

    @Test
    public void startAndStop() {
        assertNotNull(mBleDriver);
        assertTrue(mBleDriver.StartBleDriver(UUID.randomUUID().toString()));

        // Starting the service is not instantaneous and dependant of callbacks so we need to wait.
        testUtils.mSleep(testUtils.WAIT_FOR_GATT_SERVER_STARTING);

        assertTrue(mBleDriver.isStarted());
        assertTrue((BleDriver.getScanningState()));
        assertTrue((BleDriver.getAdvertisingState()));

        mBleDriver.StopBleDriver();

        assertFalse(mBleDriver.isStarted());
        assertFalse((BleDriver.getScanningState()));
        assertFalse((BleDriver.getAdvertisingState()));
    }

    @Test
    public void multipleStartAndStop() {
        for (int i = 0; i < 3; i++) {
            startAndStop();
        }
    }
}
