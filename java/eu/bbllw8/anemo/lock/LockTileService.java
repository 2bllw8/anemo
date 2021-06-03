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

import androidx.annotation.NonNull;

public final class LockTileService extends TileService {
    private static final String ACTION_ANEMO_UNLOCK = "eu.bbllw8.anemo.action.UNLOCK";

    @NonNull
    private final Intent unlockIntent = new Intent();
    private boolean hasUnlockActivity;

    private LockStore lockStore;
    private int listenerToken = -1;

    @Override
    public IBinder onBind(Intent intent) {
        lockStore = LockStore.getInstance(this);

        unlockIntent.setAction(ACTION_ANEMO_UNLOCK);
        unlockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        hasUnlockActivity = !getPackageManager()
                .queryIntentActivities(unlockIntent, 0)
                .isEmpty();

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
            if (hasUnlockActivity) {
                startActivityAndCollapse(unlockIntent);
            } else {
                lockStore.unlock();
            }
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
                ? R.string.tile_status_locked
                : R.string.tile_status_unlocked));
        tile.setState(isLocked ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.updateTile();
    }
}
