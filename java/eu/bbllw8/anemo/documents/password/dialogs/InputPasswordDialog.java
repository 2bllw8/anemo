/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.password.dialogs;

import android.app.Activity;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

import eu.bbllw8.anemo.documents.lock.LockStore;
import eu.bbllw8.anemo.documents.password.R;
import eu.bbllw8.anemo.documents.password.TextListener;

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
        final Button positiveBtn = dialog.findViewById(R.id.inputBtnPositive);
        final Button neutralBtn = dialog.findViewById(R.id.inputBtnNeutral);
        final Button negativeBtn = dialog.findViewById(R.id.inputBtnNegative);

        final TextListener validator = buildInputValidator(passwordField, positiveBtn);
        passwordField.addTextChangedListener(validator);

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
        neutralBtn.setOnClickListener(v -> {
            dialog.dismiss();
            changePassword.run();
        });
        negativeBtn.setOnClickListener(v -> dismiss());
    }

    @NonNull
    private TextListener buildInputValidator(@NonNull EditText passwordField,
                                             @NonNull Button positiveBtn) {
        return new TextListener() {
            @Override
            protected void onTextChanged(@NonNull String text) {
                final String value = passwordField.getText().toString();
                positiveBtn.setEnabled(value.length() >= MIN_PASSWORD_LENGTH);
            }
        };
    }
}
