package com.example.ble;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.widget.TextView;

public class DeviceActivity extends AppCompatActivity {

    BluetoothDevice bluetoothDevice;
    TextView deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        bluetoothDevice = getIntent().getExtras().getParcelable("ble.device");
        deviceName = findViewById(R.id.deviceName_textView);
        deviceName.setText(bluetoothDevice.getName());
    }
}
