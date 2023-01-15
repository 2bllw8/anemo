/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.config.password;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.lock.LockStore;

public abstract class PasswordDialog {
    protected static final int MIN_PASSWORD_LENGTH = 4;

    protected final Resources res;
    protected final LockStore lockStore;
    protected final Runnable onSuccess;
    protected final AlertDialog dialog;

    public PasswordDialog(Activity activity, LockStore lockStore, Runnable onSuccess,
            @StringRes int title, @LayoutRes int layout) {
        this.res = activity.getResources();
        this.lockStore = lockStore;
        this.onSuccess = onSuccess;
        this.dialog = new AlertDialog.Builder(activity, R.style.DialogTheme).setTitle(title)
                .setView(layout)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, (d, which) -> dismiss())
                .create();
    }

    public void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public final void show() {
        dialog.show();
        build();
    }

    protected Drawable getErrorIcon() {
        final Drawable drawable = dialog.getContext().getDrawable(R.drawable.ic_error);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    protected abstract void build();
}
