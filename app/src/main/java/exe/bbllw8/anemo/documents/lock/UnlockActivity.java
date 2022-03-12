/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.lock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.Optional;

import exe.bbllw8.anemo.documents.config.ConfigurationActivity;
import exe.bbllw8.anemo.documents.config.password.InputPasswordDialog;
import exe.bbllw8.anemo.documents.lock.LockStore;
import exe.bbllw8.anemo.shell.ShortcutActivity;

public final class UnlockActivity extends Activity {
    public static final String OPEN_AFTER_UNLOCK = "open_after_unlock";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Runnable onUnlocked;
        if (getIntent().getBooleanExtra(OPEN_AFTER_UNLOCK, false)) {
            onUnlocked = () -> {
                startActivity(new Intent(this, ShortcutActivity.class));
                finish();
            };
        } else {
            onUnlocked = this::finish;
        }

        final LockStore lockStore = LockStore.getInstance(this);
        if (lockStore.hasPassword()) {
            final Runnable openConfig = () -> {
                startActivity(new Intent(this, ConfigurationActivity.class));
                finish();
            };
            new InputPasswordDialog(this, lockStore, onUnlocked, openConfig)
                    .show();
        } else {
            lockStore.unlock();
            onUnlocked.run();
        }
    }
}
