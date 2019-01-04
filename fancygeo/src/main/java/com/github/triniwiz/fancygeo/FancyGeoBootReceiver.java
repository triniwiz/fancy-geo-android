package com.github.triniwiz.fancygeo;

import android.content.Context;
import android.content.Intent;

/**
 * Created by Osei Fortune on 12/22/18
 */

public class FancyGeoBootReceiver extends android.content.BroadcastReceiver {
    private static boolean isSetup = false;
    @Override
    public void onReceive(Context context, Intent intent) {
        if(!isSetup){
            if (intent.getAction() != null) {
                String action = intent.getAction();
                if(action.equals("android.intent.action.BOOT_COMPLETED") || action.equals("android.intent.action.LOCKED_BOOT_COMPLETED")){
                    FancyGeo geo = new FancyGeo(context);
                    for (String fence : geo.getAllFences()) {
                        String type = FancyGeo.getType(fence);
                        if (type.equals("circle")) {
                            FancyGeo.CircleFence circleFence = FancyGeo.getGsonInstance().fromJson(fence, FancyGeo.CircleFence.class);
                            geo.restoreFenceCircle(circleFence.getId(), circleFence.getTransition(), circleFence.getLoiteringDelay(), circleFence.getCoordinates(), circleFence.getRadius(), circleFence.getNotification());
                        }
                    }
                    isSetup = true;
                }
            }
        }
    }
}
