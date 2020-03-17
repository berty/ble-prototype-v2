package com.example.ble;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class BleDriverStartStopTest {
    BleDriver mBleDriver = BleDriver.getInstance();

    @Test
    public void startAndStop() {
        assertNotNull(mBleDriver);
        assertTrue(mBleDriver.StartBleDriver(UUID.randomUUID().toString()));
        mBleDriver.StopBleDriver();
    }

    @Test
    public void multipleStartAndStop() {
        for (int i = 0; i < 3; i++) {
            startAndStop();
        }
    }
}
