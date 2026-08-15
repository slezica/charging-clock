package io.salezica.chclock.ambient;

public interface Ambient {

    boolean hasPermissions();

    void requestPermissions();

    boolean isSupported();

    void setAlwaysOn(boolean alwaysOn);

    boolean isAlwaysOn();

    AodStyle getStyle();

    boolean canOpenStyleSettings();

    void openStyleSettings();
}
