package io.salezica.chclock.ui;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import io.salezica.chclock.R;
import io.salezica.chclock.ambient.AodStyle;
import io.salezica.chclock.ambient.StatusItem;

/**
 * Assembles the dashboard rows from a MainPresenter. Single code path for the
 * real and mock presenters, so screenshots exercise the actual UI logic.
 */
public class StatusItems {

    private final Context context;
    private final MainPresenter presenter;

    public StatusItems(Context context, MainPresenter presenter) {
        this.context = context;
        this.presenter = presenter;
    }

    public List<StatusItem> build() {
        List<StatusItem> items = new ArrayList<>();

        items.add(StatusItem.neutral(
            str(R.string.label_charger),
            str(presenter.isPlugged() ? R.string.value_connected : R.string.value_disconnected)
        ));

        items.addAll(buildAmbientItems());

        items.add(buildBatteryOptimizationItem());

        return items;
    }

    private List<StatusItem> buildAmbientItems() {
        List<StatusItem> items = new ArrayList<>();

        if (!presenter.isSupported()) {
            items.add(StatusItem.warn(
                str(R.string.label_aod), str(R.string.value_not_detected),
                str(R.string.hint_not_supported),
                null
            ));
            return items;
        }

        // No early return on missing permission: settings reads are ungated,
        // so the AOD and style rows below stay accurate either way.
        if (presenter.hasPermission()) {
            items.add(StatusItem.ok(str(R.string.label_permission), str(R.string.value_granted)));
        } else {
            items.add(StatusItem.warn(
                str(R.string.label_permission), str(R.string.value_missing),
                str(R.string.hint_permission),
                presenter::requestPermission
            ));
        }

        boolean alwaysOn = presenter.isAlwaysOn();
        boolean expected = presenter.isPlugged() && presenter.isEnabled();
        String aodValue = str(alwaysOn ? R.string.value_on : R.string.value_off);

        if (alwaysOn == expected) {
            items.add(StatusItem.ok(str(R.string.label_aod), aodValue));
        } else {
            items.add(StatusItem.warn(
                str(R.string.label_aod), aodValue,
                str(R.string.hint_aod_mismatch),
                null
            ));
        }

        AodStyle style = presenter.getAodStyle();
        if (style == AodStyle.ALWAYS) {
            items.add(StatusItem.ok(str(R.string.label_aod_style), str(style.labelRes)));
        } else if (presenter.canFixAodStyle()) {
            items.add(StatusItem.warn(
                str(R.string.label_aod_style), str(style.labelRes),
                str(R.string.hint_style_fixable),
                presenter::fixAodStyle
            ));
        } else {
            // No settings screen found to open; point at Samsung settings instead.
            items.add(StatusItem.warn(
                str(R.string.label_aod_style), str(style.labelRes),
                str(R.string.hint_style_manual),
                null
            ));
        }

        return items;
    }

    private StatusItem buildBatteryOptimizationItem() {
        if (presenter.isIgnoringBatteryOptimizations()) {
            return StatusItem.ok(str(R.string.label_battery), str(R.string.value_unrestricted));
        }

        return StatusItem.warn(
            str(R.string.label_battery), str(R.string.value_restricted),
            str(R.string.hint_battery),
            presenter::openBatteryOptimizationSettings
        );
    }

    private String str(int resId) {
        return context.getString(resId);
    }
}
