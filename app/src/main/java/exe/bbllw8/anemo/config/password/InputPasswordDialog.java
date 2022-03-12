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

public final class InputPasswordDialog extends PasswordDialog {
    private final Runnable openConfiguration;

    public InputPasswordDialog(Activity activity,
                               LockStore lockStore,
                               Runnable onUnlocked,
                               Runnable openConfiguration) {
        super(activity, lockStore, onUnlocked, R.string.tile_unlock, R.layout.password_input);
        this.openConfiguration = openConfiguration;
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
                dismiss();
                lockStore.unlock();
                onSuccess.run();
            } else {
                passwordField.setError(res.getString(R.string.password_error_wrong));
            }
        });

        neutralBtn.setVisibility(View.VISIBLE);
        neutralBtn.setText(R.string.configuration_label);
        neutralBtn.setOnClickListener(v -> {
            dismiss();
            openConfiguration.run();
        });
    }

    private TextListener buildInputValidator(EditText passwordField,
                                             Button positiveBtn) {
        return text -> {
            final String value = passwordField.getText().toString();
            positiveBtn.setEnabled(value.length() >= MIN_PASSWORD_LENGTH);
        };
    }
}
