/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.lock;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public final class LockTileService extends TileService {

    private LockStore lockStore;
    private int listenerToken = -1;

    @Override
    public IBinder onBind(Intent intent) {
        lockStore = LockStore.getInstance(this);

        return super.onBind(intent);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        updateTile(lockStore.isLocked());
        listenerToken = lockStore.addListener(this::updateTile);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();

        if (listenerToken != -1) {
            lockStore.removeListener(listenerToken);
            listenerToken = -1;
        }
    }

    @Override
    public void onClick() {
        super.onClick();

        if (lockStore.isLocked()) {
            lockStore.unlock();
        } else {
            lockStore.lock();
        }
    }

    private void updateTile(boolean isLocked) {
        final Tile tile = getQsTile();
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_key_tile));
        tile.setLabel(getString(isLocked
                ? R.string.tile_unlock
                : R.string.tile_lock));
        tile.setSubtitle(getString(isLocked
                ? R.string.tiles_status_locked
                : R.string.tiles_status_unlocked));
        tile.setState(isLocked ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.updateTile();
    }
}
