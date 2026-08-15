package io.salezica.chclock.ui;

import android.content.Context;

import io.salezica.chclock.ambient.Ambient;
import io.salezica.chclock.ambient.AmbientProvider;
import io.salezica.chclock.services.AmbientControlService;
import io.salezica.chclock.utils.PowerUtils;
import io.salezica.chclock.utils.Prefs;

public class RealMainPresenter implements MainPresenter {

    private final Context context;
    private final Ambient ambient;

    public RealMainPresenter(Context context) {
        this.context = context;
        this.ambient = AmbientProvider.getFor(context);
    }

    @Override
    public boolean isEnabled() {
        return Prefs.isEnabled(context);
    }

    @Override
    public void setEnabled(boolean enabled) {
        Prefs.setEnabled(context, enabled);
        AmbientControlService.applyPowerState(context, isPlugged());
    }

    @Override
    public boolean isPlugged() {
        return PowerUtils.isPlugged(context);
    }

    @Override
    public boolean isSupported() {
        return ambient.isSupported();
    }

    @Override
    public boolean hasPermission() {
        return ambient.hasPermissions();
    }

    @Override
    public void requestPermission() {
        ambient.requestPermissions();
    }

    @Override
    public boolean isAlwaysOn() {
        return ambient.isAlwaysOn();
    }

    @Override
    public String getAodStyle() {
        return ambient.getStyle();
    }

    @Override
    public boolean isIgnoringBatteryOptimizations() {
        return PowerUtils.isIgnoringBatteryOptimizations(context);
    }

    @Override
    public void openBatteryOptimizationSettings() {
        PowerUtils.openBatteryOptimizationSettings(context);
    }
}
