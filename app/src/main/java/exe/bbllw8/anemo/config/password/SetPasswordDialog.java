/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.config.password;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.lock.LockStore;

public final class SetPasswordDialog extends PasswordDialog {

    public SetPasswordDialog(Activity activity, LockStore lockStore, Runnable onSuccess) {
        super(activity, lockStore, onSuccess, R.string.password_set_title,
                R.layout.password_first_set);
    }

    @Override
    protected void build() {
        final EditText passwordField = dialog.findViewById(R.id.passwordFieldView);
        final EditText repeatField = dialog.findViewById(R.id.repeatFieldView);
        final Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

        final TextListener validator = buildValidator(passwordField, repeatField, positiveBtn);
        passwordField.addTextChangedListener(validator);
        repeatField.addTextChangedListener(validator);

        positiveBtn.setVisibility(View.VISIBLE);
        positiveBtn.setText(R.string.password_set_action);
        positiveBtn.setEnabled(false);
        positiveBtn.setOnClickListener(v -> {
            final String passwordValue = passwordField.getText().toString();
            if (lockStore.setPassword(passwordValue)) {
                dismiss();
                lockStore.unlock();
                onSuccess.run();
            }
        });
    }

    private TextListener buildValidator(EditText passwordField, EditText repeatField,
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
