package com.github.triniwiz.fancygeo;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by Osei Fortune on 12/24/18
 */
public class FancyGeoActionReceiver extends IntentService {

    public FancyGeoActionReceiver() {
        super("FancyGeoActionReceiver");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }
        int notificationId = (int) bundle.get(FancyGeoNotifications.GEO_NOTIFICATION_ID);
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(FancyGeo.GEO_LOCATION_DATA, 0);
        for (String key : preferences.getAll().keySet()) {
            String value = preferences.getString(key, "");
            if (!value.equals("")) {
                String type = FancyGeo.getType(value);
                switch (type) {
                    case "circle":
                        FancyGeo.CircleFence fenceShape = FancyGeo.getGsonInstance().fromJson(value, FancyGeo.CircleFence.class);
                        if (fenceShape.getNotification().getId() == notificationId) {
                            FancyGeo.executeOnMessageReceivedListener(FancyGeo.getGsonInstance().toJson(fenceShape.getNotification()));
                            break;
                        }
                        break;
                }
            }
        }

    }
}
