/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.receiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.documents.home.HomeEnvironment;
import exe.bbllw8.anemo.task.TaskExecutor;

public final class ReceiverActivity extends Activity {
    private static final String TAG = "ReceiverActivity";

    private final TaskExecutor taskExecutor = new TaskExecutor();
    private final AtomicReference<Optional<Dialog>> dialogRef
            = new AtomicReference<>(Optional.empty());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) {
            Log.e(TAG, "Nothing to do");
            finish();
            return;
        }

        final String type = intent.getType();
        if (type != null) {
            try {
                final Importer[] importers = getImporters();
                for (final Importer importer : importers) {
                    if (importer.typeMatch(type)) {
                        runImporter(importer, intent);
                        break;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed import", e);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        dialogRef.get().ifPresent(Dialog::dismiss);
        taskExecutor.terminate();
        super.onDestroy();
    }

    private Importer[] getImporters() throws IOException {
        final HomeEnvironment homeEnvironment = HomeEnvironment.getInstance(this);
        final Path fallbackDir = homeEnvironment.getBaseDir();
        return new Importer[]{
                // Audio
                new Importer(this,
                        taskExecutor,
                        homeEnvironment.getDefaultDirectory(HomeEnvironment.MUSIC)
                                .orElse(fallbackDir),
                        "audio/",
                        R.string.receiver_audio_default_name),
                // Images
                new Importer(this,
                        taskExecutor,
                        homeEnvironment.getDefaultDirectory(HomeEnvironment.PICTURES)
                                .orElse(fallbackDir),
                        "image/",
                        R.string.receiver_image_default_name),
                // PDF
                new Importer(this,
                        taskExecutor,
                        homeEnvironment.getDefaultDirectory(HomeEnvironment.DOCUMENTS)
                                .orElse(fallbackDir),
                        "application/pdf",
                        R.string.receiver_pdf_default_name),
                // Video
                new Importer(this,
                        taskExecutor,
                        homeEnvironment.getDefaultDirectory(HomeEnvironment.MOVIES)
                                .orElse(fallbackDir),
                        "video/",
                        R.string.receiver_video_default_name),
        };
    }

    private void runImporter(Importer importer, Intent intent) {
        importer.execute(intent.getParcelableExtra(Intent.EXTRA_STREAM),
                fileName -> {
                    final Dialog dialog = new AlertDialog.Builder(this, R.style.DialogTheme)
                            .setMessage(getString(R.string.receiver_importing_message, fileName))
                            .setCancelable(false)
                            .create();
                    dialogRef.getAndSet(Optional.of(dialog)).ifPresent(Dialog::dismiss);
                    dialog.show();
                },
                path -> {
                    final Dialog dialog = new AlertDialog.Builder(this, R.style.DialogTheme)
                            .setMessage(getString(R.string.receiver_importing_done_ok, path))
                            .setPositiveButton(android.R.string.ok, (d, which) -> d.dismiss())
                            .setOnDismissListener(d -> finish())
                            .create();
                    dialogRef.getAndSet(Optional.of(dialog)).ifPresent(Dialog::dismiss);
                    dialog.show();
                },
                fileName -> {
                    final Dialog dialog = new AlertDialog.Builder(this, R.style.DialogTheme)
                            .setMessage(getString(R.string.receiver_importing_done_fail, fileName))
                            .setPositiveButton(android.R.string.ok, (d, which) -> d.dismiss())
                            .setOnDismissListener(d -> finish())
                            .create();
                    dialogRef.getAndSet(Optional.of(dialog)).ifPresent(Dialog::dismiss);
                    dialog.show();
                }
        );
    }
}
