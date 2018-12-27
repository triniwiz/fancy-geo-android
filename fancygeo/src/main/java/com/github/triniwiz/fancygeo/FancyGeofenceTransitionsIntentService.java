package com.github.triniwiz.fancygeo;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.gson.Gson;

import java.util.List;


/**
 * Created by triniwiz on 12/13/18
 */
public class FancyGeofenceTransitionsIntentService extends IntentService {

    FancyGeofenceTransitionsIntentService(String name) {
        super(name);
    }

    public FancyGeofenceTransitionsIntentService() {
        super("FancyGeofenceTransitions");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Gson gson = FancyGeo.getGsonInstance();
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent != null && geofencingEvent.hasError()) {
           /* String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());*/
            //Log.e(FancyGeo.TAG, geofencingEvent.getErrorCode());
            Log.e(FancyGeo.TAG, "fencing event Error");
            return;
        }
        int geofenceTransition = geofencingEvent != null ? geofencingEvent.getGeofenceTransition() : -1;
        List<Geofence> triggeringGeofences = geofencingEvent != null ? geofencingEvent.getTriggeringGeofences() : null;
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(FancyGeo.GEO_LOCATION_DATA, 0);
        switch (geofenceTransition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Log.d(FancyGeo.TAG, "GEOFENCE_TRANSITION_ENTER");
                if (triggeringGeofences != null) {
                    for (Geofence fence : triggeringGeofences) {
                        String id = fence.getRequestId();
                        String request = preferences.getString(id, "");
                        String type =   FancyGeo.getType(request);
                        if (!request.isEmpty() && type.equals("circle")) {
                            FancyGeo.CircleFence geoFence = gson.fromJson(request, FancyGeo.CircleFence.class);
                            FancyGeoNotifications.sendNotification(geoFence.getNotification());
                        }
                    }
                }
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                Log.d(FancyGeo.TAG, "GEOFENCE_TRANSITION_DWELL");
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Log.d(FancyGeo.TAG, "GEOFENCE_TRANSITION_EXIT");
                if (triggeringGeofences != null) {
                    for (Geofence fence : triggeringGeofences) {
                        String id = fence.getRequestId();
                        String request = preferences.getString(id, "");
                        Log.d(FancyGeo.TAG, "request: " + FancyGeo.getType(request));
                        Log.d(FancyGeo.TAG, "requestId : " + id);
                        String type = FancyGeo.getType(request);
                        Log.d(FancyGeo.TAG, "isCircle: " + String.valueOf(type.equals("circle")));
                        if (!request.isEmpty() && type.equals("circle")) {
                            FancyGeo.CircleFence geoFence = gson.fromJson(request, FancyGeo.CircleFence.class);
                            FancyGeoNotifications.sendNotification(geoFence.getNotification());
                        }

                    }
                }
                break;
        }

    }
}
