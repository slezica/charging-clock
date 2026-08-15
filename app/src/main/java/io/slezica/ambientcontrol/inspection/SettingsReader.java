package io.slezica.ambientcontrol.inspection;

import android.content.ContentResolver;
import android.os.Handler;
import android.provider.Settings;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SettingsReader {

    public interface OnChangeListener {
        void onChange(String name, String oldValue, String newValue);
    }

    private final ContentResolver cr;

    private Map<String, String> lastSeenSettings;

    public SettingsReader(ContentResolver cr) {
        this.cr = cr;
    }

    public void startWatchingChanges(OnChangeListener onChange) {
        checkChangesEvery(onChange, 1000);
    }

    private void checkChangesEvery(OnChangeListener onChange, int delay) {
        checkChanges(onChange);
        new Handler().postDelayed(() -> checkChangesEvery(onChange, delay), delay);
    }

    private void checkChanges(OnChangeListener onChange) {
        Map<String, String> currentSettings = getSettings();

        if (lastSeenSettings != null) {
            for (String name: currentSettings.keySet()) {
                String currentValue = currentSettings.get(name);
                String previousValue = lastSeenSettings.get(name);

                if (!currentValue.equals(previousValue)) {
                    onChange.onChange(name, previousValue, currentValue);
                }
            }
        }

        lastSeenSettings = currentSettings;
    }

    public <T> Map<String, String> getSettings() {
        Map<String, String> keyValues = new HashMap<>();

        for (String name: getStaticStrings(Settings.Global.class)) {
            String value = getGlobalSettingValue(name);
            if (value != null) keyValues.put("global:" + name, value);
        }

        for (String name: getStaticStrings(Settings.System.class)) {
            String value = getSystemSettingValue(name);
            if (value != null) keyValues.put("system:" + name, value);
        }

        for (String name: getStaticStrings(Settings.Secure.class)) {
            String value = getSecureSettingValue(name);
            if (value != null) keyValues.put("secure:" + name, value);
        }

        return keyValues;
    }

    private String getGlobalSettingValue(String settingName) {
        try {
            return "" + Settings.Global.getInt(cr, settingName);
        } catch (Settings.SettingNotFoundException | SecurityException e) {}

        try {
            return "" + Settings.Global.getFloat(cr, settingName);
        } catch (Settings.SettingNotFoundException | SecurityException e) {}

        try {
            return "" + Settings.Global.getLong(cr, settingName);
        } catch (Settings.SettingNotFoundException | SecurityException e) {}

        try {
            return Settings.Global.getString(cr, settingName);
        } catch (SecurityException e) {
            return "SecurityException!";
        }
    }

    private String getSystemSettingValue(String settingName) {
        try {
            return "" + Settings.System.getInt(cr, settingName);
        } catch (Settings.SettingNotFoundException | SecurityException e) {}

        try {
            return "" + Settings.System.getFloat(cr, settingName);
        } catch (Settings.SettingNotFoundException | SecurityException e) {}

        try {
            return "" + Settings.System.getLong(cr, settingName);
        } catch (Settings.SettingNotFoundException | SecurityException e) {}

        try {
            return Settings.System.getString(cr, settingName);
        } catch (SecurityException e) {
            return "SecurityException!";
        }
    }

    private String getSecureSettingValue(String settingName) {
        try {
            return "" + Settings.Secure.getInt(cr, settingName);
        } catch (Settings.SettingNotFoundException | SecurityException e) {}

        try {
            return "" + Settings.Secure.getFloat(cr, settingName);
        } catch (Settings.SettingNotFoundException | SecurityException e) {}

        try {
            return "" + Settings.Secure.getLong(cr, settingName);
        } catch (Settings.SettingNotFoundException | SecurityException e) {}

        try {
            return Settings.Secure.getString(cr, settingName);
        } catch (SecurityException e) {
            return "SecurityException!";
        }
    }

    private <T> List<String> getStaticStrings(Class<T> cls) {
        return getStaticStringFields(cls).stream()
            .map(it -> {
                try {
                    return (String) it.get(null);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());
    }

    private <T> List<Field> getStaticStringFields(Class<T> cls) {
        return Arrays.stream(cls.getDeclaredFields())
            .filter(it -> Modifier.isStatic(it.getModifiers()) && it.getType().equals(String.class))
            .collect(Collectors.toList());
    }

}
