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

public final class SetPasswordDialog extends PasswordDialog {

    public SetPasswordDialog(@NonNull Activity activity,
                             @NonNull LockStore lockStore) {
        super(activity, lockStore, R.string.password_set_title, R.layout.password_first_set);
    }

    @Override
    protected void build() {
        final EditText passwordField = dialog.findViewById(R.id.passwordFieldView);
        final EditText repeatField = dialog.findViewById(R.id.repeatFieldView);
        final Button positiveBtn = dialog.findViewById(R.id.setBtnPositive);
        final Button negativeBtn = dialog.findViewById(R.id.setBtnNegative);

        final TextListener validator = buildValidator(passwordField, repeatField, positiveBtn);
        passwordField.addTextChangedListener(validator);
        repeatField.addTextChangedListener(validator);

        positiveBtn.setEnabled(false);
        positiveBtn.setOnClickListener(v -> {
            final String passwordValue = passwordField.getText().toString();
            if (lockStore.setPassword(passwordValue)) {
                lockStore.unlock();
                dismiss();
            }
        });

        negativeBtn.setOnClickListener(v -> dismiss());
    }

    @NonNull
    private TextListener buildValidator(@NonNull EditText passwordField,
                                        @NonNull EditText repeatField,
                                        @NonNull Button positiveBtn) {
        return text -> {
            final String passwordValue = passwordField.getText().toString();
            final String repeatValue = repeatField.getText().toString();

            if (passwordValue.length() < MIN_PASSWORD_LENGTH) {
                positiveBtn.setEnabled(false);
                passwordField.setError(res.getString(
                        R.string.password_error_length, MIN_PASSWORD_LENGTH));
                repeatField.setError(null);
            } else if (!passwordValue.equals(repeatValue)) {
                positiveBtn.setEnabled(false);
                passwordField.setError(null);
                repeatField.setError(res.getString(
                        R.string.password_error_mismatch));
            } else {
                positiveBtn.setEnabled(true);
                passwordField.setError(null);
                repeatField.setError(null);
            }
        };
    }
}
