package io.salezica.chclock.ui;

import java.util.ArrayList;
import java.util.List;

import io.salezica.chclock.ambient.StatusItem;

/**
 * Assembles the dashboard rows from a MainPresenter. Single code path for the
 * real and mock presenters, so screenshots exercise the actual UI logic.
 */
public class StatusItems {

    public static List<StatusItem> build(MainPresenter presenter) {
        List<StatusItem> items = new ArrayList<>();

        items.add(StatusItem.neutral(
            "Charger", presenter.isPlugged() ? "Connected" : "Disconnected"
        ));

        items.addAll(buildAmbientItems(presenter));

        items.add(buildBatteryOptimizationItem(presenter));

        return items;
    }

    private static List<StatusItem> buildAmbientItems(MainPresenter presenter) {
        List<StatusItem> items = new ArrayList<>();

        if (!presenter.isSupported()) {
            items.add(StatusItem.warn(
                "Always On Display", "Not detected",
                "This device does not expose Samsung AOD settings.",
                null
            ));
            return items;
        }

        if (presenter.hasPermission()) {
            items.add(StatusItem.ok("Permission", "Granted"));
        } else {
            items.add(StatusItem.warn(
                "Permission", "Missing",
                "Charging Clock needs \"Modify system settings\" to control AOD.",
                presenter::requestPermission
            ));
            return items;
        }

        boolean alwaysOn = presenter.isAlwaysOn();
        boolean expected = presenter.isPlugged() && presenter.isEnabled();

        if (alwaysOn == expected) {
            items.add(StatusItem.ok("Always On Display", alwaysOn ? "On" : "Off"));
        } else {
            items.add(StatusItem.warn(
                "Always On Display", alwaysOn ? "On" : "Off",
                "Doesn't match the charger state. The background service may not be running.",
                null
            ));
        }

        String style = presenter.getAodStyle();
        if ("Always".equals(style)) {
            items.add(StatusItem.ok("AOD style", style));
        } else {
            items.add(StatusItem.warn(
                "AOD style", style,
                "Set Always On Display to \"Always\" in Samsung settings, so it stays visible while charging.",
                null
            ));
        }

        return items;
    }

    private static StatusItem buildBatteryOptimizationItem(MainPresenter presenter) {
        if (presenter.isIgnoringBatteryOptimizations()) {
            return StatusItem.ok("Battery optimization", "Unrestricted");
        }

        return StatusItem.warn(
            "Battery optimization", "Restricted",
            "The system may kill the background service, so the charger goes unnoticed."
                + " Tap Fix, find Charging Clock in the list, and choose \"Don't optimize\".",
            presenter::openBatteryOptimizationSettings
        );
    }
}
