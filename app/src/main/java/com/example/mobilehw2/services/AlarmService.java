package com.example.mobilehw2.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.mobilehw2.R;
import com.example.mobilehw2.activities.WakeupActivity;
import com.example.mobilehw2.permissionGetters.DeviceAdmin;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static java.lang.StrictMath.abs;

public class AlarmService extends Service implements SensorEventListener {

    public static final String CHANNEL_ID = "AlarmServiceChannel";

    private float threshold = 0.0f;

    private Sensor mySensor;
    private SensorManager mySensorManager;
    private DevicePolicyManager deviceManger;
    private ActivityManager activityManager;
    private ComponentName componentName;

    private MediaPlayer mediaPlayer;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private CountDownTimer countDownTimer;
    private Vibrator v;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void lunchWakeUpActivity() {
        Intent intent = new Intent(this, WakeupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void vibrate() {

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 10 minute
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(10 * 60 * 1000, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(10 * 60 * 1000);
        }

    }

    private void startTimer() {

        countDownTimer = new CountDownTimer(10 * 6 * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                // do nothing
            }

            public void onFinish() {
                endService();
            }
        }.start();

    }

    private void playMusic() {
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        mediaPlayer = MediaPlayer.create(this, alarmUri);
        mediaPlayer.setLooping(true); // Set looping
        mediaPlayer.setVolume(100, 100);
        mediaPlayer.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String thresh = intent.getExtras().get("threshold").toString();
        threshold = Float.valueOf(thresh);

        Log.v("threshold", String.valueOf(threshold));


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel();
        else
            startForeground(1, new Notification());

        setUp();

        // todo do multiThread ...
        lunchWakeUpActivity();
        startTimer();
        playMusic();
        vibrate();

        return START_STICKY;
    }

    private void setUp() {

        Log.v("alarm", "Alarm service started.");

        // Create sensor manager
        mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // GYROSCOPE sensor
        assert mySensorManager != null;
        mySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mySensorManager.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);

        deviceManger = (DevicePolicyManager) getSystemService(
                DEVICE_POLICY_SERVICE);
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        componentName = new ComponentName(this, DeviceAdmin.class);

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "MyApp::MyWakelockTag");

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (abs(event.values[2]) > threshold) {
            Log.v("syn", "here");
            endService();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not in use
    }

    @Override
    public void onDestroy() {
        endService();
    }

    private void endService() {

        //syncFileMemory();

        // too annoying
        mediaPlayer.stop();
        mediaPlayer.release();
        // annoying ...
        v.cancel();
        // quietly ...
        mySensorManager.unregisterListener(this);
        countDownTimer.cancel();
        stopSelf();
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(1, notification);
        }
    }

    /*@Override
    public void onTaskRemoved(Intent rootIntent) {

        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent);

        super.onTaskRemoved(rootIntent);
    }
    */

    // Binder given to clients
    private final IBinder mBinder = new AlarmService.LocalBinder();


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {

        public AlarmService getService() {
            // Return this instance of SleepModeService so clients can call public methods
            return AlarmService.this;
        }

    }

    private void syncFileMemory() {

        Log.v("sync", "here");
        String written = readFromFile();
        char alarm = written.charAt(0);
        if (alarm == '1') {
            written = "0".concat(written.substring(1));

            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput("config.txt", Context.MODE_PRIVATE));
                outputStreamWriter.write(written);
                outputStreamWriter.close();

            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
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


}
