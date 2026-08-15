package io.slezica.ambientcontrol;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.List;

import io.slezica.ambientcontrol.ambient.Ambient;
import io.slezica.ambientcontrol.ambient.AmbientProvider;
import io.slezica.ambientcontrol.ambient.StatusItem;
import io.slezica.ambientcontrol.inspection.SettingsReader;
import io.slezica.ambientcontrol.services.AmbientControlService;
import io.slezica.ambientcontrol.utils.PowerUtils;
import io.slezica.ambientcontrol.utils.Prefs;

public class MainActivity extends AppCompatActivity {

    private Ambient ambient;
    private LinearLayout statusContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ambient = AmbientProvider.getFor(this);
        statusContainer = (LinearLayout) findViewById(R.id.status_container);

        SwitchCompat enabledSwitch = findViewById(R.id.enabled_switch);
        enabledSwitch.setChecked(Prefs.isEnabled(this));
        enabledSwitch.setOnCheckedChangeListener((view, isChecked) -> {
            Prefs.setEnabled(this, isChecked);
            AmbientControlService.applyPowerState(this, PowerUtils.isPlugged(this));
            renderStatus();
        });

        startControlService();

        // Only for development, as Android changes how settings are managed:
        // startWatchingSettingChanges();
    }

    private void startWatchingSettingChanges() {
        new SettingsReader(getContentResolver()).startWatchingChanges((name, oldValue, newValue) ->
            Log.d("SettingsReader", "Settings: " + name + " changed from '" + oldValue + "' to '" + newValue + "'")
        );
    }

    private void startControlService() {
        Intent intent = new Intent(this, AmbientControlService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderStatus();
    }

    private void renderStatus() {
        List<StatusItem> items = new ArrayList<>();

        items.add(PowerUtils.getChargerStatus(this));
        items.addAll(ambient.getStatus());
        items.add(PowerUtils.getBatteryOptimizationStatus(this));

        statusContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (StatusItem item : items) {
            View row = inflater.inflate(R.layout.row_status, statusContainer, false);

            TextView label = row.findViewById(R.id.status_label);
            TextView value = row.findViewById(R.id.status_value);
            TextView hint = row.findViewById(R.id.status_hint);
            Button fix = row.findViewById(R.id.status_fix);

            label.setText(item.label);
            value.setText(item.value);
            value.setTextColor(getColor(getToneColor(item.tone)));

            if (item.hint != null) {
                hint.setText(item.hint);
            } else {
                hint.setVisibility(View.GONE);
            }

            if (item.fix != null) {
                fix.setOnClickListener(v -> {
                    item.fix.run();
                    renderStatus();
                });
            } else {
                fix.setVisibility(View.GONE);
            }

            statusContainer.addView(row);
        }
    }

    private int getToneColor(StatusItem.Tone tone) {
        switch (tone) {
            case OK:   return R.color.status_ok;
            case WARN: return R.color.status_warn;
            default:   return R.color.text_primary;
        }
    }
}
