/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.aeolus.config.password;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import exe.bbllw8.aeolus.R;
import exe.bbllw8.aeolus.lock.LockStore;

public final class ChangePasswordDialog extends PasswordDialog {

    public ChangePasswordDialog(Activity activity, LockStore lockStore, Runnable onSuccess) {
        super(activity, lockStore, onSuccess, R.string.password_change_title,
                R.layout.password_change);
    }

    @Override
    protected void build() {
        final EditText currentField = dialog.findViewById(R.id.currentFieldView);
        final EditText passwordField = dialog.findViewById(R.id.passwordFieldView);
        final EditText repeatField = dialog.findViewById(R.id.repeatFieldView);
        final Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        final Button neutralBtn = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);

        final TextListener validator = buildTextListener(passwordField, repeatField, positiveBtn);
        passwordField.addTextChangedListener(validator);
        repeatField.addTextChangedListener(validator);

        positiveBtn.setVisibility(View.VISIBLE);
        positiveBtn.setText(R.string.password_change_action);
        positiveBtn.setEnabled(false);
        positiveBtn.setOnClickListener(v -> {
            final String currentPassword = currentField.getText().toString();
            final String newPassword = passwordField.getText().toString();

            if (lockStore.passwordMatch(currentPassword)) {
                if (lockStore.setPassword(newPassword)) {
                    dismiss();
                    lockStore.unlock();
                    onSuccess.run();
                }
            } else {
                currentField.setError(res.getString(R.string.password_error_wrong), getErrorIcon());
            }
        });

        neutralBtn.setVisibility(View.VISIBLE);
        neutralBtn.setText(R.string.password_change_remove);
        neutralBtn.setOnClickListener(v -> {
            lockStore.removePassword();
            onSuccess.run();
            dismiss();
        });
    }

    private TextListener buildTextListener(EditText passwordField, EditText repeatField,
            Button positiveBtn) {
        return text -> {
            final String passwordValue = passwordField.getText().toString();
            final String repeatValue = repeatField.getText().toString();

            if (passwordValue.length() < MIN_PASSWORD_LENGTH) {
                positiveBtn.setEnabled(false);
                passwordField.setError(
                        res.getString(R.string.password_error_length, MIN_PASSWORD_LENGTH),
                        getErrorIcon());
                repeatField.setError(null);
            } else if (!passwordValue.equals(repeatValue)) {
                positiveBtn.setEnabled(false);
                passwordField.setError(null);
                repeatField.setError(res.getString(R.string.password_error_mismatch),
                        getErrorIcon());
            } else {
                positiveBtn.setEnabled(true);
                passwordField.setError(null);
                repeatField.setError(null);
            }
        };
    }
}
