package io.salezica.chclock.ambient;

import io.salezica.chclock.R;

/**
 * Samsung AOD display style. Only ALWAYS keeps the display on while charging;
 * every other style is warned about in the dashboard.
 */
public enum AodStyle {

    ALWAYS(R.string.style_always),
    TAP_TO_SHOW(R.string.style_tap_to_show),
    AUTO(R.string.style_auto),
    NEW_NOTIFICATIONS(R.string.style_new_notifications),
    SCHEDULED(R.string.style_scheduled);

    public final int labelRes;

    AodStyle(int labelRes) {
        this.labelRes = labelRes;
    }
}
