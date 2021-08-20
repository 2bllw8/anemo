/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.lock;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.NonNull;

public final class LockTileService extends TileService {
    private static final String ACTION_ANEMO_UNLOCK = "exe.bbllw8.anemo.action.UNLOCK";

    @NonNull
    private final Intent unlockIntent = new Intent()
            .setAction(ACTION_ANEMO_UNLOCK)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    private boolean hasUnlockActivity;

    private LockStore lockStore;
    private int listenerToken = LockStore.NULL_LISTENER_ID;

    @Override
    public IBinder onBind(Intent intent) {
        lockStore = LockStore.getInstance(this);

        hasUnlockActivity = !getPackageManager()
                .queryIntentActivities(unlockIntent, 0)
                .isEmpty();

        return super.onBind(intent);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        initializeTile();
        updateTile(lockStore.isLocked());
        listenerToken = lockStore.addListener(this::updateTile);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();

        if (listenerToken != LockStore.NULL_LISTENER_ID) {
            lockStore.removeListener(listenerToken);
            listenerToken = LockStore.NULL_LISTENER_ID;
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

    private void initializeTile() {
        final Tile tile = getQsTile();
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_key_tile));
    }

    private void updateTile(boolean isLocked) {
        final Tile tile = getQsTile();
        if (isLocked) {
            tile.setLabel(getString(R.string.tile_unlock));
            tile.setStateDescription(getString(R.string.tile_status_locked));
            tile.setState(Tile.STATE_INACTIVE);
        } else {
            tile.setLabel(getString(R.string.tile_lock));
            tile.setStateDescription(getString(R.string.tile_status_unlocked));
            tile.setState(Tile.STATE_ACTIVE);
        }
        tile.updateTile();
    }
}
