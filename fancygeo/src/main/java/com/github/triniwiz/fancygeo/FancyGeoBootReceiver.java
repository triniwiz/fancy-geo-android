package com.github.triniwiz.fancygeo;

import android.content.Context;
import android.content.Intent;


/**
 * Created by triniwiz on 12/22/18
 */
public class FancyGeoBootReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            FancyGeo geo = new FancyGeo(context);
            for (String fence : geo.getAllFences()) {
                String type = FancyGeo.getType(fence);
                if (type.equals("circle")) {
                    FancyGeo.CircleFence circleFence = FancyGeo.getGsonInstance().fromJson(fence, FancyGeo.CircleFence.class);
                    geo.restoreFenceCircle(circleFence.getId(), circleFence.getTransition(), circleFence.getLoiteringDelay(), circleFence.getCoordinates(), circleFence.getRadius(), circleFence.getNotification());
                }
            }
        }
    }
}
