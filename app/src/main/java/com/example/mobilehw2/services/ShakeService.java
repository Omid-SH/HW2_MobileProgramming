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
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.mobilehw2.permissionGetters.DeviceAdmin;
import com.example.mobilehw2.R;

public class ShakeService extends Service implements SensorEventListener {

    public static final String CHANNEL_ID = "ShakeServiceChannel";
    private static final String TAG = "Service";

    private SensorManager mySensorManager;

    private float threshold = 0.0f;
    private float mAccelerometerData;
    private float mAccelerometerDataCurrent;
    private float mAccelerometerDataLast;

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        // Create sensor manager
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String thresh = intent.getExtras().get("threshold").toString();
        threshold = Float.valueOf(thresh);

        Log.v("threshold", String.valueOf(threshold));


        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel();
        else
            startForeground(1, new Notification());

        setUp();

        return START_STICKY;
    }

    private void setUp() {

        mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometerData = 10f;
        mAccelerometerDataCurrent = SensorManager.GRAVITY_EARTH;
        mAccelerometerDataLast = SensorManager.GRAVITY_EARTH;

        // Accelerometer sensor
        assert mySensorManager != null;
        Sensor mySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mySensorManager.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);

        DevicePolicyManager deviceManger = (DevicePolicyManager) getSystemService(
                DEVICE_POLICY_SERVICE);
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ComponentName componentName = new ComponentName(this, DeviceAdmin.class);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "MyApp::MyWakelockTag");

        Log.v(TAG, "setResource");

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        Log.v(TAG, "sensing");

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        mAccelerometerDataLast = mAccelerometerDataCurrent;
        mAccelerometerDataCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
        float delta = mAccelerometerDataCurrent - mAccelerometerDataLast;
        mAccelerometerData = mAccelerometerData * 0.9f + delta;
        if (mAccelerometerData > threshold) {
            Log.v(TAG, "light on");
            screenOn();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not in use
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Destroyed");
        mySensorManager.unregisterListener(this);
        stopSelf();
    }


    private void screenOn() {
        if (wakeLock.isHeld() == false) {
            wakeLock.acquire(1000);
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
    }*/

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

    private final IBinder mBinder = new ShakeService.LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public class LocalBinder extends Binder {

        public ShakeService getService() {
            // Return this instance of SleepModeService so clients can call public methods
            return ShakeService.this;
        }

    }

}
