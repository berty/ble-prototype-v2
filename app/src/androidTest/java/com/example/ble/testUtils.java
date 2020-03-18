package com.example.ble;

import static org.junit.Assert.*;

public class testUtils {

    public static final int WAIT_FOR_GATT_SERVER_STARTING = 1000;
    public static final int WAIT_FOR_SCANNING = 5000;

    public static void mSleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            fail("sleep failed");
        }
    }

    public static String parse(String input, String prefix) {
        String peerID = null;
        int indexPrefix = 0;

        if (input != null) {
            if ((indexPrefix = input.indexOf(prefix)) != -1) {
                peerID = input.substring(indexPrefix + prefix.length());
            }
        }
        return peerID;
    }
}
