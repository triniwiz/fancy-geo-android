package com.github.triniwiz.fancygeo;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Osei Fortune on 12/28/18
 */
public class FancyGeoLifeCycle implements Application.ActivityLifecycleCallbacks {


    private static FancyGeoLifeCycle lifeCycle = new FancyGeoLifeCycle();


    public static void registerCallbacks(Application app) {
        if (app == null) {
            Log.d("LifecycleCallbacks", "The application is null, it's not passed correctly!");
            throw new RuntimeException("The application is null, it's not passed correctly!");
        }

        // clean up, not to leak and register it N times...
        Log.d("LifecycleCallbacks", "Unregistering the activity lifecycle callbacks...");
        app.unregisterActivityLifecycleCallbacks(lifeCycle);

        Log.d("LifecycleCallbacks", "Registering the activity lifecycle callbacks...");
        app.registerActivityLifecycleCallbacks(lifeCycle);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        FancyGeo.isActive = true;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        FancyGeo.isActive = false;
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
