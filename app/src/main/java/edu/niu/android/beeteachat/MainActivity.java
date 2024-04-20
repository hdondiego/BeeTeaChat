package edu.niu.android.beeteachat;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
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
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Context context;

    // Must have for the Bluetooth functionality
    private BluetoothAdapter bluetoothAdapter; // For features like turning on/off Bluetooth and getting a list of paired devices
    private ChatUtils chatUtils;

    private ListView listMainChat;
    private EditText editTextCreateMessage;
    private Button btnSendMessage;
    private ArrayAdapter<String> adapterMainChat;

    private final int LOCATION_PERMISSION_REQUEST = 101;
    private final int BLUETOOTH_SCAN_PERMISSION_REQUEST = 102;
    private final int SELECT_DEVICE = 103;
    //private Set<BluetoothDevice> pairedDevices;
    //private BluetoothViewModel bluetoothViewModel;

    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";
    private String connectedDevice;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case MESSAGE_STATE_CHANGED:
                    switch (message.arg1) {
                        case ChatUtils.STATE_NONE:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_LISTEN:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState("Connected: " + connectedDevice);
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] buffer = (byte[]) message.obj;
                    String inputBuffer = new String(buffer, 0, message.arg1);
                    adapterMainChat.add(connectedDevice + ": " + inputBuffer);
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer_write = (byte[]) message.obj;
                    String outputBuffer = new String(buffer_write);
                    adapterMainChat.add("Me: " + outputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void setState(CharSequence subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
    }

    private ActivityResultLauncher<Intent> deviceListResultLauncher;

    /*
     * Android Marshmallow and above need dynamic permission request
     * */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        init(); // Setup the ListView for the messages, the EditText to input the message, and Send button to direct the inputted text to ChatUtils
        initBluetooth(); // Attempts to get the BluetoothAdapter; if not successful, show a Toast saying the device doesn't support Bluetooth
        chatUtils = new ChatUtils(context, handler); // Passing in Context for runtime permission handling in ChatUtils and Handler to tell MainActivity about the device's Bluetooth state

        // Needed for new way of implementing onActivityForResult
        deviceListResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // get result data here
                            String address = result.getData().getStringExtra("deviceAddress");
                            //Toast.makeText(context, "Address: " + address, Toast.LENGTH_SHORT).show();
                            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));
                        }
                    }
                });

    }

    // Setup the ListView for the messages, the EditText to input the message,
    // and Send button to direct the inputted text to ChatUtils
    private void init() {
        listMainChat = (ListView) findViewById(R.id.list_conversation);
        editTextCreateMessage = (EditText) findViewById(R.id.editTextEnterMessage);
        btnSendMessage = (Button) findViewById(R.id.btnSendMessage);

        adapterMainChat = new ArrayAdapter<String>(context, R.layout.message_layout);
        listMainChat.setAdapter(adapterMainChat);

        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editTextCreateMessage.getText().toString();
                if (!message.isEmpty()) {
                    editTextCreateMessage.setText("");
                    chatUtils.write(message.getBytes());
                }
            }
        });
    }

    // Attempts to get the BluetoothAdapter
    // If not successful, show a Toast saying the device doesn't support Bluetooth
    private void initBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
        }
    }

    // Allows us to add our OptionsMenu that contains two buttons
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_search_devices) {
            // The gateway to the DeviceListActivity
            Toast.makeText(context, "Clicked Search Devices", Toast.LENGTH_SHORT).show();
            checkPermission(); //
            return true;
        } else if (item.getItemId() == R.id.menu_enable_bluetooth) {
            //Toast.makeText(context, "Clicked Enable Bluetooth", Toast.LENGTH_SHORT).show();
            if (bluetoothAdapter != null) {
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
        /*
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            //  Permission was already granted
            //  Go ahead and search for available devices

            Intent intent = new Intent(context, DeviceListActivity.class);
            startActivity(intent);
        }
        */

        // Checks to see if
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //permissionRequester.launch(arrayOf(BLUETOOTH_CONNECT, BLUETOOTH_SCAN))
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION}, BLUETOOTH_SCAN_PERMISSION_REQUEST);
        } else {
            // connect to device
            Intent intent = new Intent(context, DeviceListActivity.class);
            //startActivity(intent);
            /*
            ActivityResultLauncher<Intent> deviceListResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                // get result data here
                                //Log.d(MainActivity.class.getSimpleName(), "Inside of deviceListResultLauncher");
                                String address = result.getData().getStringExtra("deviceAddress");
                                Toast.makeText(context, "Address: " + address, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
             */
            deviceListResultLauncher.launch(intent);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(context, DeviceListActivity.class);
                //startActivity(intent);
                /*
                ActivityResultLauncher<Intent> deviceListResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        new ActivityResultCallback<ActivityResult>() {
                            @Override
                            public void onActivityResult(ActivityResult result) {
                                if (result.getResultCode() == Activity.RESULT_OK) {
                                    // get result data here
                                    //Log.d(MainActivity.class.getSimpleName(), "Inside of deviceListResultLauncher");
                                    String address = result.getData().getStringExtra("deviceAddress");
                                    Toast.makeText(context, "Address: " + address, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                 */
                deviceListResultLauncher.launch(intent);
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
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.isEnabled();
        }

        // If the device is not already visible to other devices, we will make it visible
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.BLUETOOTH_SCAN}, BLUETOOTH_SCAN_PERMISSION_REQUEST);
            return;
        } else {
            if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); // Make the device visible for five minutes
                startActivity(discoveryIntent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (chatUtils != null) {
            chatUtils.stop();
        }
    }
}