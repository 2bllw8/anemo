/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.snippet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import eu.bbllw8.anemo.home.HomeEnvironment;
import eu.bbllw8.anemo.task.TaskExecutor;
import eu.bbllw8.anemo.tip.TipDialog;

public final class SnippetTakingActivity extends Activity {
    private static final String TAG = "SnippetTakingActivity";

    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";

    private HomeEnvironment homeEnvironment;
    private DateTimeFormatter dateTimeFormatter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            homeEnvironment = HomeEnvironment.getInstance(this);
            dateTimeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load home environment", e);
            finish();
            return;
        }

        final Intent intent = getIntent();
        final Optional<String> textOpt = Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())
                ? Optional.ofNullable(intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT))
                : Optional.empty();

        if (textOpt.isPresent()) {
            showDialog(textOpt.get());
        } else {
            finish();
        }
    }

    private void showDialog(@NonNull String text) {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(R.layout.dialog_save_snippet)
                .create();

        dialog.setTitle(R.string.snippet_taking_label);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.snippet_save),
                (d, which) -> onSaveSnippet(dialog));
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.snippet_cancel),
                (d, which) -> {
                    d.dismiss();
                    finish();
                });
        dialog.show();

        final EditText snippetNameView = dialog.findViewById(R.id.snippetNameView);
        snippetNameView.setText(getString(R.string.snippet_file_name,
                dateTimeFormatter.format(LocalDateTime.now())));
        final EditText snippetTextView = dialog.findViewById(R.id.snippetTextView);
        snippetTextView.setText(text);
        snippetTextView.requestFocus();
        snippetTextView.setSelection(text.length());
    }

    private void onSaveSnippet(@NonNull Dialog dialog) {
        final EditText snippetNameView = dialog.findViewById(R.id.snippetNameView);
        final EditText snippetTextView = dialog.findViewById(R.id.snippetTextView);

        final String name = snippetNameView.getText().toString();
        final String snippet = snippetTextView.getText().toString();
        dialog.dismiss();

        final AtomicReference<TipDialog> tipDialogRef = new AtomicReference<>(
                showImportInProgress());

        final Runnable autoDismiss = () -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
            final TipDialog tipDialog = tipDialogRef.get();
            if (tipDialog != null && tipDialog.isShowing()) {
                tipDialog.dismiss();
            }
            finish();
        }, 2500);

        TaskExecutor.runTask(() -> writeSnippet(name, snippet),
                success -> {
                    tipDialogRef.getAndSet(success
                            ? showImportSuccess(name)
                            : showImportFail())
                            .dismiss();
                    autoDismiss.run();
                });
    }

    @WorkerThread
    private boolean writeSnippet(@NonNull String fileName,
                                 @NonNull String text) {
        final Optional<File> snippetsDirOpt = homeEnvironment.getDefaultDirectory(
                HomeEnvironment.SNIPPETS);
        if (!snippetsDirOpt.isPresent()) {
            Log.e(TAG, "Can't access the " + HomeEnvironment.SNIPPETS + " directory");
            return false;
        }

        final File file = new File(snippetsDirOpt.get(), fileName);
        try (final OutputStream outputStream = new FileOutputStream(file)) {
            try (final InputStream inputStream = new ByteArrayInputStream(text.getBytes())) {
                final byte[] buffer = new byte[4096];
                int read = inputStream.read(buffer);
                while (read > 0) {
                    outputStream.write(buffer, 0, read);
                    read = inputStream.read(buffer);
                }
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Can't write " + file.getAbsolutePath(), e);
            return false;
        }
    }

    @NonNull
    private TipDialog showImportInProgress() {
        return new TipDialog.Builder(this)
                .setMessage(getString(R.string.snippet_save_progress))
                .setCancelable(false)
                .setDismissOnTouchOutside(false)
                .setProgress()
                .show();
    }

    @NonNull
    private TipDialog showImportSuccess(@NonNull String fileName) {
        final String message = getString(R.string.snippet_save_success,
                HomeEnvironment.SNIPPETS,
                fileName);
        return new TipDialog.Builder(this)
                .setMessage(message)
                .setIcon(R.drawable.tip_ic_success)
                .setDismissOnTouchOutside(true)
                .setCancelable(true)
                .show();
    }

    @NonNull
    private TipDialog showImportFail() {
        return new TipDialog.Builder(this)
                .setMessage(getString(R.string.snippet_save_failure))
                .setIcon(R.drawable.tip_ic_error)
                .setDismissOnTouchOutside(true)
                .setCancelable(true)
                .show();
    }
}
