/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.password;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import eu.bbllw8.anemo.documents.lock.LockStore;
import eu.bbllw8.anemo.documents.password.dialogs.ChangePasswordDialog;
import eu.bbllw8.anemo.documents.password.dialogs.InputPasswordDialog;
import eu.bbllw8.anemo.documents.password.dialogs.ResetPasswordDialog;
import eu.bbllw8.anemo.documents.password.dialogs.SetPasswordDialog;

public final class PasswordActivity extends Activity {

    private LockStore lockStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lockStore = LockStore.getInstance(this);

        if (lockStore.hasPassword()) {
            showInputPasswordDialog();
        } else {
            showSetPasswordDialog();
        }
    }

    private void showChangePasswordDialog() {
        new ChangePasswordDialog(this, lockStore, this::showResetPasswordDialog).show();
    }

    private void showInputPasswordDialog() {
        new InputPasswordDialog(this, lockStore, this::showChangePasswordDialog).show();
    }

    private void showResetPasswordDialog() {
        new ResetPasswordDialog(this, lockStore).show();
    }

    private void showSetPasswordDialog() {
        new SetPasswordDialog(this, lockStore).show();
    }

}
