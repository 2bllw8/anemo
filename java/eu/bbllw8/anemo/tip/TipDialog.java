/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.tip;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public final class TipDialog extends Dialog {

    private TipDialog(@NonNull Context context) {
        super(context, R.style.TipTheme);
    }

    public static class Builder {
        @NonNull
        private final Context context;
        @NonNull
        private CharSequence message;
        @DrawableRes
        private int icon;
        private boolean cancelable;
        private boolean dismissOnTouchOutside;

        public Builder(@NonNull Context context) {
            this.context = context;
            this.message = "";
        }

        @NonNull
        public Builder setMessage(@NonNull CharSequence message) {
            this.message = message;
            return this;
        }

        @NonNull
        public Builder setIcon(@DrawableRes int icon) {
            this.icon = icon;
            return this;
        }

        public Builder setProgress() {
            this.icon = 0;
            return this;
        }

        @NonNull
        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        @NonNull
        public Builder setDismissOnTouchOutside(boolean dismissOnTouchOutside) {
            this.dismissOnTouchOutside = dismissOnTouchOutside;
            return this;
        }

        @NonNull
        public TipDialog create() {
            final TipDialog dialog = new TipDialog(context);
            dialog.setCancelable(cancelable);
            dialog.setCanceledOnTouchOutside(dismissOnTouchOutside);
            dialog.setContentView(R.layout.tip_ui);
            final ImageView iconView = dialog.findViewById(android.R.id.icon);
            final ProgressBar progressBar = dialog.findViewById(android.R.id.progress);
            if (icon == 0) {
                iconView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                iconView.setImageResource(icon);
                iconView.setVisibility(View.VISIBLE);
            }
            final TextView messageView = dialog.findViewById(android.R.id.message);
            messageView.setText(message);
            return dialog;
        }
    }
}
