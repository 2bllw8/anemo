/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.password;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import exe.bbllw8.anemo.documents.lock.LockStore;
import exe.bbllw8.anemo.documents.password.dialogs.ChangePasswordDialog;
import exe.bbllw8.anemo.documents.password.dialogs.InputPasswordDialog;
import exe.bbllw8.anemo.documents.password.dialogs.ResetPasswordDialog;
import exe.bbllw8.anemo.documents.password.dialogs.SetPasswordDialog;

public final class PasswordActivity extends Activity {
    public static final String OPEN_AFTER_UNLOCK = "open_after_unlock";

    private LockStore lockStore;
    private boolean openAfterUnlock;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lockStore = LockStore.getInstance(this);
        openAfterUnlock = getIntent().getBooleanExtra(OPEN_AFTER_UNLOCK, false);

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
        new InputPasswordDialog(this, lockStore, openAfterUnlock,
                this::showChangePasswordDialog).show();
    }

    private void showResetPasswordDialog() {
        new ResetPasswordDialog(this, lockStore).show();
    }

    private void showSetPasswordDialog() {
        new SetPasswordDialog(this, lockStore).show();
    }

}
