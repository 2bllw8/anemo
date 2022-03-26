/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.lock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.config.ConfigurationActivity;
import exe.bbllw8.anemo.config.password.TextListener;
import exe.bbllw8.anemo.shell.LauncherActivity;

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
            setupUI();
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

        passwordField.addTextChangedListener((TextListener) text ->
                unlockBtn.setEnabled(passwordField.getText().length() >= MIN_PASSWORD_LENGTH));

        configBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, ConfigurationActivity.class));
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
