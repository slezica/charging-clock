package io.salezica.chclock.ambient;

import android.content.Context;
import android.content.SharedPreferences;

import io.salezica.chclock.utils.TaggedLog;

public class AmbientMock implements Ambient {

    private static final String PREFS = "AmbientMock";
    private static final String PREF_ALWAYS_ON = "alwaysOn";


    private final TaggedLog log = new TaggedLog(this);

    private SharedPreferences preferences;

    public AmbientMock(Context context) {
        log.d("Initialized with " + context.getClass().getSimpleName());

        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public String getStyle() {
        log.d("getStyle called, returning Always");
        return "Always";
    }

    @Override
    public boolean hasPermissions() {
        log.d("hasPermissions called, returning true");
        return true;
    }

    @Override
    public void requestPermissions() {
        log.d("Request permissions called");
    }

    @Override
    public boolean isSupported() {
        log.d("isSupported called, returning true");
        return true;
    }

    @Override
    public void setAlwaysOn(boolean isAlwaysOn) {
        log.d("Setting alwaysOn to " + isAlwaysOn);
        preferences.edit().putBoolean(PREF_ALWAYS_ON, isAlwaysOn).commit();
    }

    @Override
    public boolean isAlwaysOn() {
        final boolean isAlwaysOn = preferences.getBoolean(PREF_ALWAYS_ON, false);
        log.d("isAlwaysOn called, returning " + isAlwaysOn);
        return isAlwaysOn;
    }
}
