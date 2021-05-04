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
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

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
import java.util.function.Consumer;

import eu.bbllw8.anemo.home.HomeEnvironment;

public final class SnippetTakingActivity extends Activity {
    private static final String TAG = "SnippetTakingActivity";

    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";

    private HomeEnvironment homeEnvironment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            homeEnvironment = HomeEnvironment.getInstance(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load home environment", e);
            finish();
        }

        final Intent intent = getIntent();
        final String action = intent.getAction();

        final String text = Intent.ACTION_PROCESS_TEXT.equals(action)
                ? intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
                : null;

        if (text == null) {
            finish();
        } else {
            showDialog(text);
        }
    }

    private void showDialog(@NonNull String text) {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(R.layout.dialog_save_snippet)
                .create();

        dialog.setTitle(R.string.snippet_taking_label);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(d -> finish());
        dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.snippet_save),
                (d, which) -> onSaveSnippet(dialog));
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.snippet_cancel),
                (d, which) -> d.dismiss());
        dialog.show();

        final EditText snippetTextView = dialog.findViewById(R.id.snippetTextView);
        snippetTextView.setText(text);
        snippetTextView.setSelection(text.length());
    }

    private void onSaveSnippet(@NonNull Dialog dialog) {
        final EditText snippetTextView = dialog.findViewById(R.id.snippetTextView);
        final String snippet = snippetTextView.getText().toString();
        saveSnippet(snippet, success -> {
            int message = success
                    ? R.string.snippet_save_succeed
                    : R.string.snippet_save_failed;
            Toast.makeText(this, message, Toast.LENGTH_SHORT)
                    .show();
            dialog.dismiss();
        });
    }

    private void saveSnippet(@NonNull String text,
                             @NonNull Consumer<Boolean> callback) {
        final String fileName = getString(R.string.snippet_file_name,
                DateTimeFormatter.ofPattern(TIME_FORMAT).format(LocalDateTime.now()));
        TaskExecutor.runTask(() -> writeSnippet(fileName, text), callback);
    }


    @WorkerThread
    private boolean writeSnippet(@NonNull String fileName,
                                 @NonNull String text) {
        final File snippetsDir = homeEnvironment.getDefaultDirectory(HomeEnvironment.SNIPPETS);
        if (snippetsDir == null) {
            Log.e(TAG, "Can't access the " + HomeEnvironment.SNIPPETS + " directory");
            return false;
        }

        final File file = new File(snippetsDir, fileName);
        try (OutputStream outputStream = new FileOutputStream(file)) {
            try (InputStream inputStream = new ByteArrayInputStream(text.getBytes())) {
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
}
