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

import androidx.core.app.NotificationCompat;

import com.example.mobilehw2.permissionGetters.DeviceAdmin;
import com.example.mobilehw2.R;

import static java.lang.StrictMath.abs;


public class SleepModeService extends Service implements SensorEventListener {

    public static final String CHANNEL_ID = "SleepModeServiceChannel";

    SensorManager mySensorManager;

    private double threshold = 0.0f;

    private static String TAG = "TAG";
    private DevicePolicyManager deviceManger;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String thresh = intent.getExtras().get("threshold").toString();
        threshold = Double.valueOf(thresh);
        threshold = Math.cos(threshold) * SensorManager.GRAVITY_EARTH - 0.05f;

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

        // Create sensor manager
        mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Accelerometer sensor
        Sensor mySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mySensorManager.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);

        deviceManger = (DevicePolicyManager) getSystemService(
                DEVICE_POLICY_SERVICE);

        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ComponentName componentName = new ComponentName(this, DeviceAdmin.class);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "MyApp::MyWakelockTag");

        Log.v(TAG, "setResource");

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (abs(event.values[2]) > threshold) {
            Log.d(TAG, "We should sleep !!");
            deviceManger.lockNow();
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


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {

        public SleepModeService getService() {
            // Return this instance of SleepModeService so clients can call public methods
            return SleepModeService.this;
        }

    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }


}
