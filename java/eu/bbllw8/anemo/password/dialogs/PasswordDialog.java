/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.password.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import eu.bbllw8.anemo.lock.LockStore;

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
        this.dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(layout)
                .setCancelable(false)
                .create();
    }

    public void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        activity.finish();
    }

    public void show() {
        dialog.show();
        build();
    }

    protected abstract void build();
}
