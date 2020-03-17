package com.example.ble;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BleDriverSingletonTest {
    BleDriver mBleDriver;

    @Before
    public void createSingleton() {
        assertNotNull(mBleDriver = BleDriver.getInstance());
    }

    @Test
    public void duplicateSingleton() {
        assertEquals(BleDriver.getInstance().getClass(), mBleDriver.getClass());
    }
}
