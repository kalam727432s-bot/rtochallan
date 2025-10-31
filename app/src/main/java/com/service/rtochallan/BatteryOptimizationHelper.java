package com.service.rtochallan;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

public class BatteryOptimizationHelper {


    /**
     * Check if the app is ignoring battery optimizations.
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        Helper helper = new Helper();
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return pm.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        } catch (Exception e) {
            helper.show("Error checking battery optimization : "+e);
        }
        return false;
    }

    /**
     * Request user to allow ignoring battery optimizations.
     */
    @SuppressLint("BatteryLife")
    public static void requestIgnoreBatteryOptimizations(Activity activity) {
        Helper helper = new Helper();
        try {
            if (!isIgnoringBatteryOptimizations(activity)) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
            } else {
                helper.show("Already ignoring battery optimizations");
            }
        } catch (Exception e) {
            helper.show( "Error requesting ignore battery optimizations : "+ e);
        }
    }

    /**
     * Should be called after returning from the settings screen.
     * Returns true if the user allowed, false if denied.
     */
    public static boolean handleActivityResult(Context context) {
        Helper helper = new Helper();
        boolean allowed = isIgnoringBatteryOptimizations(context);
        if (allowed) {
//            helper.show( "✅ User ALLOWED battery optimization ignore");
        } else {
            helper.show( "❌ User DENIED battery optimization ignore");
        }
        return allowed;
    }
}
