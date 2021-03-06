package com.example.ble;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = ScanListAdapter.class.getSimpleName();
    private static final int ENABLE_BT_REQUEST = 1;
    private static String[] permissionStrings = new String[] {Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION};
    private static final int BLUETOOTH = 0;
    private static final int BLUETOOTH_ADMIN = 1;
    private static final int ACCESS_FINE_LOCATION = 2;
    private int[] permissionsRequestCode = { BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION };
    private boolean permissions[] = {false, false, false};
    private BluetoothAdapter bluetoothAdapter;
    private ListView scanList;
    private ScanListAdapter scanListAdapter;
    private Handler handler;
    private AdapterView.OnItemClickListener messageClickHandler;

    // interface where received data are put
    public static ArrayList<String> dataArray = new ArrayList<>();

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanListAdapter.notifyDataSetChanged();
        }
    };

    private class ScanListAdapter extends ArrayAdapter<String> {

        public ScanListAdapter(Context context, int resource, ArrayList<String> devices) {
            super(context, resource, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1,
                        container, false);
            }
            TextView textView = convertView.findViewById(android.R.id.text1);
            textView.setText(getItem(position));
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Thread.currentThread().setName("mainThread");

        scanList = findViewById(R.id.scan_listView);
        scanListAdapter = new ScanListAdapter(this,
                android.R.layout.simple_list_item_1 , dataArray);
        scanList.setAdapter(scanListAdapter);


        messageClickHandler = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*Intent intent = new Intent(getApplicationContext(), DeviceActivity.class);
                intent.putExtra("ble.device",
                        (BluetoothDevice)parent.getItemAtPosition(position));
                startActivity(intent);*/
            }
        };
        //scanList.setOnItemClickListener(messageClickHandler);

        handler = new Handler(Looper.getMainLooper());

        /* Check if BLE permissions are granted
           Visit : https://developer.android.com/training/permissions/requesting#perm-check
        */
        for (int i = 0; i < 3; i++) {
            if (hasPermission(permissionStrings[i])) {
                permissions[i] = true;
            } else {
                requestPermission(i);
            }
        }

        // Check BLE supported by target device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            //return ;
        }
        else {
            Toast.makeText(this, R.string.ble_is_supported, Toast.LENGTH_SHORT).show();
        }

        // Check if BLE is enabled and ask user to enable it if needed
        // API level 18 minimum required for the BluetoothManager class
        final BluetoothManager bluetoothManager =
                (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if ((bluetoothAdapter == null) || (!bluetoothAdapter.isEnabled())) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST);
        }
        IntentFilter filter = new IntentFilter(JavaToGo.INTERFACE_UPDATE_DATA);
        getApplicationContext().registerReceiver(mBroadcastReceiver, filter);
    }

    private boolean hasPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    private void requestPermission(int i) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionStrings[i])) {
            Toast.makeText(this, R.string.ble_permissions_explain,
                    Toast.LENGTH_LONG);
        }
        ActivityCompat.requestPermissions(this, new String[]{permissionStrings[i]},
                permissionsRequestCode[i]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] strPerm,
                                           int [] grantResults) {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissions[requestCode] = true;
        } else {
            permissions[requestCode] = false;
        }
    }

    public void driverOn(final View view) {
        if (!BleDriver.StartBleDriver(UUID.randomUUID().toString()))
            Log.e(TAG, "bleScanStart: failed to start ble");
    }

    public void driverOff(final View view) {
        BleDriver.StopBleDriver();
    }

    public void scanOn(final View view) {
        // Stops scanning after 20 seconds.
        final long SCAN_PERIOD = 20000;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BleDriver.setScanning(false);
            }
        }, SCAN_PERIOD);
        BleDriver.setScanning(true);
    }

    public void scanOff(View view) {
        BleDriver.setScanning(false);
    }

    public void advertiseOn(View view) {
        BleDriver.setAdvertising(true);
    }

    public void advertiseOff(View view) {
        BleDriver.setAdvertising(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENABLE_BT_REQUEST) {
            int bleState = resultCode;
        }
    }

    private void addDataEntry(String device) {
        dataArray.add(device);
    }
}
