package com.github.triniwiz.fancygeo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_DWELL;
import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER;
import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

/**
 * Created by triniwiz on 12/13/18
 */
public class FancyGeo {
    static final String TAG = "triniwiz.fancygeo";
    static final String GEO_LOCATION_DATA = "FANCY_GEO_LOCATION_DATA";
    public static final int GEO_LOCATION_PERMISSIONS_REQUEST = 138;
    private static final String[] GEO_LOCATION_PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    private FusedLocationProviderClient mFusedLocationClient;

    private GeofencingClient mGeofencingClient;
    private Context ctx;
    private PendingIntent mGeofencePendingIntent;
    private SharedPreferences sharedPreferences;
    private static Gson gson;

    public static interface FenceListener {
        public void onCreate(String id);

        public void onFail(Exception e);

        public void onRemove(String id);

        public void onExpire(String id);
    }

    public static Gson getGsonInstance() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }

    public static enum FenceTransition {
        ENTER,
        DWELL,
        EXIT,
        ENTER_EXIT,
        ENTER_DWELL,
        DWELL_EXIT,
        ALL
    }

    public static class FenceNotification {
        private int id;
        private String title;
        private String body;

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    public static class FenceHttp {
    }

    public static abstract class FenceShape {

        public abstract FenceTransition getTransition();

        public abstract int getLoiteringDelay();

        public abstract double[] getCoordinates();

        public abstract String getId();

        public abstract String getType();

        public abstract String toJson();

        public abstract FenceNotification getNotification();

        public abstract void setNotification(FenceNotification notification);
    }

    public static class CircleFence extends FenceShape {
        private String id;
        private double[] coordinates;
        private double radius;
        private String type;
        private FenceTransition transition;
        private int loiteringDelay;
        private FenceNotification notification;

        CircleFence(String id, FenceTransition transition, double[] coordinates, double radius, int loiteringDelay) {
            super();
            this.id = id;
            this.coordinates = coordinates;
            this.radius = radius;
            this.type = "circle";
            this.transition = transition;
            this.loiteringDelay = loiteringDelay;
        }

        CircleFence(String id, FenceTransition transition, double[] coordinates, double radius, int loiteringDelay, FenceNotification notification) {
            super();
            this.id = id;
            this.coordinates = coordinates;
            this.radius = radius;
            this.type = "circle";
            this.transition = transition;
            this.loiteringDelay = loiteringDelay;
            this.notification = notification;
        }

        static public CircleFence fromString(String json){
            return getGsonInstance().fromJson(json, CircleFence.class);
        }
        @Override
        public FenceTransition getTransition() {
            return transition;
        }

        @Override
        public int getLoiteringDelay() {
            return this.loiteringDelay;
        }

        public double getRadius() {
            return radius;
        }

        @Override
        public double[] getCoordinates() {
            return coordinates;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getType() {
            return type;
        }

        public String toJson() {
            return getGsonInstance().toJson(this);
        }

        @Override
        public FenceNotification getNotification() {
            return notification;
        }

        @Override
        public void setNotification(FenceNotification notification) {
            this.notification = notification;
        }
    }

    public static String getType(String json) {
        if (json.isEmpty()) {
            return "";
        }
        int typeIndex = json.indexOf("type");
        String typeSubStr = json.substring(typeIndex);
        int separator;
        if (typeSubStr.contains("}")) {
            separator = typeSubStr.indexOf("}");
        } else {
            separator = typeSubStr.indexOf(",");
        }
        int removeColon = typeSubStr.indexOf(":") + 1;
        String quote = "\"";

        return typeSubStr.substring(removeColon, separator).replace(quote, "").replace(quote, "");
    }

    public FancyGeo(Context context) {
        ctx = context;
        mGeofencingClient = LocationServices.getGeofencingClient(ctx);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx);
        sharedPreferences = context.getSharedPreferences(GEO_LOCATION_DATA, 0);
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(ctx, FancyGeofenceTransitionsIntentService.class);
        mGeofencePendingIntent = PendingIntent.getService(ctx, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    public static void requestPermission(Context context) {
        ActivityCompat.requestPermissions((Activity) context, GEO_LOCATION_PERMISSIONS, GEO_LOCATION_PERMISSIONS_REQUEST);
    }

    public static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == (PackageManager.PERMISSION_GRANTED);
        }
        return true;
    }

    public static class FancyLocation {
        private Location location;
        double latitude;
        double longitude;
        double altitude;
        float horizontalAccuracy;
        float verticalAccuracy;
        float speed;
        float direction;
        long timestamp;

        private FancyLocation() {
        }

        public static FancyLocation fromLocation(Location location) {
            FancyLocation fancyLocation = new FancyLocation();
            fancyLocation.location = location;
            fancyLocation.latitude = location.getLatitude();
            fancyLocation.longitude = location.getLongitude();
            fancyLocation.altitude = location.getAltitude();
            fancyLocation.horizontalAccuracy = location.getAccuracy();
            fancyLocation.verticalAccuracy = location.getAccuracy();
            fancyLocation.speed = location.getSpeed();
            fancyLocation.direction = location.getBearing();
            fancyLocation.timestamp = location.getTime();
            return fancyLocation;
        }

        public String toJson() {
            return getGsonInstance().toJson(this);
        }

        public Location getLocation() {
            return location;
        }
    }

    public static interface FancyGeoCurrentLocationListener {
        public void onLocation(FancyLocation location);

        public void onLocationError(Exception exception);
    }

    @SuppressLint("MissingPermission")
    public void getCurrentLocation(final FancyGeoCurrentLocationListener listener) {
        final Task<Location> lastLocation = mFusedLocationClient.getLastLocation();
        final LocationCallback callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    FancyLocation fancyLocation = FancyLocation.fromLocation(location);
                    //if(fancyLocation)
                    listener.onLocation(fancyLocation);
                    break;
                }
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {

            }
        };
        lastLocation.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    listener.onLocation(FancyLocation.fromLocation(location));
                } else {
                    LocationRequest request = new LocationRequest();
                    request.setInterval(60000);
                    request.setFastestInterval(5000);
                    request.setPriority(LocationRequest.PRIORITY_LOW_POWER);
                    mFusedLocationClient.requestLocationUpdates(request, callback, null);
                }
            }
        });

        lastLocation.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                listener.onLocationError(e);
            }
        });
    }

    public ArrayList<String> getAllFences() {
        ArrayList<String> list = new ArrayList<>();
        for (String key : sharedPreferences.getAll().keySet()) {
            String value = sharedPreferences.getString(key, "");
            list.add(value);
        }
        return list;
    }

    public void removeAllFences() {
        Task<Void> removeTask = mGeofencingClient.removeGeofences(getGeofencePendingIntent());
        removeTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
        removeTask.addOnCanceledListener(new OnCanceledListener() {
            @Override
            public void onCanceled() {
                sharedPreferences.edit().clear().apply();
            }
        });
    }

    @SuppressLint("MissingPermission")
    void restoreFenceCircle(final @Nullable String id, final FenceTransition transition, final int loiteringDelay, final double[] points, final double radius, @Nullable final FenceNotification notification) {
        final String requestId = id == null ? UUID.randomUUID().toString() : id;
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        Geofence.Builder fenceBuilder = new Geofence.Builder();
        fenceBuilder.setRequestId(requestId);
        fenceBuilder.setCircularRegion(points[0], points[1], (float) radius);
        switch (transition) {
            case ENTER:
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_ENTER);
                break;
            case EXIT:
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_EXIT);
                break;
            case DWELL:
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_DWELL);
                fenceBuilder.setLoiteringDelay(loiteringDelay);
                break;
            case ENTER_EXIT:
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_ENTER | GEOFENCE_TRANSITION_EXIT);
                break;
            case ENTER_DWELL:
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_ENTER | GEOFENCE_TRANSITION_DWELL);
                fenceBuilder.setLoiteringDelay(loiteringDelay);
                break;
            case DWELL_EXIT:
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_DWELL | GEOFENCE_TRANSITION_EXIT);
                fenceBuilder.setLoiteringDelay(loiteringDelay);
                break;
            case ALL:
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_ENTER | GEOFENCE_TRANSITION_DWELL | GEOFENCE_TRANSITION_EXIT);
                break;
        }

        fenceBuilder.setExpirationDuration(NEVER_EXPIRE);

        builder.addGeofence(fenceBuilder.build());
        if (!hasPermission(ctx)) {
            return;
        }
        GeofencingRequest request = builder.build();
        mGeofencingClient.addGeofences(request, getGeofencePendingIntent());
    }

    @SuppressLint("MissingPermission")
    public void createFenceCircle(final @Nullable String id, final FenceTransition transition, final int loiteringDelay, final double[] points, final double radius, @Nullable final FenceNotification notification, @Nullable final FenceListener listener) {
        final String requestId = id == null ? UUID.randomUUID().toString() : id;
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        Geofence.Builder fenceBuilder = new Geofence.Builder();
        fenceBuilder.setRequestId(requestId);
        fenceBuilder.setCircularRegion(points[0], points[1], (float) radius);
        switch (transition) {
            case ENTER:
                builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_ENTER);
                break;
            case EXIT:
                // builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT);
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_EXIT);
                break;
            case DWELL:
                builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_DWELL);
                fenceBuilder.setLoiteringDelay(loiteringDelay);
                break;
            case ENTER_EXIT:
                builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_ENTER | GEOFENCE_TRANSITION_EXIT);
                break;
            case ENTER_DWELL:
                builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL);
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_ENTER | GEOFENCE_TRANSITION_DWELL);
                fenceBuilder.setLoiteringDelay(loiteringDelay);
                break;
            case DWELL_EXIT:
                builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_DWELL | GEOFENCE_TRANSITION_EXIT);
                fenceBuilder.setLoiteringDelay(loiteringDelay);
                break;
            case ALL:
                builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL);
                fenceBuilder.setTransitionTypes(GEOFENCE_TRANSITION_ENTER | GEOFENCE_TRANSITION_DWELL | GEOFENCE_TRANSITION_EXIT);
                break;
        }

        fenceBuilder.setExpirationDuration(NEVER_EXPIRE);

        builder.addGeofence(fenceBuilder.build());
        if (!hasPermission(ctx)) {
            return;
        }
        GeofencingRequest request = builder.build();
        Log.d(TAG, request.toString());
        Task<Void> addTask = mGeofencingClient.addGeofences(request, getGeofencePendingIntent());
        addTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onSuccess(Void aVoid) {
                CircleFence circleFence;
                if (notification != null) {
                    circleFence = new CircleFence(requestId, transition, points, radius, loiteringDelay, notification);
                } else {
                    circleFence = new CircleFence(requestId, transition, points, radius, loiteringDelay);
                }
                sharedPreferences.edit().putString(requestId, circleFence.toJson()).commit();
                Log.d(TAG, "SUCCESS");
                if (listener != null) {
                    listener.onCreate(requestId);
                }
            }
        });
        addTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "FAILURE : " + e.getLocalizedMessage() + "addOnFailureListener");
                if (listener != null) {
                    listener.onFail(e);
                }
            }
        });
    }

    public void removeFence(final String id, final @Nullable FenceListener listener) {
        List<String> list = new ArrayList<String>();
        list.add(id);
        Task<Void> removeTask = mGeofencingClient.removeGeofences(list);
        removeTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (listener != null) {
                    listener.onFail(e);
                }
            }
        });

        removeTask.addOnCanceledListener(new OnCanceledListener() {
            @Override
            public void onCanceled() {
                if (listener != null) {
                    listener.onRemove(id);
                }
            }
        });
    }
}