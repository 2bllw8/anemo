/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.password.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.documents.lock.LockStore;
import exe.bbllw8.anemo.documents.password.TextListener;

public final class InputPasswordDialog extends PasswordDialog {

    @NonNull
    private final Runnable changePassword;

    public InputPasswordDialog(@NonNull Activity activity,
                               @NonNull LockStore lockStore,
                               @NonNull Runnable changePassword) {
        super(activity, lockStore, R.string.tile_unlock, R.layout.password_input);
        this.changePassword = changePassword;
    }

    @Override
    protected void build() {
        final EditText passwordField = dialog.findViewById(R.id.passwordFieldView);
        final Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        final Button neutralBtn = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);

        final TextListener validator = buildInputValidator(passwordField, positiveBtn);
        passwordField.addTextChangedListener(validator);

        positiveBtn.setVisibility(View.VISIBLE);
        positiveBtn.setText(R.string.password_input_action);
        positiveBtn.setEnabled(false);
        positiveBtn.setOnClickListener(v -> {
            final String value = passwordField.getText().toString();
            if (lockStore.passwordMatch(value)) {
                lockStore.unlock();
                dismiss();
            } else {
                passwordField.setError(res.getString(R.string.password_error_wrong));
            }
        });

        neutralBtn.setVisibility(View.VISIBLE);
        neutralBtn.setText(R.string.password_input_change);
        neutralBtn.setOnClickListener(v -> {
            dialog.dismiss();
            changePassword.run();
        });
    }

    @NonNull
    private TextListener buildInputValidator(@NonNull EditText passwordField,
                                             @NonNull Button positiveBtn) {
        return text -> {
            final String value = passwordField.getText().toString();
            positiveBtn.setEnabled(value.length() >= MIN_PASSWORD_LENGTH);
        };
    }
}
