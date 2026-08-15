package io.salezica.chclock.ambient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import io.salezica.chclock.utils.TaggedLog;

public class AmbientSamsung implements Ambient {

    private final TaggedLog log = new TaggedLog(this);

    // Samsung ships its own AOD implementation (com.samsung.android.app.aodservice)
    // controlled through Settings.System, not the AOSP doze_always_on secure setting.
    // Third-party apps can only write the aod_mode master switch: the style keys
    // (e.g. aod_tap_to_show_mode) are rejected by the settings provider even with
    // WRITE_SECURE_SETTINGS, so the AOD style must be chosen once in Samsung settings.
    private static final String AOD_MODE = "aod_mode";
    private static final String AOD_TAP_TO_SHOW = "aod_tap_to_show_mode";

    private final Context context;

    public AmbientSamsung(Context context) {
        this.context = context;
    }

    @Override
    public String getStyle() {
        if (getSetting(AOD_TAP_TO_SHOW) != 0) return "Tap to show";
        if (getSetting("aod_display_mode_auto") != 0) return "Auto";
        if (getSetting("aod_show_for_new_noti") != 0) return "New notifications";
        if (getSetting("aod_mode_start_time") != getSetting("aod_mode_end_time")) return "Scheduled";
        return "Always";
    }

    private int getSetting(String name) {
        return Settings.System.getInt(context.getContentResolver(), name, 0);
    }

    @Override
    public boolean hasPermissions() {
        // Settings.System writes are gated by the WRITE_SETTINGS appop,
        // not a runtime permission.
        return Settings.System.canWrite(context);
    }

    @Override
    public void requestPermissions() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                .setData(Uri.parse("package:" + context.getPackageName()));

        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        log.d("Opening WRITE_SETTINGS grant screen");
        context.startActivity(intent);
    }

    @Override
    public boolean isSupported() {
        try {
            Settings.System.getInt(context.getContentResolver(), AOD_MODE);
            return true;

        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    @Override
    public void setAlwaysOn(boolean alwaysOn) {
        log.d("Setting Samsung AOD always on: " + alwaysOn);

        if (alwaysOn && isTapToShowEnabled()) {
            log.d("Warning: AOD style is tap-to-show; set it to Always in Samsung settings");
        }

        if (alwaysOn != isAlwaysOn()) {
            Settings.System.putInt(context.getContentResolver(), AOD_MODE, alwaysOn ? 1 : 0);
        }
    }

    @Override
    public boolean isAlwaysOn() {
        try {
            return Settings.System.getInt(context.getContentResolver(), AOD_MODE) != 0;

        } catch (Settings.SettingNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isTapToShowEnabled() {
        try {
            return Settings.System.getInt(context.getContentResolver(), AOD_TAP_TO_SHOW) != 0;

        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

}
