package edu.niu.android.beeteachat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Context context;

    // Must have for the Bluetooth functionality
    private BluetoothAdapter bluetoothAdapter; // For features like turning on/off Bluetooth and getting a list of paired devices
    private final int LOCATION_PERMISSION_REQUEST = 101;
    //private Set<BluetoothDevice> pairedDevices;
    //private BluetoothViewModel bluetoothViewModel;

    /*
    * Android Marshmallow and above need dynamic permission request
    * */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        // If it is null, then that means that the device doesn't support Bluetooth
        //bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Commenting all of this for now
        /*
        // Android's preferred way
        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        final int REQUEST_CODE = 100;

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "This device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
        } else if (!bluetoothAdapter.isEnabled()) {
            // Bluetooth isn't enabled or permission isn't enabled
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            // Asking for permission to use Bluetooth if permission has not been granted before
            String permission = "android.Manifest.permission.BLUETOOTH_CONNECT";
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] { permission }, REQUEST_CODE);
            }

            // Enable Bluetooth
            startActivityForResult(intent, REQUEST_CODE);
        }

        // Get a set of devices paired to the Android device
        // If Bluetooth is not enabled, it is an empty set
        pairedDevices = bluetoothAdapter.getBondedDevices();

        // Discover other Bluetooth devices to connect to
        bluetoothViewModel = new BluetoothViewModel();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        bluetoothViewModel.addDevice(BluetoothDeviceGeneric(device, null));

                }
            }
        };

        registerReceiver(broadcastReceiver, intentFilter);
         */
        initBluetooth();
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return true;//super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        /*
        switch (item.getItemId()) {
            case R.id.menu_search_devices:
                Toast.makeText(context, "Clicked Search Devices", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_enable_bluetooth:
                Toast.makeText(context, "Clicked Enable Bluetooth", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
         */

        /*
        switch (item.getItemId()) {
            case R.id.menu_search_devices:
                Toast.makeText(context, "Clicked Search Devices", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_enable_bluetooth:
                Toast.makeText(context, "Clicked Enable Bluetooth", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
         */

        if (item.getItemId() == R.id.menu_search_devices) {
            //Toast.makeText(context, "Clicked Search Devices", Toast.LENGTH_SHORT).show();
            checkPermission();
            return true;
        } else if (item.getItemId() == R.id.menu_enable_bluetooth) {
            //Toast.makeText(context, "Clicked Enable Bluetooth", Toast.LENGTH_SHORT).show();
            if (bluetoothAdapter != null){
                /*
                    bluetoothAdapter will only be null if the device
                    indicates that it does not have Bluetooth functionality.
                    If it is null, we do not want to call enableBluetooth()
                    because it will call bluetoothAdapter.enable(), and the
                    app will crash on a null reference to bluetoothAdapter
                */
                enableBluetooth();
            } else {
                Toast.makeText(context, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            /*
                Permission was already granted
                Go ahead and search for available devices
             */
            Intent intent = new Intent(context, DeviceListActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(context, DeviceListActivity.class);
                startActivity(intent);
            } else {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Location permission is required.\nPlease grant permission.")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkPermission();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MainActivity.this.finish();
                            }
                        })
                        .show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void enableBluetooth() {
        /*
        if (bluetoothAdapter.isEnabled()) {
            Toast.makeText(context, "Bluetooth already enabled", Toast.LENGTH_SHORT).show();
        } else {
            // Enabled Bluetooth
            bluetoothAdapter.enable();
        }
         */
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.isEnabled();
        }

        // If the device is not already visible to other devices, we will make it visible
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); // Make the device visible for five minutes
            startActivity(discoveryIntent);
        }
    }
}