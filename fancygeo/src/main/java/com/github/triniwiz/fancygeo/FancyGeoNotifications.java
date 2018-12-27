package com.github.triniwiz.fancygeo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.tasks.Task;

/**
 * Created by triniwiz on 12/22/18
 */
public class FancyGeoNotifications extends ContextWrapper {
    public final static String CHANNEL_NAME_DEFAULT = "Fancy Geo Notifications";
    public final static String CHANNEL_NAME = "FANCY_GEO_NOTIFICATIONS_CHANNEL_NAME";
    public final static String CHANNEL_ID = "com.github.triniwiz.fancygeo";
    public final static String GEO_NOTIFICATION_ID = "geo_id";
    private SharedPreferences sharedPreferences;
    private NotificationManagerCompat mManager;
    private static FancyGeoNotifications mSelf;

    FancyGeoNotifications(Context base) {
        super(base);
        sharedPreferences = base.getSharedPreferences(FancyGeo.GEO_LOCATION_DATA, 0);
    }

    public static void init(Context context) {
        if (mSelf == null) {
            mSelf = new FancyGeoNotifications(context);
            mSelf.setupChannel();
        }
    }

    public static FancyGeoNotifications getInstance() {
        return mSelf;
    }

    private String getChannelName() {
        return sharedPreferences.getString(CHANNEL_NAME, CHANNEL_NAME_DEFAULT);
    }

    public void setChannelName(String name) {
        sharedPreferences.edit().putString(CHANNEL_NAME, name).apply();
    }

    public void setupChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel channel = manager.getNotificationChannel(getChannelName());
                Log.d(FancyGeo.TAG, "Channel Name: -> " + channel);
                if (channel == null) {
                    NotificationChannel newChannel = new NotificationChannel(CHANNEL_ID, getChannelName(), NotificationManager.IMPORTANCE_DEFAULT);
                    manager.createNotificationChannel(newChannel);
                }
            }
        }
    }

    private void forceMainActivityReload() {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        if (launchIntent != null) {
            Log.d(FancyGeo.TAG, "starting activity for package: " + getApplicationContext().getPackageName());
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(launchIntent);
        }
    }


    public static void sendNotification(FancyGeo.FenceNotification notification) {
        FancyGeoNotifications notifications = getInstance();
        Log.d(FancyGeo.TAG, "sendNotification");
        if (notifications != null) {
            Log.d(FancyGeo.TAG, "got Instance");
            NotificationCompat.Builder builder = Build.VERSION.SDK_INT >= 26 ? new NotificationCompat.Builder(notifications.getApplicationContext(), CHANNEL_ID) : new NotificationCompat.Builder(notifications.getApplicationContext());

            Log.d(FancyGeo.TAG, "NOTIFICATION: -> " + notification);
            String packageName = notifications.getApplicationInfo().packageName;

            int notify = notifications.getResources().getIdentifier("ic_stat_notify", "drawable", packageName);
            int appIcon = notifications.getApplicationInfo().icon;
            int sil = notifications.getResources().getIdentifier("ic_stat_notify_silhouette", "drawable", packageName);
            int icon;
            if (notify > 0) {
                icon = notify;
            } else if (sil > 0) {
                icon = sil;
            } else {
                icon = appIcon;
            }
            Log.d(FancyGeo.TAG, "Icon " + icon);
            Intent intent = new Intent(notifications.getApplicationContext(), FancyGeoActionReceiver.class);
            intent.putExtra(GEO_NOTIFICATION_ID, notification.getId());
            builder.setContentTitle(notification.getTitle())
                    .setSmallIcon(icon)
                    .setContentText(notification.getBody())
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(
                            PendingIntent.getService(
                                    notifications.getApplicationContext(),
                                    notification.getId(),
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            )
                    )
                    .setAutoCancel(true);

            notifications.getManager().notify(notification.getId(), builder.build());
        }
    }

    private NotificationManagerCompat getManager() {
        if (mManager == null) {
            mManager = NotificationManagerCompat.from(getApplicationContext());
        }
        return mManager;
    }
}
