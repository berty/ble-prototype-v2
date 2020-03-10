package com.example.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

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
public class BleDriverInstrumentedTest {
    private static final String HELLO_WORLD = "Hello World!";
    private BleDriver mBleDriver;
    private String peerID;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String peerID = intent.getStringExtra(JavaToGo.INTERFACE_EXTRA_DATA_PID);
            String data = intent.getStringExtra(JavaToGo.INTERFACE_EXTRA_DATA);
            if (action == JavaToGo.INTERFACE_FOUND_PEER) {
                assertNotNull(peerID);
                assertEquals(true, BleDriver.SendToPeer(peerID, HELLO_WORLD.getBytes()));
            } else if (action == JavaToGo.INTERFACE_RECEIVE_FROM_PEER) {
                assertNotNull(peerID);
                assertNotNull(data);
                assertEquals(new StringBuilder(HELLO_WORLD).reverse(), data);
            } else {
                fail("Intent not recognized");
            }
        }
    };

    @Before
    public void createBleInstance() {
        mBleDriver = BleDriver.getInstance();
        assertNotNull(mBleDriver);

        IntentFilter filter = new IntentFilter(JavaToGo.INTERFACE_FOUND_PEER);
        filter.addAction(JavaToGo.INTERFACE_RECEIVE_FROM_PEER);
        InstrumentationRegistry.getInstrumentation().getTargetContext().registerReceiver(mBroadcastReceiver, filter);
    }

    @Test
    public void bleDriver_isSetup() {
        // Context of the app under test.
        //Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // try multiple start/stop
        for (int i = 0; i < 3; i++) {
            mBleDriver.StopBleDriver();
            peerID = UUID.randomUUID().toString();
            assertEquals(true, mBleDriver.StartBleDriver(peerID));
        }

        // enable scanning devices
        mBleDriver.StartScanning();
    }
}
