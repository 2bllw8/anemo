/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.receiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.documents.home.HomeEnvironment;
import exe.bbllw8.anemo.task.TaskExecutor;
import exe.bbllw8.either.Try;

public class ReceiverActivity extends Activity {
    private static final String TAG = "ReceiverActivity";
    private static final int DOCUMENT_PICKER_REQ_CODE = 7;
    private static final String[] NAME_PROJECTION = {OpenableColumns.DISPLAY_NAME};

    private final TaskExecutor taskExecutor = new TaskExecutor();
    private final AtomicReference<Optional<Dialog>> dialogRef = new AtomicReference<>(
            Optional.empty());
    private final AtomicReference<Optional<Uri>> importRef = new AtomicReference<>(
            Optional.empty());

    private ContentResolver contentResolver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contentResolver = getContentResolver();

        final Intent intent = getIntent();
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) {
            Log.e(TAG, "Nothing to do");
            finish();
            return;
        }

        final String type = intent.getType();
        if (type == null) {
            Log.e(TAG, "Can't determine type of sent content");
            finish();
            return;
        }

        final Uri source = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        importRef.set(Optional.of(source));

        final Intent pickerIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(type)
                .putExtra(Intent.EXTRA_TITLE,
                        getFileName(source).orElse(getString(R.string.receiver_default_file_name)))
                .putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                        DocumentsContract.buildRootsUri(HomeEnvironment.AUTHORITY));
        startActivityForResult(pickerIntent, DOCUMENT_PICKER_REQ_CODE);
    }

    @Override
    protected void onDestroy() {
        dialogRef.getAndSet(null).ifPresent(Dialog::dismiss);
        importRef.set(null);
        taskExecutor.terminate();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DOCUMENT_PICKER_REQ_CODE) {
            if (resultCode == RESULT_OK) {
                doImport(data.getData());
            } else {
                Log.d(TAG, "Action canceled");
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void doImport(Uri destination) {
        final Optional<Uri> sourceOpt = importRef.get();
        // No Optional#isEmpty() in android
        // noinspection SimplifyOptionalCallChains
        if (!sourceOpt.isPresent()) {
            Log.e(TAG, "Nothing to import");
            return;
        }
        final Uri source = sourceOpt.get();

        onImportStarted();
        taskExecutor.runTask(() -> copyUriToUri(source, destination), result -> result
                .forEach(success -> onImportSucceeded(), failure -> onImportFailed()));
    }

    private void onImportStarted() {
        final Dialog dialog = new AlertDialog.Builder(this, R.style.DialogTheme)
                .setMessage(getString(R.string.receiver_importing_message))
                .setCancelable(false)
                .create();
        dialogRef.getAndSet(Optional.of(dialog)).ifPresent(Dialog::dismiss);
    }

    private void onImportSucceeded() {
        final Dialog dialog = new AlertDialog.Builder(this, R.style.DialogTheme)
                .setMessage(getString(R.string.receiver_importing_done_ok))
                .setPositiveButton(android.R.string.ok, (d, which) -> d.dismiss())
                .setOnDismissListener(d -> finish())
                .create();
        dialogRef.getAndSet(Optional.of(dialog)).ifPresent(Dialog::dismiss);
        dialog.show();
    }

    private void onImportFailed() {
        final Dialog dialog = new AlertDialog.Builder(this, R.style.DialogTheme)
                .setMessage(getString(R.string.receiver_importing_done_fail))
                .setPositiveButton(android.R.string.ok, (d, which) -> d.dismiss())
                .setOnDismissListener(d -> finish())
                .create();
        dialogRef.getAndSet(Optional.of(dialog)).ifPresent(Dialog::dismiss);
        dialog.show();
    }

    private Try<Void> copyUriToUri(Uri source, Uri destination) {
        return Try.from(() -> {
            try (InputStream iStream = contentResolver.openInputStream(source)) {
                try (OutputStream oStream = contentResolver.openOutputStream(destination)) {
                    final byte[] buffer = new byte[4096];
                    int read = iStream.read(buffer);
                    while (read > 0) {
                        oStream.write(buffer, 0, read);
                        read = iStream.read(buffer);
                    }
                }
            }
            return null;
        });
    }

    private Optional<String> getFileName(Uri uri) {
        try (final Cursor cursor = contentResolver.query(uri, NAME_PROJECTION, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return Optional.empty();
            } else {
                final int nameIndex = cursor.getColumnIndex(NAME_PROJECTION[0]);
                if (nameIndex >= 0) {
                    final String name = cursor.getString(nameIndex);
                    return Optional.ofNullable(name);
                } else {
                    return Optional.empty();
                }
            }
        }
    }
}
