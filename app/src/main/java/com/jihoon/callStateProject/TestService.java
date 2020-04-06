package com.jihoon.callStateProject;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class TestService extends Service
{
    PhoneStateCheckListener phoneStateCheckListener;
    TelephonyManager telephonyManager;

    DBHelper dbHelper;
    SQLiteDatabase db = null;
    Cursor cursor;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.drawable.ic_launcher_background);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "snwodeer_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "SnowDeer Service Channel",
                    NotificationManager.IMPORTANCE_NONE);//IMPORTANCE_DEFAULT
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContent(remoteViews)
                .setContentIntent(pendingIntent);
        builder.setAutoCancel(true);

        startForeground(1, builder.build());
    }
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d("jihoonDebugging", "hi?");

    }
    public boolean isServiceRunningCheck() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.jihoon.callStateProject.MainService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("jihoonDebugging", "hi?");

        try{
            if(!isServiceRunningCheck())
            {
                dbHelper = new DBHelper(TestService.this, 1);
                db = dbHelper.getWritableDatabase();

                phoneStateCheckListener = new PhoneStateCheckListener(this);
                telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
                telephonyManager.listen(phoneStateCheckListener, PhoneStateListener.LISTEN_CALL_STATE);

                startForegroundService();
            }
        }
        catch (Exception e)
        {

        }


        return startId;
    }
    public class PhoneStateCheckListener extends PhoneStateListener
    {
        TestService testService;

        PhoneStateCheckListener(TestService _main){
            testService = _main;
        }
        @Override
        public void onCallStateChanged(int state, String incomingNumber)
        {
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                try
                {
                    if(isServiceRunningCheck())
                        stopService(new Intent(testService, MainService.class));
                }
                catch (Exception e)
                {
                    Log.d("jihoonDebugging", e.getMessage());
                }
            }
            else if (state == TelephonyManager.CALL_STATE_RINGING) {
                Intent my_intent = new Intent(testService, MainService.class);
                incomingNumber = replaceFunc(incomingNumber);
                String[] str = listUpdate(incomingNumber);

                //name TEXT, phonenum TEXT, email TEXT, memo TEXT)
                String [] checkBox = checklistUpdate();
                if(checkBox[0].equals("1"))
                    my_intent.putExtra("name", str[0]);
                if(checkBox[1].equals("1"))
                    my_intent.putExtra("phonenum", str[1]);
                if(checkBox[2].equals("1"))
                    my_intent.putExtra("email", str[2]);
                if(checkBox[3].equals("1"))
                    my_intent.putExtra("memo", str[3]);

                if(str[0] != null || str[1] != null || str[2] != null || str[3] != null)
                    startService(my_intent);
            }
            else if (state == TelephonyManager.CALL_STATE_OFFHOOK)
            {
                try
                {
                    if(isServiceRunningCheck())
                        stopService(new Intent(testService, MainService.class));
                }
                catch (Exception e)
                {
                    Log.d("jihoonDebugging", e.getMessage());
                }
            }
        }
    }

    public String[] checklistUpdate()
    {
        String[] str = new String[4];
        cursor = db.rawQuery("SELECT * FROM checkBox;", null);
        while (cursor.moveToNext())
        {
            for(int i=0;i<4;i++) str[i] = cursor.getString(i);
        }
        return str;
    }



    public String[] listUpdate(String n)
    {
        String[] str = new String[4];
        cursor = db.rawQuery("SELECT * FROM callState where phonenum = '" + n + "';", null);
        while (cursor.moveToNext()) for(int i=0;i<4;i++) str[i] = cursor.getString(i);
        return str;
    }

    public String replaceFunc(String str)
    {
        str = str.replace("-","");
        str = str.replace("(","");
        str = str.replace(")","");
        str = str.replace(" ","");

        return str;
    }

}
