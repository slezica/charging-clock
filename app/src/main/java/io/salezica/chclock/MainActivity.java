package io.salezica.chclock;

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

import java.util.List;

import io.salezica.chclock.ambient.StatusItem;
import io.salezica.chclock.inspection.SettingsReader;
import io.salezica.chclock.services.AmbientControlService;
import io.salezica.chclock.ui.MainPresenter;
import io.salezica.chclock.ui.MockMainPresenter;
import io.salezica.chclock.ui.RealMainPresenter;
import io.salezica.chclock.ui.StatusItems;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_PRESET = "preset";

    private MainPresenter presenter;
    private LinearLayout statusContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        presenter = createPresenter();
        statusContainer = (LinearLayout) findViewById(R.id.status_container);

        SwitchCompat enabledSwitch = findViewById(R.id.enabled_switch);
        enabledSwitch.setChecked(presenter.isEnabled());
        enabledSwitch.setOnCheckedChangeListener((view, isChecked) -> {
            presenter.setEnabled(isChecked);
            renderStatus();
        });

        if (!isMocked()) {
            startControlService();
        }

        // Only for development, as Android changes how settings are managed:
        // startWatchingSettingChanges();
    }

    private MainPresenter createPresenter() {
        if (isMocked()) {
            return MockMainPresenter.fromPreset(getIntent().getStringExtra(EXTRA_PRESET));
        }

        return new RealMainPresenter(this);
    }

    // Debug-only escape hatch for screenshots and UI work; see bin/chclock capture.
    private boolean isMocked() {
        return BuildConfig.DEBUG && getIntent().hasExtra(EXTRA_PRESET);
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
        List<StatusItem> items = StatusItems.build(presenter);

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
