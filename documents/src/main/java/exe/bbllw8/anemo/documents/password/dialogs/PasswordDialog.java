/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.password.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import exe.bbllw8.anemo.documents.lock.LockStore;

public abstract class PasswordDialog {
    protected static final int MIN_PASSWORD_LENGTH = 4;

    @NonNull
    private final Activity activity;
    @NonNull
    protected final Resources res;
    @NonNull
    protected final LockStore lockStore;
    @NonNull
    protected final AlertDialog dialog;

    public PasswordDialog(@NonNull Activity activity,
                          @NonNull LockStore lockStore,
                          @StringRes int title,
                          @LayoutRes int layout) {
        this.activity = activity;
        this.res = activity.getResources();
        this.lockStore = lockStore;
        this.dialog = new AlertDialog.Builder(activity, exe.bbllw8.anemo.shell.R.style.DialogTheme)
                .setTitle(title)
                .setView(layout)
                .setCancelable(false)
                .create();
    }

    public final void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        activity.finish();
    }

    public final void show() {
        dialog.show();
        build();
    }

    protected abstract void build();
}
