package io.salezica.chclock.utils;

import android.content.Context;

public class Prefs {

    private static final String FILE = "settings";
    private static final String KEY_ENABLED = "enabled";

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
