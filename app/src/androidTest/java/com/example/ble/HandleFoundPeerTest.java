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
    private static final String HELLO_WORLD = "Hello World!";

    public static final int WAIT_FOR_GATT_SERVER_STARTING = 1000;
    public static final int WAIT_FOR_SCANNING = 5000;

    private BleDriver mBleDriver;
    private String peerID;

    /*private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("onReceive");
            assertNotNull(MainActivity.dataArray.get(0));
                //assertEquals(new StringBuilder(HELLO_WORLD).reverse(), data);
            System.out.println("data: " + MainActivity.dataArray.get(0));
        }
    };*/

    private void mSleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            fail("sleep failed");
        }
    }
    @Before
    public void createBleInstance() {
        mBleDriver = BleDriver.getInstance();
        assertNotNull(mBleDriver);

        /*IntentFilter filter = new IntentFilter(JavaToGo.INTERFACE_UPDATE_DATA);
        InstrumentationRegistry.getInstrumentation().getTargetContext().registerReceiver(mBroadcastReceiver, filter);*/
    }

    @Test
    public void bleDriver_isSetup() {
        System.out.println("hello");
        // Context of the app under test.
        //Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // try multiple start/stop
        for (int i = 0; i < 3; i++) {
            mBleDriver.StopBleDriver();
            peerID = UUID.randomUUID().toString();
            assertEquals(true, mBleDriver.StartBleDriver(peerID));
        }
        // Wait that the GATT server is started
        mSleep(WAIT_FOR_GATT_SERVER_STARTING);
        // enable scanning devices
        mBleDriver.StartScanning();
        // Wait that scanning is finished
        mSleep(WAIT_FOR_SCANNING);
        assertNotNull(MainActivity.dataArray.get(0));
        //assertEquals(new StringBuilder(HELLO_WORLD).reverse(), data);
        System.out.println("data: " + MainActivity.dataArray.get(0));
    }
}
