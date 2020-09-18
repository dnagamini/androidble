package com.example.android.bluetoothlegatt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends AppCompatActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private TextView mConnectionState;
    private ScrollView mDataFieldScroll;
    private TextView mDataField;
    private EditText mCommandField;
    private Button mButtonSend;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicHM_10;
    private List<Word> mWordList;
    private WordViewModel mWordViewModel;
    // private WordRepository mRepository;
    private int position = 0;
    private int firebase_active = 0;
    private String auxID;
    private String auxData;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mDbRef;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                enableNotificationHM_10(); //Bluetooth Receive
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void clearUI() {
        //mDataField.setText("");
    }

    Handler periodic_handler = new Handler();

    private Runnable runTask = new Runnable() {
        @Override
        public void run() {

            if (mWordList != null) {
                if(position == mWordList.size()){
                    position = 0;
                }
                Word current = mWordList.get(position);
                sendBLEdata(current.getWord());
                position++;

            }
            periodic_handler.postDelayed(this, 2500);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        //((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mDataField = (TextView) findViewById(R.id.data_value);
        mDataFieldScroll = (ScrollView)findViewById(R.id.data_value_scroll);
        mCommandField = (EditText) findViewById(R.id.command_message);
        mButtonSend = (Button) findViewById(R.id.button_send);
        mButtonSend.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        if(mConnected == true) {
                            String message = mCommandField.getText().toString();
                            sendBLEdata(message);
                        }
                    }
        });

        Switch periodic_switch = (Switch) findViewById(R.id.switch_periodic_message);
        periodic_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    periodic_handler.post(runTask);
                    //mWordList = mRepository.getAllWordsPeriodic();
                    // The toggle is enabled
                } else {
                    periodic_handler.removeCallbacks(runTask);
                    // The toggle is disabled
                }
            }
        });

        Switch firebase_switch = (Switch) findViewById(R.id.switch_firebase);
        firebase_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    firebase_active = 1;
                } else {
                    firebase_active = 0;
                }
            }
        });

        // Get a new or existing ViewModel from the ViewModelProvider.
        mWordViewModel = new ViewModelProvider(this).get(WordViewModel.class);

        mWordViewModel.getAllWords().observe(this, new Observer<List<Word>>() {
            @Override
            public void onChanged(@Nullable final List<Word> words) {
                // Update the cached copy of the words
                mWordList = words;
            }
        });

        //getActionBar().setTitle(mDeviceName);
        //getActionBar().setDisplayHomeAsUpEnabled(true);

        Toolbar controlToolbar = (Toolbar) findViewById(R.id.control_toolbar);
        setSupportActionBar(controlToolbar);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    public void configPeriodic(View view){
        Intent periodicMessageIntent = new Intent(this, PeriodicMessageActivity.class);
        startActivity(periodicMessageIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        periodic_handler.removeCallbacks(runTask);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.append("RECEIVE:" + data + "\n");

            if(firebase_active == 1){
                if(data.startsWith("ID")){
                    auxID = data.replace("ID", "");
                }

                if(data.startsWith("DATA")){
                    auxData = data.replace("DATA", "");
                    mDatabase = FirebaseDatabase.getInstance();
                    mDbRef = mDatabase.getReference("data");
                    String userId = mDbRef.push().getKey();
                    Log.v(TAG, auxID);
                    Log.v(TAG, auxData);
                    mDbRef.child(userId).setValue(new Post(auxID, auxData));
                    mDataField.append("Firebase: Data Sent to Cloud" + "\n");
                }

            }

            mDataFieldScroll.fullScroll(View.FOCUS_DOWN);
            TextCounter();
        }
    }

    private void sendBLEdata(String message) {

        String startmessage = "#";
        byte[] startbyte = startmessage.getBytes();
        if(sendHM_10(startbyte)) {
            mDataField.append("SEND: ");
            // mDataField.append(startmessage);
        }

        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run(){
                String firstmessage;
                if(message.length() > 20){
                    firstmessage = message.substring(0, 20);
                }
                else{
                    firstmessage = message.substring(0, message.length());
                }
                byte[] firstbyte = firstmessage.getBytes();
                if(sendHM_10(firstbyte)) {
                    mDataField.append(firstmessage);
                }
            }
        },1000);

        if(message.length() > 20){
            String secondmessage = message.substring(20);
            byte[] secondbyte = secondmessage.getBytes();
            handler.postDelayed(new Runnable() {
                @Override
                public void run(){
                    if(sendHM_10(secondbyte)) {
                        mDataField.append(secondmessage);
                    }
                }
            },1100);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run(){
                String endmessage = "*";
                byte[] endbyte = endmessage.getBytes();
                if(sendHM_10(endbyte)) {
                    // mDataField.append(endmessage + "\n");
                    mDataField.append("\n");
                }
            }
        },1200);

        mDataFieldScroll.fullScroll(View.FOCUS_DOWN);
        TextCounter();

    }

    private boolean sendHM_10(byte data[]){
        if(bluetoothGattCharacteristicHM_10 == null){
            Log.w(TAG, "Custom BLE Characteristic not found");
            return false;
        }
        if ((bluetoothGattCharacteristicHM_10.getProperties() | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            bluetoothGattCharacteristicHM_10.setValue(data);
            return mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
        }
        Log.w(TAG, "Characteristic has no write property");
        return false;
    }

    private void enableNotificationHM_10(){
        UUID UUID_HM_10 = UUID.fromString(SampleGattAttributes.HM_10);
        BluetoothGattService mCustomService = mBluetoothLeService.getHM_10GattService();
        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return;
        }
        bluetoothGattCharacteristicHM_10 = mCustomService.getCharacteristic(UUID_HM_10);
        if(bluetoothGattCharacteristicHM_10 == null){
            Log.w(TAG, "Custom BLE Characteristic not found");
            return;
        }
        if ((bluetoothGattCharacteristicHM_10.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mBluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristicHM_10, true);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void TextCounter(){
        if (mDataField.getLineCount() > 100) {
            mDataField.getEditableText().delete(0, mDataField.getText().length() - 1000);
        }
    }

}
