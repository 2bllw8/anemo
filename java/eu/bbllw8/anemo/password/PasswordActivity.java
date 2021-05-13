/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.password;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.bbllw8.anemo.lock.LockStore;

public final class PasswordActivity extends Activity {
    private static final int MIN_PASSWORD_LENGTH = 4;

    private LockStore lockStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lockStore = LockStore.getInstance(this);

        if (lockStore.hasPassword()) {
            showInputPasswordDialog();
        } else {
            showFirstSetPasswordDialog();
        }
    }

    /* First setup */

    private void showFirstSetPasswordDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.password_set_title)
                .setView(R.layout.password_first_set)
                .setCancelable(false)
                .show();

        onFirstSetPasswordDialogCreate(dialog);
    }

    private void onFirstSetPasswordDialogCreate(@NonNull AlertDialog dialog) {
        final EditText passwordField = dialog.findViewById(R.id.passwordFieldView);
        final EditText repeatField = dialog.findViewById(R.id.repeatFieldView);
        final Button positiveBtn = dialog.findViewById(R.id.setBtnPositive);
        final Button negativeBtn = dialog.findViewById(R.id.setBtnNegative);

        final TextListener textListener = new TextListener() {
            @Override
            protected void onTextChanged(@NonNull String text) {
                final String passwordValue = passwordField.getText().toString();
                final String repeatValue = repeatField.getText().toString();

                if (passwordValue.length() < MIN_PASSWORD_LENGTH) {
                    positiveBtn.setEnabled(false);
                    passwordField.setError(
                            getString(R.string.password_error_length, MIN_PASSWORD_LENGTH));
                    repeatField.setError(null);
                } else if (!passwordValue.equals(repeatValue)) {
                    positiveBtn.setEnabled(false);
                    passwordField.setError(null);
                    repeatField.setError(getString(R.string.password_error_mismatch));
                } else {
                    positiveBtn.setEnabled(true);
                    passwordField.setError(null);
                    repeatField.setError(null);
                }
            }
        };

        passwordField.addTextChangedListener(textListener);
        repeatField.addTextChangedListener(textListener);

        positiveBtn.setEnabled(false);
        positiveBtn.setOnClickListener(v -> {
            final String passwordValue = passwordField.getText().toString();
            if (lockStore.setPassword(passwordValue)) {
                onSuccess();
            }
        });

        negativeBtn.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
    }

    /* Input to unlock */

    private void showInputPasswordDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.password_input_title)
                .setView(R.layout.password_input)
                .setCancelable(false)
                .show();

        onInputFormDialogCreate(dialog);
    }

    private void onInputFormDialogCreate(@NonNull AlertDialog dialog) {
        final EditText passwordField = dialog.findViewById(R.id.passwordFieldView);
        final Button positiveBtn = dialog.findViewById(R.id.inputBtnPositive);
        final Button negativeBtn = dialog.findViewById(R.id.inputBtnNegative);

        passwordField.addTextChangedListener(new TextListener() {
            @Override
            protected void onTextChanged(@NonNull String text) {
                final String value = passwordField.getText().toString();
                positiveBtn.setEnabled(value.length() >= MIN_PASSWORD_LENGTH);
            }
        });

        positiveBtn.setEnabled(false);
        positiveBtn.setOnClickListener(v -> {
            final String value = passwordField.getText().toString();
            if (lockStore.passwordMatch(value)) {
                onSuccess();
            } else {
                passwordField.setError(getString(R.string.password_error_wrong));
            }
        });

        negativeBtn.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
    }

    /* Common */

    private void onSuccess() {
        lockStore.unlock();
        finish();
    }

    private static abstract class TextListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            onTextChanged(s.toString());
        }

        protected abstract void onTextChanged(@NonNull String text);
    }
}
