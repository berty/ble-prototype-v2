package com.example.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class HandleFoundPeerTest {

    private BleDriver mBleDriver;

    @Before
    public void init() {
        assertTrue(mBleDriver.StartBleDriver(UUID.randomUUID().toString()));
    }

    @Test
    public void foundOnePeerTest() {
        // Wait that scanning is finished
        testUtils.mSleep(testUtils.WAIT_FOR_SCANNING);
        assertNotNull(MainActivity.dataArray.get(0));
        //assertEquals(new StringBuilder(HELLO_WORLD).reverse(), data);
        System.out.println("data: " + MainActivity.dataArray.get(0));
    }

}
