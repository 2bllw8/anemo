/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.config;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.config.password.ChangePasswordDialog;
import exe.bbllw8.anemo.config.password.ResetPasswordDialog;
import exe.bbllw8.anemo.config.password.SetPasswordDialog;
import exe.bbllw8.anemo.lock.LockStore;
import exe.bbllw8.anemo.shell.AnemoShell;

public final class ConfigurationActivity extends Activity {

    private TextView passwordSetView;
    private TextView passwordResetView;

    private LockStore lockStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lockStore = LockStore.getInstance(getApplicationContext());

        setContentView(R.layout.configuration);

        passwordSetView = findViewById(R.id.configuration_password_set);
        passwordResetView = findViewById(R.id.configuration_password_reset);

        final Switch shortcutSwitch = findViewById(R.id.configuration_show_shortcut);
        shortcutSwitch.setChecked(AnemoShell.isEnabled(getApplication()));
        shortcutSwitch.setOnCheckedChangeListener((v, isChecked) ->
                AnemoShell.setEnabled(getApplication(), isChecked));

        setupPasswordViews();

        final Switch lockSwitch = findViewById(R.id.configuration_lock);
        lockSwitch.setChecked(!lockStore.isLocked());
        lockSwitch.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                lockStore.unlock();
            } else {
                lockStore.lock();
            }
        });

        final Switch autoLockSwitch = findViewById(R.id.configuration_auto_lock);
        autoLockSwitch.setChecked(lockStore.isAutoLockEnabled());
        autoLockSwitch.setOnCheckedChangeListener((v, isChecked) ->
                lockStore.setAutoLockEnabled(isChecked));
    }

    private void setupPasswordViews() {
        if (lockStore.hasPassword()) {
            passwordSetView.setText(R.string.configuration_password_change);
            passwordSetView.setOnClickListener(v -> new ChangePasswordDialog(this,
                    lockStore, this::setupPasswordViews).show());
            passwordResetView.setEnabled(true);
        } else {
            passwordSetView.setText(R.string.configuration_password_set);
            passwordSetView.setOnClickListener(v -> new SetPasswordDialog(this,
                    lockStore, this::setupPasswordViews).show());
            passwordResetView.setEnabled(false);
        }
        passwordResetView.setOnClickListener(v -> new ResetPasswordDialog(this,
                lockStore, this::setupPasswordViews).show());
    }
}
