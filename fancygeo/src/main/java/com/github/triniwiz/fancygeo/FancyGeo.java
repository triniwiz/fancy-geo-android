package com.github.triniwiz.fancygeo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_DWELL;
import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER;
import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

/**
 * Created by Osei Fortune on 12/13/18
 */
public class FancyGeo {
    static final String TAG = "triniwiz.fancygeo";
    static final String GEO_LOCATION_DATA = "FANCY_GEO_LOCATION_DATA";
    public static final int GEO_LOCATION_PERMISSIONS_REQUEST = 138;
    private static final String[] GEO_LOCATION_PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    public static final String GEO_TRANSITION_TYPE = "TRANSITION_TYPE";
    private FusedLocationProviderClient mFusedLocationClient;
    public static int defaultGetLocationTimeout = 5 * 60 * 1000; // 5 minutes
    public static double minRangeUpdate = 0.1; // 0 meters
    public static int minTimeUpdate = 1 * 60 * 1000; // 1 minute
    public static int fastestTimeUpdate = 5 * 1000; // 5 secs
    private GeofencingClient mGeofencingClient;
    private Context ctx;
    private static PendingIntent mGeofencePendingIntent;
    private SharedPreferences sharedPreferences;
    private static Gson gson;
    static boolean isActive;
    private static String cachedData;

    private static NotificationsListener onMessageReceivedListener;

    public interface Callback {
        void onSuccess();

        void onFail(Exception e);
    }

    public interface FenceCallback {
        void onSuccess(String id);

        void onFail(Exception e);
    }

    public static void init(Application application) {
        FancyGeoLifeCycle.registerCallbacks(application);
        FancyGeoNotifications.init(application.getApplicationContext());
    }

    public static Gson getGsonInstance() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }

    public enum FenceTransition {
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
        private String requestId;

        public String getRequestId() {
            return requestId;
        }

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

        void setRequestId(String requestId) {
            this.requestId = requestId;
        }
    }

    public interface NotificationsListener {
        void onSuccess(String data);

        void onError(Exception e);
    }

    public static void setOnMessageReceivedListener(NotificationsListener listener) {
        onMessageReceivedListener = listener;

        if (cachedData != null) {
            executeOnMessageReceivedListener(cachedData);
            cachedData = null;
        }
    }

    public static void executeOnMessageReceivedListener(String data) {
        if (onMessageReceivedListener != null) {
            Log.d(TAG, "Sending message to client");
            onMessageReceivedListener.onSuccess(data);
        } else {
            Log.d(TAG, "No callback function - caching the data for later retrieval.");
            cachedData = data;
        }
    }


    public static class FenceHttp {
        // TODO
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

        static public CircleFence fromString(String json) {
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


    public static class FancyLocationOptions {
        public int desiredAccuracy;


        public int updateDistance;


        public int updateTime;


        public int minimumUpdateTime;


        public int maximumAge;


        public int timeout;

        public FancyLocationOptions() {
        }
    }

    public enum FancyLocationAccuracy {
        any(300),
        high(3);
        private final int accuracy;

        FancyLocationAccuracy(int accuracy) {
            this.accuracy = accuracy;
        }
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

    public interface FancyGeoCurrentLocationListener {
        void onLocation(FancyLocation location);

        void onLocationError(String error);
    }


    @SuppressLint("MissingPermission")
    public void getCurrentLocation(@Nullable final FancyLocationOptions options, final FancyGeoCurrentLocationListener listener) {
        final LocationCallback callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    FancyLocation loc = FancyLocation.fromLocation(locationResult.getLastLocation());
                    int maxAge = options != null ? options.maximumAge : -1;
                    if (maxAge > -1) {
                        if (loc.timestamp + maxAge > new Date().getTime()) {
                            listener.onLocation(loc);
                        } else {
                            listener.onLocationError("Last known location too old!");
                        }
                    } else {
                        listener.onLocation(loc);
                    }
                } else {
                    listener.onLocationError("There is no last known location!");
                }

            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {

            }
        };
        if (options != null && options.timeout == 0) {
            final Task<Location> lastLocation = mFusedLocationClient.getLastLocation();
            lastLocation.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        FancyLocation loc = FancyLocation.fromLocation(location);
                        int maxAge = options != null ? options.maximumAge : -1;
                        if (maxAge > -1) {
                            if (loc.timestamp + maxAge > new Date().getTime()) {
                                listener.onLocation(loc);
                            } else {
                                listener.onLocationError("Last known location too old!");
                            }
                        } else {
                            listener.onLocation(loc);
                        }
                    } else {
                        listener.onLocationError("There is no last known location!");
                    }
                }
            });

            lastLocation.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    listener.onLocationError(e.getMessage());
                }
            });
        } else {
            LocationRequest request = new LocationRequest();
            int updateTime = options != null ? options.updateTime : minTimeUpdate;
            request.setInterval(updateTime);
            int minimumUpdateTime = options != null ? options.minimumUpdateTime : Math.min(updateTime, fastestTimeUpdate);
            request.setFastestInterval(minimumUpdateTime);
            if (options != null) {
                request.setSmallestDisplacement(options.updateDistance);
            }
            if (options != null && options.desiredAccuracy == FancyLocationAccuracy.high.accuracy) {
                request.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);
            } else {
                request.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            }
            mFusedLocationClient.requestLocationUpdates(request, callback, null);
        }
    }

    public ArrayList<String> getAllFences() {
        ArrayList<String> list = new ArrayList<>();
        for (String key : sharedPreferences.getAll().keySet()) {
            String value = sharedPreferences.getString(key, "");
            list.add(value);
        }
        return list;
    }

    public void removeAllFences(@Nullable final Callback callback) {
        Task<Void> removeTask = mGeofencingClient.removeGeofences(getGeofencePendingIntent());
        removeTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (callback != null) {
                    callback.onFail(e);
                }
            }
        });

        removeTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                sharedPreferences.edit().clear().apply();
                if (callback != null) {
                    callback.onSuccess();
                }
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
    public void createFenceCircle(final @Nullable String id, final FenceTransition transition, final int loiteringDelay, final double[] points, final double radius, @Nullable final FenceNotification notification, @Nullable final FenceCallback callback) {
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
        Task<Void> addTask = mGeofencingClient.addGeofences(request, getGeofencePendingIntent());
        addTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onSuccess(Void aVoid) {
                CircleFence circleFence;
                if (notification != null) {
                    notification.setRequestId(requestId);
                    circleFence = new CircleFence(requestId, transition, points, radius, loiteringDelay, notification);
                } else {
                    circleFence = new CircleFence(requestId, transition, points, radius, loiteringDelay);
                }
                sharedPreferences.edit().putString(requestId, circleFence.toJson()).commit();
                if (callback != null) {
                    callback.onSuccess(requestId);
                }
            }
        });
        addTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (callback != null) {
                    callback.onFail(e);
                }
            }
        });
    }

    public void removeFence(final String id, final @Nullable Callback callback) {
        List<String> list = new ArrayList<>();
        list.add(id);
        Task<Void> removeTask = mGeofencingClient.removeGeofences(list);
        removeTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (callback != null) {
                    callback.onFail(e);
                }
            }
        });

        removeTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }
        });
    }

    public FenceShape getFence(String id) {
        String fence = sharedPreferences.getString(id, "");
        if (fence.equals("")) {
            return null;
        }
        String type = getType(fence);
        switch (type) {
            case "circle":
                return getGsonInstance().fromJson(fence, CircleFence.class);
            default:
                return null;
        }
    }
}
