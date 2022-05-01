/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.config;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.function.Consumer;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.config.password.ChangePasswordDialog;
import exe.bbllw8.anemo.config.password.ResetPasswordDialog;
import exe.bbllw8.anemo.config.password.SetPasswordDialog;
import exe.bbllw8.anemo.lock.LockStore;
import exe.bbllw8.anemo.lock.UnlockActivity;
import exe.bbllw8.anemo.shell.AnemoShell;

public final class ConfigurationActivity extends Activity {

    private TextView passwordSetView;
    private TextView passwordResetView;
    private TextView changeLockView;

    private LockStore lockStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lockStore = LockStore.getInstance(getApplicationContext());
        lockStore.addListener(onLockChanged);

        setContentView(R.layout.configuration);

        passwordSetView = findViewById(R.id.configuration_password_set);
        passwordResetView = findViewById(R.id.configuration_password_reset);

        final Switch shortcutSwitch = findViewById(R.id.configuration_show_shortcut);
        shortcutSwitch.setChecked(AnemoShell.isEnabled(getApplication()));
        shortcutSwitch.setOnCheckedChangeListener(
                (v, isChecked) -> AnemoShell.setEnabled(getApplication(), isChecked));

        setupPasswordViews();

        changeLockView = findViewById(R.id.configuration_lock);
        changeLockView.setText(lockStore.isLocked()
                ? R.string.configuration_storage_unlock
                : R.string.configuration_storage_lock);
        changeLockView.setOnClickListener($ -> {
            if (lockStore.isLocked()) {
                startActivity(new Intent(this, UnlockActivity.class));
            } else {
                lockStore.lock();
            }
        });

        final Switch autoLockSwitch = findViewById(R.id.configuration_auto_lock);
        autoLockSwitch.setChecked(lockStore.isAutoLockEnabled());
        autoLockSwitch.setOnCheckedChangeListener(
                (v, isChecked) -> lockStore.setAutoLockEnabled(isChecked));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lockStore.removeListener(onLockChanged);
    }

    private void setupPasswordViews() {
        if (lockStore.hasPassword()) {
            passwordSetView.setText(R.string.configuration_password_change);
            passwordSetView.setOnClickListener(
                    $ -> new ChangePasswordDialog(this, lockStore, this::setupPasswordViews)
                            .show());
            passwordResetView.setEnabled(true);
        } else {
            passwordSetView.setText(R.string.configuration_password_set);
            passwordSetView.setOnClickListener(
                    $ -> new SetPasswordDialog(this, lockStore, this::setupPasswordViews).show());
            passwordResetView.setEnabled(false);
        }
        passwordSetView.setEnabled(!lockStore.isLocked());
        passwordResetView.setOnClickListener(
                $ -> new ResetPasswordDialog(this, lockStore, this::setupPasswordViews).show());
    }

    private final Consumer<Boolean> onLockChanged = isLocked -> {
        passwordSetView.setEnabled(!isLocked);
        changeLockView.setText(isLocked
                ? R.string.configuration_storage_unlock
                : R.string.configuration_storage_lock);
    };
}
