package com.example.mobilehw2.receivers;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.mobilehw2.services.AlarmService;

public class AlarmReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(final Context context, Intent intent) {

        Log.v("threshold", intent.getExtras().getString("threshold").toString());
        Log.v("alarm", "received let's alarm");
        Intent i = new Intent(context, AlarmService.class);
        i.putExtra("threshold", intent.getExtras().get("threshold").toString());
        context.startForegroundService(i);
        setResultCode(Activity.RESULT_OK);

    }


}
