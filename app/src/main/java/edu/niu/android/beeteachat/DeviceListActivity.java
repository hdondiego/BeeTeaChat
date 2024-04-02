package edu.niu.android.beeteachat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {
    private ListView listPairedDevices, listAvailableDevices;
    private ArrayAdapter<String> pairedDevicesAdapter, availableDevicesAdapter;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        init();
    }

    private void init() {
        listPairedDevices = (ListView) findViewById(R.id.listPairedDevices);
        listAvailableDevices = (ListView) findViewById(R.id.listAvailableDevices);

        pairedDevicesAdapter = new ArrayAdapter<String>(context, R.layout.device_list_item);
        availableDevicesAdapter = new ArrayAdapter<String>(context, R.layout.device_list_item);

        listPairedDevices.setAdapter(pairedDevicesAdapter);
        listAvailableDevices.setAdapter(availableDevicesAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        /*
        switch (item.getItemId()) {

        }
         */
        if (item.getItemId() == R.id.menu_scan_devices) {
            Toast.makeText(context, "Scan devices clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}