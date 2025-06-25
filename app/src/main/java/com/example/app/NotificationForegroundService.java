package com.example.app;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;



public class NotificationForegroundService extends Service {

    private static final String CHANNEL_ID = "MyChannelID";
    private static final int NOTIFICATION_ID = 1;
    public UsageStatsManager usageStatsManager;
    private boolean isMonitoring = true;
    Map<String, String> targetAppPackageNames = new HashMap<>();
    public Handler handler;

    public Timer timer;
    public List<UsageStats> usageStatsList;

    @Override
    public void onCreate() {
        super.onCreate();
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, System.currentTimeMillis() - 5000, System.currentTimeMillis());
        handler = new Handler();
        timer = new Timer();
        populateAllInstalledApps();


//        targetAppPackageNames.put("com.instagram.android", "Instagram"); // Instagram
//        targetAppPackageNames.put("com.facebook.katana", "Facebook"); // Facebook
//        targetAppPackageNames.put("com.twitter.android", "Twitter"); // Twitter
//        targetAppPackageNames.put("com.snapchat.android", "Snapchat"); // Snapchat
//        targetAppPackageNames.put("com.linkedin.android", "LinkedIn"); // LinkedIn
//        targetAppPackageNames.put("com.whatsapp", "WhatsApp"); // WhatsApp
//        targetAppPackageNames.put("com.google.android.youtube", "YouTube");//Youtube

        handler = new Handler();
        createAndShowNotification("titleOFNotification", 0);
    }

    private void populateAllInstalledApps() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Get all apps with launcher activity
        List<ResolveInfo> pkgAppsList = pm.queryIntentActivities(mainIntent, 1);
        for (ResolveInfo info : pkgAppsList) {
            String packageName = info.activityInfo.packageName;
            String className = info.activityInfo.name;
            String appLabel = info.loadLabel(pm).toString();
            String appProcessName = info.activityInfo.applicationInfo.processName;
            String appSourceDir = info.activityInfo.applicationInfo.sourceDir;
            boolean isSystemApp = (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

            // Add to the map
            targetAppPackageNames.put(packageName, appLabel);

            // Log full details
//            Log.d("TargetAppLoaded", "Label: " + appLabel);
//            Log.d("TargetAppLoaded", "Package: " + packageName);
//            Log.d("TargetAppLoaded", "Class: " + className);
//            Log.d("TargetAppLoaded", "Process: " + appProcessName);
//            Log.d("TargetAppLoaded", "Source Dir: " + appSourceDir);
//            Log.d("TargetAppLoaded", "System App: " + isSystemApp);
//            Log.d("TargetAppLoaded", "-----------------------------");
        }

        // Optional: Log the loaded apps
//        for (Map.Entry<String, String> entry : targetAppPackageNames.entrySet()) {
//            System.out.println("TargetAppLoaded " + " Package: " + entry.getKey() + " | App: " + entry.getValue());
//        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!isMonitoring) {
            isMonitoring = true;
        }
        final long[] startTime = {System.currentTimeMillis()};
        new Thread((new Runnable() {
            @Override
            public void run() {
                String previousForeGround = "";
                while (isMonitoring) {

                    long threadStartTime = System.currentTimeMillis();
                    Intent overlayIntent = new Intent(getApplicationContext(), OverlayService.class);


                    //This will executed at t milli seconds
                    Pair<String,String> result = isForeground();
                    String currentForeGroundPackage = result.first;
                    String status = result.second;

                    if(status.equals("1") && !currentForeGroundPackage.equals(previousForeGround)){
                        overlayIntent.putExtra("packageName", previousForeGround);
                        overlayIntent.putExtra("BackOrFore","2");
                        startService(overlayIntent);
                        previousForeGround = currentForeGroundPackage;
                    }
//                    Log.d("NotificationForeGround","prev: " + previousForeGround + " current: " + currentForeGroundPackage);

                    overlayIntent.putExtra("packageName", result.first);
                    overlayIntent.putExtra("BackOrFore",result.second);
                    startService(overlayIntent);

                    long  elapsedTime = System.currentTimeMillis() - threadStartTime;
                    long sleepTime = 500 - elapsedTime; // Adjust sleep time
                    if(sleepTime>0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        })).start();


        return START_STICKY;
    }

    private Pair<String,String> isForeground() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long currentTime = System.currentTimeMillis();
        String resultPackageName="",status="0";
        // Query for usage statistics
        UsageEvents.Event event = new UsageEvents.Event();
        UsageEvents usageEvents = usageStatsManager.queryEvents(currentTime - 500 , currentTime); // Query for the last 1 seconds

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            String foregroundAppPackageName = event.getPackageName();
            if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (targetAppPackageNames.containsKey(foregroundAppPackageName)) {
                    resultPackageName = foregroundAppPackageName;
                    status = "1";
                    Log.d("NotificationForeground", "Target package foreground:" + foregroundAppPackageName);
                } else {
                    resultPackageName = foregroundAppPackageName;
                    status = "2";
                    Log.d("NotificationForeground", "Non Target package foreground:" + foregroundAppPackageName);
                }
            }
            if (event.getEventType() == UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                Log.d("NotificationForeground", "Non interactive screen remove the overlay");
                resultPackageName = foregroundAppPackageName;
                status = "2";
            }
        }
        return Pair.create(resultPackageName,status);
    }


    public void createAndShowNotification(String titleOFNotification, long textOfNotification) {
        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Hi there!!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true); // Make the notification ongoing (sticky)

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());

        startForeground(NOTIFICATION_ID, builder.build());
    }


    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My Notification Channel";
            String description = "Channel for my notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
