/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.lock;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import exe.bbllw8.anemo.documents.R;
import exe.bbllw8.anemo.documents.password.PasswordActivity;

public final class LockTileService extends TileService {
    private boolean hasUnlockActivity;
    private LockStore lockStore;
    private int listenerToken = LockStore.NULL_LISTENER_ID;

    @Override
    public IBinder onBind(Intent intent) {
        lockStore = LockStore.getInstance(this);

        final int status = getPackageManager().getComponentEnabledSetting(
                new ComponentName(this, PasswordActivity.class));
        hasUnlockActivity = PackageManager.COMPONENT_ENABLED_STATE_DISABLED != status;

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
                final Intent intent = new Intent(this, PasswordActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityAndCollapse(intent);
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
            tile.setState(Tile.STATE_INACTIVE);
            if (Build.VERSION.SDK_INT >= 30) {
                tile.setStateDescription(getString(R.string.tile_status_locked));
            }
        } else {
            tile.setLabel(getString(R.string.tile_lock));
            tile.setState(Tile.STATE_ACTIVE);
            if (Build.VERSION.SDK_INT >= 30) {
                tile.setStateDescription(getString(R.string.tile_status_unlocked));
            }
        }
        tile.updateTile();
    }
}
