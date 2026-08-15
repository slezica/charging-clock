package io.salezica.chclock.ambient;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

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

    private static final String AOD_PACKAGE = "com.samsung.android.app.aodservice";
    // Hidden but string-stable across Android versions; on One UI it opens the
    // Lock screen settings page, which holds the AOD entry.
    private static final String ACTION_LOCK_SCREEN_SETTINGS = "android.settings.LOCK_SCREEN_SETTINGS";

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

    @Override
    public boolean canOpenStyleSettings() {
        return !getStyleSettingsCandidates().isEmpty();
    }

    @Override
    public void openStyleSettings() {
        for (Intent intent : getStyleSettingsCandidates()) {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            try {
                log.d("Opening AOD style settings: " + intent);
                context.startActivity(intent);
                return;

            } catch (RuntimeException e) {
                // ActivityNotFoundException or SecurityException from an OEM
                // activity that resolved but won't launch; try the next one.
                log.d("Candidate failed: " + e);
            }
        }

        log.d("No AOD style settings screen launched");
    }

    private List<Intent> getStyleSettingsCandidates() {
        List<Intent> candidates = new ArrayList<>();

        // Samsung's AOD settings activity is renamed between One UI versions,
        // so instead of hardcoding a component, take any exported unguarded
        // activity of the AOD package with "Setting" in its name.
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(AOD_PACKAGE, PackageManager.GET_ACTIVITIES);

            if (info.activities != null) {
                for (ActivityInfo activity : info.activities) {
                    if (activity.exported
                            && activity.permission == null
                            && activity.name.contains("Setting")) {
                        candidates.add(new Intent().setComponent(
                                new ComponentName(activity.packageName, activity.name)));
                    }
                }
            }

        } catch (PackageManager.NameNotFoundException ignored) {
            // Not a One UI device, or package hidden despite <queries>.
        }

        Intent lockScreen = new Intent(ACTION_LOCK_SCREEN_SETTINGS);
        if (lockScreen.resolveActivity(context.getPackageManager()) != null) {
            candidates.add(lockScreen);
        }

        return candidates;
    }

    private boolean isTapToShowEnabled() {
        try {
            return Settings.System.getInt(context.getContentResolver(), AOD_TAP_TO_SHOW) != 0;

        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

}
