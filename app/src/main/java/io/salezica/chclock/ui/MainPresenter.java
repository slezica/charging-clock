package io.salezica.chclock.ui;

/**
 * Everything MainActivity needs to render and act, as typed properties and
 * actions. RealMainPresenter backs it with the actual system; MockMainPresenter
 * fakes it in memory for screenshots and UI work on unsupported devices.
 */
public interface MainPresenter {

    boolean isEnabled();
    void setEnabled(boolean enabled);

    boolean isPlugged();

    boolean isSupported();

    boolean hasPermission();
    void requestPermission();

    boolean isAlwaysOn();

    String getAodStyle();

    boolean canFixAodStyle();
    void fixAodStyle();

    boolean isIgnoringBatteryOptimizations();
    void openBatteryOptimizationSettings();
}
