/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.aeolus.lock;

import android.app.Activity;
import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.concurrent.Executor;

import exe.bbllw8.aeolus.R;
import exe.bbllw8.aeolus.config.ConfigurationActivity;
import exe.bbllw8.aeolus.config.password.TextListener;
import exe.bbllw8.aeolus.shell.LauncherActivity;

public final class UnlockActivity extends Activity {
    public static final String OPEN_AFTER_UNLOCK = "open_after_unlock";
    private static final int MIN_PASSWORD_LENGTH = 4;

    private LockStore lockStore;
    private Runnable onUnlocked;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lockStore = LockStore.getInstance(this);
        onUnlocked = getOnUnlocked(getIntent());
        if (lockStore.hasPassword()) {
            if (lockStore.isBiometricUnlockEnabled()) {
                unlockViaBiometricAuthentication();
            } else {
                setupUI();
            }
        } else {
            doUnlock();
        }
    }

    private void setupUI() {
        setContentView(R.layout.password_input);
        setFinishOnTouchOutside(true);

        final EditText passwordField = findViewById(R.id.passwordFieldView);
        final ImageView configBtn = findViewById(R.id.configurationButton);
        final Button unlockBtn = findViewById(R.id.unlockButton);
        final Button cancelBtn = findViewById(R.id.cancelButton);

        passwordField.addTextChangedListener((TextListener) text -> unlockBtn
                .setEnabled(passwordField.getText().length() >= MIN_PASSWORD_LENGTH));

        configBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, ConfigurationActivity.class));
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        unlockBtn.setEnabled(false);
        unlockBtn.setOnClickListener(v -> {
            final String value = passwordField.getText().toString();
            if (lockStore.passwordMatch(value)) {
                doUnlock();
            } else {
                passwordField.setError(getString(R.string.password_error_wrong));
            }
        });

        cancelBtn.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    private void doUnlock() {
        lockStore.unlock();
        onUnlocked.run();
    }

    @RequiresApi(29)
    private void unlockViaBiometricAuthentication() {
        final Executor executor = getMainExecutor();
        final CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(this::finish);

        final BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                .setTitle(getString(R.string.tile_unlock))
                .setDescription(getString(R.string.password_input_biometric_message))
                .setNegativeButton(getString(R.string.password_input_biometric_fallback), executor,
                        (dialog, which) -> setupUI())
                .build();
        prompt.authenticate(cancellationSignal, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        doUnlock();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                    }
                });
    }

    private Runnable getOnUnlocked(Intent intent) {
        if (intent.getBooleanExtra(OPEN_AFTER_UNLOCK, false)) {
            return () -> {
                startActivity(new Intent(this, LauncherActivity.class));
                setResult(Activity.RESULT_OK);
                finish();
            };
        } else {
            return () -> {
                setResult(Activity.RESULT_OK);
                finish();
            };
        }
    }
}
