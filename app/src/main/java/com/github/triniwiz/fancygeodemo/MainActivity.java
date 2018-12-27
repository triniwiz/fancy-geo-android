package com.github.triniwiz.fancygeodemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.github.triniwiz.fancygeo.FancyGeo;
import com.github.triniwiz.fancygeo.FancyGeoNotifications;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private SupportMapFragment mapFragment;
    private FancyGeo fancyGeo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fancyGeo = new FancyGeo(this);
        setContentView(R.layout.activity_main);
        FancyGeoNotifications.init(this);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        final Activity that = this;

        if (FancyGeo.hasPermission(this)) {
            googleMap.setMyLocationEnabled(true);
        }
        LatLng latLng = new LatLng(37.422, -122.084);
        CameraPosition position = new CameraPosition(latLng, 12, 0, 0);
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(position);
        googleMap.moveCamera(update);

        for (String fence : fancyGeo.getAllFences()) {
            String type = FancyGeo.getType(fence);
            if (type.equals("circle")) {
                FancyGeo.CircleFence circleFence = FancyGeo.CircleFence.fromString(fence);
                Log.d("triniwiz.fancydemo", "Restored: " + circleFence.getId());
                CircleOptions circleOptions = new CircleOptions();
                double[] cords = circleFence.getCoordinates();
                circleOptions.center(new LatLng(cords[0], cords[1]));
                circleOptions.radius(circleFence.getRadius());
                googleMap.addCircle(circleOptions);
            }
        }
        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng latLng) {
                System.out.println("Has permission: " + FancyGeo.hasPermission(that));
                if (FancyGeo.hasPermission(that)) {
                    double[] points = {latLng.latitude, latLng.longitude};
                    FancyGeo.FenceNotification notification = new FancyGeo.FenceNotification();
                    notification.setId(1);
                    notification.setBody("Some Title");
                    notification.setTitle("SomeBody");
                    fancyGeo.createFenceCircle(null, FancyGeo.FenceTransition.ENTER_EXIT, 0, points, 1000, notification, new FancyGeo.FenceListener() {
                        @Override
                        public void onCreate(String id) {
                            Log.d("triniwiz.fancydemo", "Created: " + id);
                            CircleOptions circleOptions = new CircleOptions();
                            circleOptions.center(latLng);
                            circleOptions.radius(1000);
                            googleMap.addCircle(circleOptions);
                        }

                        @Override
                        public void onFail(Exception e) {

                        }

                        @Override
                        public void onRemove(String id) {

                        }

                        @Override
                        public void onExpire(String id) {

                        }
                    });
                } else {
                    FancyGeo.requestPermission(that);
                }
            }
        });
    }

}
