package io.slezica.ambientcontrol.ambient;

import android.content.Context;

public class AmbientProvider {

    public static final boolean DEBUG = false;

    public static Ambient getFor(Context context) {
        if (DEBUG) {
            return new AmbientMock(context);
        }

        // Play Store version: One UI only. On other devices the app reports
        // "Not detected" and does nothing.
        return new AmbientSamsung(context);
    }

}
