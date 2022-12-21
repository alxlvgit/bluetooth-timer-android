package com.example.bluetoothtimer;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Button startPauseButton;
    private Button reset;
    private TextView countdown;
    private CountDownTimer countDownTimer;
    private EditText input;
    boolean timerRunning;
    ListView listView;
    final BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private String deviceToUnpair;
    private List<String> myBondedDevices;
    private long preset = 0;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch timeFormat;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch bluetoothOff;
    private static final int BT_PERMISSIONS_REQUEST_CODE = 0x55;
    private int permissionsCount;

    // min/sec format for timer
    public void updateCountdownTextMinutes(long millis) {
        int minutes = (int) (millis / 1000) / 60;
        int seconds = (int) (millis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        countdown.setText(timeLeftFormatted);
    }

    // hr/min/sec format for timer
    public void updateCountdownTextHours(long millis) {
        int hours = (int) (millis / 3600000);
        int minutes = (int) ((millis / 1000) % 3600) / 60;
        int seconds = (int) (millis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        countdown.setText(timeLeftFormatted);
    }

    // Start timer
    @SuppressLint("SetTextI18n")
    public void startTimer() {
        if (bAdapter.isEnabled()) {
            if (presetTime() == 0) {
                return;
            }
            if (preset == 0) {
                preset = presetTime();
            }
            if ((myBondedDevices == null || deviceToUnpair == null) && !bluetoothOff.isChecked()) {
                Toast.makeText(getApplicationContext(), "Select a device to unpair or switch 'Bluetooth OFF' to start the timer", Toast.LENGTH_SHORT).show();
                return;
            }
            listView.setVisibility(View.INVISIBLE);

            // Countdown timer
            countDownTimer = new CountDownTimer(preset, 1000) {

                // Callback on set interval
                @Override
                public void onTick(long millisUntilFinished) {
                    if (timeFormat.isChecked()) {
                        updateCountdownTextHours(millisUntilFinished);
                    } else {
                        updateCountdownTextMinutes(millisUntilFinished);
                    }
                    preset = millisUntilFinished;
                }

                // Callback when timer is finished
                @SuppressLint("MissingPermission")
                @Override
                public void onFinish() {
                    timerRunning = false;
                    resetTimer();

                    // Find bluetooth device by name and unpair it if exists
                    @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();
                    for (BluetoothDevice bt : pairedDevices) {
                        if (bt.getName().contains(deviceToUnpair)) {
                            unpairDevice(bt);
                            deviceToUnpair = null;
                        }
                    }

                    //  Disable bluetooth adapter if requested
                    if (bAdapter.isEnabled() && bluetoothOff.isChecked()) {
                        bAdapter.disable();
                        Toast.makeText(getApplicationContext(), "Bluetooth Turned OFF", Toast.LENGTH_SHORT).show();
                    }
                }
            }.start();
            timerRunning = true;
            startPauseButton.setText("Pause");
            reset.setVisibility(View.INVISIBLE);
        } else {
            Toast.makeText(getApplicationContext(), "Please turn the Bluetooth ON", Toast.LENGTH_SHORT).show();
        }
    }

    // Pause timer
    @SuppressLint("SetTextI18n")
    public void pauseTimer() {
        countDownTimer.cancel();
        timerRunning = false;
        startPauseButton.setText("Start");
        reset.setVisibility(View.VISIBLE);
    }

    // Reset timer
    public void resetTimer() {
        preset = 0;
        if (timeFormat.isChecked()) {
            updateCountdownTextHours(preset);
        } else {
            updateCountdownTextMinutes(preset);
        }
        input.getText().clear();
        listView.setVisibility(View.INVISIBLE);
        reset.setVisibility(View.INVISIBLE);
        startPauseButton.setVisibility(View.VISIBLE);
        startPauseButton.setText("Start");
    }

    // Unpair bluetooth device
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Preset time for timer
    public long presetTime() {
        if (input != null && !(input.length() == 0) && !input.getText().toString().equals("0")) {
            long preset = (Long.parseLong(input.getText().toString())) * 60000;
            if (preset != 0) {
                return preset;
            }
        }
        Toast.makeText(getApplicationContext(), "Please, enter the preset value for timer", Toast.LENGTH_SHORT).show();
        return 0;
    }

    // List all paired bluetooth devices
    @SuppressLint("MissingPermission")
    public ArrayList<String> listDevices() {
        ArrayList<String> devices = new ArrayList<String>();
        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();
        for (BluetoothDevice bt : pairedDevices) {
            devices.add(bt.getName());
        }
        return devices;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();
        input = findViewById(R.id.minutesInput);
        startPauseButton = findViewById(R.id.start_pause);
        reset = findViewById(R.id.reset_btn);
        countdown = findViewById(R.id.textview_countdown);
        Button selectBt = findViewById(R.id.select_bt_btn);
        timeFormat = findViewById(R.id.switch_format);
        bluetoothOff = findViewById(R.id.bluetoothOff);
        listView = (ListView) findViewById(R.id.devices_list);

        checkBTPermissions();

        // List all bluetooth devices in a listview and select the device to unpair
        selectBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bAdapter.isEnabled()) {
                    myBondedDevices = listDevices();
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, myBondedDevices);
                    listView.setVisibility(View.VISIBLE);
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            deviceToUnpair = myBondedDevices.get(position);
                            Toast.makeText(getApplicationContext(), "You've selected " + myBondedDevices.get(position), Toast.LENGTH_SHORT).show();
                            listView.setVisibility(View.INVISIBLE);
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "Please turn the Bluetooth ON", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Reset button listener
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetTimer();
            }
        });

        // StartPause button listener
        startPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (timerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });

        // Change time format Switch listener
        timeFormat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    updateCountdownTextHours(preset);
                } else {
                    updateCountdownTextMinutes(preset);
                }
            }
        });
    }

    // Get all needed permissions
    // For Android 12 and above
    private String[] getMissingBlePermissions() {
        String[] missingPermissions = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions = new String[1];
                missingPermissions[0] = Manifest.permission.BLUETOOTH_SCAN;
            }

            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (missingPermissions == null) {
                    missingPermissions = new String[1];
                    missingPermissions[0] = Manifest.permission.BLUETOOTH_CONNECT;
                } else {
                    missingPermissions = Arrays.copyOf(missingPermissions, missingPermissions.length + 1);
                    missingPermissions[missingPermissions.length - 1] = Manifest.permission.BLUETOOTH_CONNECT;
                }
            }

        }
        return missingPermissions;
    }

    private void checkBTPermissions() {
        String[] missingPermissions = getMissingBlePermissions();
        if (missingPermissions == null || missingPermissions.length == 0) {
            Log.i(TAG, "checkBTPermissions: Permissions is already granted");
            return;
        }

        for (String permission : missingPermissions)
            Log.d(TAG, "checkBTPermissions: missing permissions " + permission);
        permissionsCount = missingPermissions.length;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(missingPermissions, BT_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == BT_PERMISSIONS_REQUEST_CODE) {
            int index = 0;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted for " + permissions[index]);
                    if (permissionsCount > 0) permissionsCount--;
                    if (permissionsCount == 0) {
                        // All permissions have been granted from user.
                    }
                } else {
                    Log.d(TAG, "Permission denied for " + permissions[index]);
                    // Permission denied
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}