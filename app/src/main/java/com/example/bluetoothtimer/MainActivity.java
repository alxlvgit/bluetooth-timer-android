package com.example.bluetoothtimer;
import static android.content.ContentValues.TAG;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    private Button startPauseButton;
    private Button reset;
    private TextView countdown;
    private CountDownTimer countDownTimer;
    private EditText input;
    boolean timerRunning;
    final BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private long preset = 0;
    private static final int BT_PERMISSIONS_REQUEST_CODE = 0x55;
    private int permissionsCount;

    //----------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // PERMISSIONS
    // Only For Android 12 and above
    // https://stackoverflow.com/questions/72825519/android-permissions-check-for-ble

    private String[] getMissingBPermissions() {
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
        String[] missingPermissions = getMissingBPermissions();
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
                        Log.d(TAG, "All permissions granted");
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
    //----------------------------------------------------------------------------------------------------------------------------------------------------------------------


    // TIMER FORMATS
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

    // TIMER FUNCTIONS
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

                // Countdown timer
                countDownTimer = new CountDownTimer(preset, 1000) {
                    // Callback on set interval
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if ((preset / 60000) >= 60) {
                            updateCountdownTextHours(millisUntilFinished);
                        } else {
                            updateCountdownTextMinutes(millisUntilFinished);
                        }
                        preset = millisUntilFinished;
                        input.setVisibility(View.INVISIBLE);
                    }

                    // Callback when timer is finished
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onFinish() {
                        timerRunning = false;
                        resetTimer();
                        input.setVisibility(View.VISIBLE);
                        //  Disable bluetooth adapter if requested
                        if (bAdapter.isEnabled()) {
                            bAdapter.disable();
                            Toast.makeText(getApplicationContext(), "Bluetooth Turned OFF", Toast.LENGTH_SHORT).show();
                        }
                    }
                }.start();
                timerRunning = true;
                startPauseButton.setText("Pause");
                reset.setVisibility(View.INVISIBLE);
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth is OFF", Toast.LENGTH_SHORT).show();
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
        updateCountdownTextMinutes(preset);
        input.getText().clear();
        reset.setVisibility(View.INVISIBLE);
        startPauseButton.setVisibility(View.VISIBLE);
        startPauseButton.setText("Start");
        input.setVisibility(View.VISIBLE);
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
        checkBTPermissions();

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
                } else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(getApplicationContext(), "Please grant the permission to use Bluetooth adapter", Toast.LENGTH_SHORT).show();
                } else {
                    startTimer();
                }
            }
        });
    }
}