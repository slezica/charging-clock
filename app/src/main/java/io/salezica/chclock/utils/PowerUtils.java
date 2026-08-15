package io.salezica.chclock.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.provider.Settings;

import static android.content.Context.POWER_SERVICE;

public class PowerUtils {

    // The direct-request dialog needs REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
    // which Play Store policy restricts. Opening the exemption list instead
    // requires no permission; the user picks the app manually.
    public static void openBatteryOptimizationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        context.startActivity(intent);
    }

    public static boolean isIgnoringBatteryOptimizations(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static boolean isPlugged(Context context) {
        Intent batteryStatus = context
            .registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        return batteryStatus != null
            && batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) > 0;
    }

}
