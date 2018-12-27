package com.github.triniwiz.fancygeo;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by triniwiz on 12/24/18
 */
public class FancyGeoActionReceiver extends IntentService {

    public FancyGeoActionReceiver() {
        super("FancyGeoActionReceiver");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(FancyGeo.TAG, "FancyGeoActionReceiver: onHandleIntent");
    }
}
