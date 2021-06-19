/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.receiver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import eu.bbllw8.anemo.documents.receiver.importer.AudioImporter;
import eu.bbllw8.anemo.documents.receiver.importer.ImageImporter;
import eu.bbllw8.anemo.documents.receiver.importer.Importer;
import eu.bbllw8.anemo.documents.receiver.importer.PdfImporter;
import eu.bbllw8.anemo.documents.receiver.importer.VideoImporter;
import eu.bbllw8.anemo.tip.TipDialog;

public final class ReceiverActivity extends Activity {

    private static final String TAG = "ReceiverActivity";

    private Importer[] importers;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            importers = new Importer[]{
                    new AudioImporter(this),
                    new ImageImporter(this),
                    new PdfImporter(this),
                    new VideoImporter(this),
            };

            final Intent intent = getIntent();
            if (intent == null
                    || !Intent.ACTION_SEND.equals(intent.getAction())
                    || !onReceiveSend(intent)) {
                finish();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load home", e);
            finish();
        }
    }

    private boolean onReceiveSend(@NonNull Intent intent) {
        final String type = intent.getType();
        if (type == null) {
            return false;
        }

        for (final Importer importer : importers) {
            if (importer.typeMatch(type)) {
                return runImporter(importer, intent);
            }
        }
        return false;
    }

    private boolean runImporter(@NonNull Importer importer,
                                @NonNull Intent intent) {
        final Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri == null) {
            return false;
        }

        final AtomicReference<TipDialog> importDialogRef = new AtomicReference<>();
        final Runnable autoDismiss = () -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
            final TipDialog dialog = importDialogRef.get();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            finish();
        }, 2500);

        importer.execute(uri,
                fileName -> importDialogRef.set(showImportInProgress(fileName)),
                (destination, fileName) -> {
                    importDialogRef.getAndSet(showImportSuccess(destination, fileName))
                            .dismiss();
                    autoDismiss.run();
                },
                fileName -> {
                    importDialogRef.getAndSet(showImportFail(fileName))
                            .dismiss();
                    autoDismiss.run();
                });
        return true;
    }

    @NonNull
    private TipDialog showImportInProgress(@NonNull String name) {
        final String message = getString(R.string.receiver_importing_message, name);
        return new TipDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .setDismissOnTouchOutside(false)
                .setProgress()
                .show();
    }

    @NonNull
    private TipDialog showImportSuccess(@NonNull String destination,
                                        @NonNull String fileName) {
        final String message = getString(R.string.receiver_importing_done_ok,
                destination,
                fileName);
        return new TipDialog.Builder(this)
                .setMessage(message)
                .setIcon(R.drawable.tip_ic_success)
                .setDismissOnTouchOutside(true)
                .setCancelable(true)
                .show();
    }

    @NonNull
    private TipDialog showImportFail(@NonNull String name) {
        final String message = getString(R.string.receiver_importing_done_fail,
                name);
        return new TipDialog.Builder(this)
                .setMessage(message)
                .setIcon(R.drawable.tip_ic_error)
                .setDismissOnTouchOutside(true)
                .setCancelable(true)
                .show();
    }
}
