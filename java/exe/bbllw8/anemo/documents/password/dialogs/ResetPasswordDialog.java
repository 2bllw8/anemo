/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.password.dialogs;

import android.app.Activity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.IOException;

import exe.bbllw8.anemo.documents.home.HomeEnvironment;
import exe.bbllw8.anemo.documents.lock.LockStore;
import exe.bbllw8.anemo.documents.password.R;
import exe.bbllw8.anemo.documents.password.TextListener;

public final class ResetPasswordDialog extends PasswordDialog {
    private static final String TAG = "ResetPasswordDialog";

    private HomeEnvironment homeEnvironment;

    public ResetPasswordDialog(@NonNull Activity activity,
                               @NonNull LockStore lockStore) {
        super(activity, lockStore, R.string.password_reset_title, R.layout.password_reset);

        try {
            homeEnvironment = HomeEnvironment.getInstance(activity);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't load home environment", e);
            dismiss();
        }
    }

    @Override
    protected void build() {
        final TextView messageView = dialog.findViewById(R.id.resetMessageView);
        final EditText codeField = dialog.findViewById(R.id.resetFieldView);
        final Button positiveBtn = dialog.findViewById(R.id.resetBtnPositive);
        final Button negativeBtn = dialog.findViewById(R.id.resetBtnNegative);

        final String resetCode = res.getString(R.string.password_reset_code);

        final String resetMessage = res.getString(R.string.password_reset_message, resetCode);
        messageView.setText(resetMessage);

        final TextListener validator = buildValidator(positiveBtn, resetCode);
        codeField.addTextChangedListener(validator);

        positiveBtn.setEnabled(false);
        positiveBtn.setOnClickListener(v -> {
            try {
                homeEnvironment.wipe();
                lockStore.removePassword();
                lockStore.unlock();
                dismiss();
            } catch (IOException e) {
                Log.e(TAG, "Couldn't wipe home environment", e);
            }
        });
        negativeBtn.setOnClickListener(v -> dismiss());
    }

    @NonNull
    private TextListener buildValidator(@NonNull Button positiveButton,
                                        @NonNull String resetCode) {
        return text -> positiveButton.setEnabled(resetCode.equals(text));
    }
}
