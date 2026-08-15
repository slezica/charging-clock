package io.salezica.chclock.services;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import io.salezica.chclock.ambient.Ambient;
import io.salezica.chclock.ambient.AmbientProvider;
import io.salezica.chclock.utils.PowerUtils;

public class AmbientTileService extends TileService {

    private Ambient ambient;

    @Override
    public void onCreate() {
        super.onCreate();
        ambient = AmbientProvider.getFor(this);
    }

    @Override
    public void onStartListening() {
        updateTile();
    }


    @Override
    public void onClick() {
        super.onClick();

        if (isAvailable()) {
            ambient.setAlwaysOn(!ambient.isAlwaysOn());
        }

        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();

        if (! isAvailable()) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            return;
        }

        if (ambient.isAlwaysOn()) {
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
        }

        tile.updateTile();
    }

    private boolean isAvailable() {
        return (ambient.isSupported() && ambient.hasPermissions());
    }
}
