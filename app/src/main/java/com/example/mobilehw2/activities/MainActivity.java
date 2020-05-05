package com.example.mobilehw2.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import com.example.mobilehw2.receivers.AlarmReceiver;
import com.example.mobilehw2.permissionGetters.DeviceAdmin;
import com.example.mobilehw2.R;
import com.example.mobilehw2.services.AlarmService;
import com.example.mobilehw2.services.ShakeService;
import com.example.mobilehw2.services.SleepModeService;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    ToggleButton alarmToggle;
    ToggleButton shakeToggle;
    ToggleButton sleepToggle;

    EditText alarmText;
    EditText shakeText;
    EditText sleepText;


    private TimePicker alarmTimePicker;

    private AlarmManager manager;
    private PendingIntent pendingIntent;

    private Intent alarmIntent;
    private Intent sleepModeIntent;
    private Intent shakeIntent;

    private DevicePolicyManager deviceManger;
    private static final int ADMIN_INTENT = 15;
    private static final String description = "Some Description About Your Admin";

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        alarmToggle = (ToggleButton) findViewById(R.id.alarmToggle);
        shakeToggle = (ToggleButton) findViewById(R.id.shakeToggle);
        sleepToggle = (ToggleButton) findViewById(R.id.sleepModeToggle);

        alarmText = findViewById(R.id.alarmSleep);
        shakeText = findViewById(R.id.shakeSensitivity);
        sleepText = findViewById(R.id.sleepModeDegree);


        alarmTimePicker = (TimePicker) findViewById(R.id.alarmTimePicker);

        compileData();

        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);


        alarmIntent = new Intent(this, AlarmReceiver.class);
        sleepModeIntent = new Intent(this, SleepModeService.class);
        shakeIntent = new Intent(this, ShakeService.class);

        ComponentName mComponentName = new ComponentName(this, DeviceAdmin.class);
        deviceManger = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);

        ComponentName compName = new ComponentName(this, DeviceAdmin.class);
        boolean active = deviceManger.isAdminActive(compName);

        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, description);
        startActivityForResult(intent, ADMIN_INTENT);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void alarmOnToggleClicked(View view) {


        if (((ToggleButton) view).isChecked()) {

            // setup alarm ...

            String threshold = alarmText.getText().toString();
            if (threshold.equals("")) {

                // default mode ...
                threshold = "2";

            }
            alarmIntent.putExtra("threshold", threshold);

            Log.d("MyActivity", "Alarm On");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
            calendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());

            alarmTimePicker.setEnabled(false);
            Log.v("alarm", "lunch launcher");

            pendingIntent = PendingIntent.getBroadcast(this,
                    0,
                    alarmIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            manager.setRepeating(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent);

            Log.v("threshold", threshold);


        } else {

            alarmTimePicker.setEnabled(true);

            // service Alive.

            // you can use these way ...
            Intent serviceIntent = new Intent(this, AlarmService.class);
            stopService(serviceIntent);

            Log.v("Service", "service offing");

                /*// or ...
                bindService(alarmIntent, mAlarmServiceConnection, Context.BIND_AUTO_CREATE);
                // you can use stopService instead.

                if (mAlarmModeServiceBound) {

                    mAlarmModeService.stopSelf();
                    unbindService(mAlarmServiceConnection);
                    mAlarmModeServiceBound = false;

                }*/

            // in broadCast

            pendingIntent = PendingIntent.getBroadcast(this,
                    0,
                    alarmIntent,
                    0);

            manager.cancel(pendingIntent);
            Log.v("Service", "broadOffing");

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void shakeOnToggleClicked(View view) {

        if (((ToggleButton) view).isChecked()) {


            String threshold = shakeText.getText().toString();
            if (threshold.equals("")) {

                // default mode ...
                threshold = "12";

            }
            shakeIntent.putExtra("threshold", threshold);

            Log.d("MyActivity", "Shake service on!");
            startForegroundService(shakeIntent);
            bindService(shakeIntent, mShakeServiceConnection, Context.BIND_AUTO_CREATE);

        } else {

            // you can use these way ...
            Intent serviceIntent = new Intent(this, ShakeService.class);
            stopService(serviceIntent);

            if (mShakeServiceBound) {

                mShakeService.stopSelf();
                unbindService(mShakeServiceConnection);
                mShakeServiceBound = false;
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sleepModeOnToggleClicked(View view) {

        if (((ToggleButton) view).isChecked()) {

            String threshold = sleepText.getText().toString();
            if (threshold.equals("")) {
                threshold = "0";
            }

            sleepModeIntent.putExtra("threshold", threshold);
            Log.d("MyActivity", "Sleep Mode service on!");
            startForegroundService(sleepModeIntent);
            bindService(sleepModeIntent, mSleepServiceConnection, Context.BIND_AUTO_CREATE);

        } else {

            // you can use these way ...
            Intent serviceIntent = new Intent(this, SleepModeService.class);
            stopService(serviceIntent);


            Log.v("Testing", "destroyed");
            if (mSleepModeServiceBound) {

                mSleepModeService.stopSelf();
                unbindService(mSleepServiceConnection);
                mSleepModeServiceBound = false;
                Log.v("Testing", "destroyed");
            }

        }
    }


    public void lockPhone() {
        deviceManger.lockNow();
    }


    // AlarmServiceBounding  ...
    AlarmService mAlarmModeService;
    boolean mAlarmModeServiceBound = false;


    private ServiceConnection mAlarmServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mAlarmModeServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AlarmService.LocalBinder myBinder = (AlarmService.LocalBinder) service;
            mAlarmModeService = myBinder.getService();
            mAlarmModeServiceBound = true;
        }
    };


    // sleepServiceBounding  ...
    SleepModeService mSleepModeService;
    boolean mSleepModeServiceBound = false;


    private ServiceConnection mSleepServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSleepModeServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SleepModeService.LocalBinder myBinder = (SleepModeService.LocalBinder) service;
            mSleepModeService = myBinder.getService();
            mSleepModeServiceBound = true;
        }
    };


    // shakeServiceBounding
    ShakeService mShakeService;
    boolean mShakeServiceBound = false;


    private ServiceConnection mShakeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mShakeServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ShakeService.LocalBinder myBinder = (ShakeService.LocalBinder) service;
            mShakeService = myBinder.getService();
            mShakeServiceBound = true;
        }
    };

    @Override
    protected void onStop() {
        super.onStop();

        if (mSleepModeServiceBound) {
            unbindService(mSleepServiceConnection);
            mSleepModeServiceBound = false;
        }

        if (mShakeServiceBound) {
            unbindService(mShakeServiceConnection);
            mShakeServiceBound = false;
        }

        if (mAlarmModeServiceBound) {
            unbindService(mAlarmServiceConnection);
            mAlarmModeServiceBound = false;
        }

        writeToFile();
    }


    private void writeToFile() {

        String statesData = "";
        if (alarmToggle.isChecked()) {
            statesData = statesData.concat("1");
        } else {
            statesData = statesData.concat("0");
        }

        if (shakeToggle.isChecked()) {
            statesData = statesData.concat("#1");
        } else {
            statesData = statesData.concat("#0");
        }

        if (sleepToggle.isChecked()) {
            statesData = statesData.concat("#1#");
        } else {
            statesData = statesData.concat("#0#");
        }

        String threshold = "";


        if (alarmText.getText().toString().equals("")) {
            threshold = threshold.concat("-#");
        } else {
            threshold = threshold.concat(alarmText.getText().toString()).concat("#");
        }

        if (shakeText.getText().toString().equals("")) {
            threshold = threshold.concat("-#");
        } else {
            threshold = threshold.concat(shakeText.getText().toString()).concat("#");
        }

        if (sleepText.getText().toString().equals("")) {
            threshold = threshold.concat("-");
        } else {
            threshold = threshold.concat(sleepText.getText().toString());
        }

        String allData = statesData.concat(threshold);

        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(allData);
            outputStreamWriter.close();

        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String readFromFile() {

        String ret = "";

        try {
            InputStream inputStream = this.openFileInput("config.txt");

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    public void compileData() {

        String data = readFromFile();

        if (data.equals("")) {
            return;
        }

        data = data.trim();

        String[] datas = data.split("#");

        // setting up toggles ...
        if (datas[0].equals("1")) {
            alarmToggle.setChecked(true);
        } else {
            alarmToggle.setChecked(false);
        }

        if (datas[1].equals("1")) {
            shakeToggle.setChecked(true);
        } else {
            shakeToggle.setChecked(false);
        }

        if (datas[2].equals("1")) {
            sleepToggle.setChecked(true);
        } else {
            sleepToggle.setChecked(false);
        }


        // setting up editTexts ...
        if (datas[3].equals("-")) {
            alarmText.setText("");
        } else {
            alarmText.setText(datas[3]);
        }

        if (datas[4].equals("-")) {
            shakeText.setText("");
        } else {
            shakeText.setText(datas[4]);
        }

        if (datas[5].equals("-")) {
            sleepText.setText("");
        } else {
            sleepText.setText(datas[5]);
        }


    }

}
