package io.salezica.chclock.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import io.salezica.chclock.MainActivity;
import io.salezica.chclock.R;
import io.salezica.chclock.ambient.Ambient;
import io.salezica.chclock.ambient.AmbientProvider;
import io.salezica.chclock.utils.PowerUtils;
import io.salezica.chclock.utils.Prefs;
import io.salezica.chclock.utils.TaggedLog;

public class AmbientControlService extends Service {

    private static String NOTIFICATION_CHANNEL_ID = "Service";
    private static String NOTIFICATION_CHANNEL_NAME = "Charging Clock";
    private static String NOTIFICATION_TITLE = "Charging Clock";

    private static final TaggedLog log = new TaggedLog(AmbientControlService.class);

    private PowerStateReceiver receiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkPowerState();

        if (receiver == null) {
            IntentFilter filter = new IntentFilter();
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED);

            receiver = new PowerStateReceiver();
            registerReceiver(receiver, filter);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            createNotificationChannel();
            startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            startForeground(1, createNotification());
        }

        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification createNotification() {
        Notification.Builder builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        return builder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_MIN
        );

        ch.setDescription("Sticky notification to keep service running");

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(ch);
    }

    private void checkPowerState() {
        log.d("Checking initial power state");
        applyPowerState(this, PowerUtils.isPlugged(this));
    }

    // Also called synchronously from the UI, so a toggle takes effect before re-rendering:
    public static void applyPowerState(Context context, boolean plugged) {
        Ambient ambient = AmbientProvider.getFor(context);

        if (!ambient.isSupported() || !ambient.hasPermissions()) {
            log.d("Ambient is not supported, or we have no permissions");
            return;
        }

        boolean enabled = Prefs.isEnabled(context);
        log.d("Plugged: " + plugged + ", enabled: " + enabled);

        try {
            ambient.setAlwaysOn(plugged && enabled);
        } catch (Exception e) {
            // A rejected settings write must not crash-loop the sticky service:
            log.d("Failed to apply power state: " + e);
        }
    }

    public class PowerStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent powerIntent) {
            // The sticky ACTION_BATTERY_CHANGED intent can lag behind this
            // broadcast, so the action itself is the source of truth here:
            applyPowerState(context, Intent.ACTION_POWER_CONNECTED.equals(powerIntent.getAction()));
        }
    }
}
