package io.salezica.chclock.ui;

import io.salezica.chclock.utils.TaggedLog;

/**
 * In-memory presenter for screenshots and UI development. Selected in debug
 * builds via the "preset" intent extra (see MainActivity). Actions mutate
 * memory or no-op; no system settings are touched.
 */
public class MockMainPresenter implements MainPresenter {

    public static final String[] PRESETS = {
        "perfect", "unplugged", "no-permission", "wrong-style",
        "battery-restricted", "not-supported", "disabled", "all-warnings",
    };

    private final TaggedLog log = new TaggedLog(this);

    private boolean enabled = true;
    private boolean plugged = true;
    private boolean supported = true;
    private boolean permission = true;
    private boolean alwaysOn = true;
    private String aodStyle = "Always";
    private boolean batteryUnrestricted = true;

    public static MockMainPresenter fromPreset(String preset) {
        MockMainPresenter p = new MockMainPresenter();

        switch (preset) {
            case "perfect":
                break;
            case "unplugged":
                p.plugged = false;
                p.alwaysOn = false;
                break;
            case "no-permission":
                p.permission = false;
                break;
            case "wrong-style":
                p.aodStyle = "Tap to show";
                break;
            case "battery-restricted":
                p.batteryUnrestricted = false;
                break;
            case "not-supported":
                p.supported = false;
                break;
            case "disabled":
                p.enabled = false;
                p.alwaysOn = false;
                break;
            case "all-warnings":
                // Every warning at once, except not-supported (hides the rest):
                p.permission = false;
                p.alwaysOn = false;          // mismatch: plugged + enabled but AOD off
                p.aodStyle = "Tap to show";
                p.batteryUnrestricted = false;
                break;
            default:
                p.log.d("Unknown preset '" + preset + "', using perfect");
        }

        return p;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.alwaysOn = enabled && plugged;
    }

    @Override
    public boolean isPlugged() {
        return plugged;
    }

    @Override
    public boolean isSupported() {
        return supported;
    }

    @Override
    public boolean hasPermission() {
        return permission;
    }

    @Override
    public void requestPermission() {
        log.d("requestPermission (no-op)");
        permission = true;
    }

    @Override
    public boolean isAlwaysOn() {
        return alwaysOn;
    }

    @Override
    public String getAodStyle() {
        return aodStyle;
    }

    @Override
    public boolean canFixAodStyle() {
        return true;
    }

    @Override
    public void fixAodStyle() {
        log.d("fixAodStyle (no-op)");
        aodStyle = "Always";
    }

    @Override
    public boolean isIgnoringBatteryOptimizations() {
        return batteryUnrestricted;
    }

    @Override
    public void openBatteryOptimizationSettings() {
        log.d("openBatteryOptimizationSettings (no-op)");
        batteryUnrestricted = true;
    }
}
