/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.receiver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import eu.bbllw8.anemo.home.HomeEnvironment;

public final class ReceiverActivity extends Activity {

    private static final String TAG = "ReceiverActivity";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String TYPE_TEXT = "text/plain";
    public static final String TYPE_IMAGE = "image/";

    private HomeEnvironment homeEnvironment;
    private DateTimeFormatter dateTimeFormatter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            dateTimeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
            homeEnvironment = HomeEnvironment.getInstance(this);
            final Intent intent = getIntent();
            if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
                onReceiveSend(intent);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load home", e);
        } finally {
            finish();
        }
    }

    private void onReceiveSend(@NonNull Intent intent) {
        final String type = intent.getType();
        if (type == null) {
            return;
        }

        if (type.equals(TYPE_TEXT)) {
            importText(intent);
        } else if (type.startsWith(TYPE_IMAGE)) {
            importImage(intent);
        }
    }

    private void importText(@NonNull Intent intent) {
        final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (text == null) {
            return;
        }

        try (final InputStream iStream = new ByteArrayInputStream(text.getBytes())) {
            final String name = getString(R.string.receiver_text_file_name,
                    dateTimeFormatter.format(LocalDateTime.now()));

            writeStream(iStream,
                    HomeEnvironment.SNIPPETS,
                    name,
                    "txt");
        } catch (IOException e) {
            Log.e(TAG, "Failed to import text", e);
        }
    }

    private void importImage(@NonNull Intent intent) {
        final Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri == null) {
            return;
        }

        try (final InputStream iStream = getContentResolver().openInputStream(imageUri)) {
            final String name = intent.hasExtra(Intent.EXTRA_TITLE)
                    ? intent.getStringExtra(Intent.EXTRA_TITLE)
                    : getString(R.string.receiver_text_image_name,
                    dateTimeFormatter.format(LocalDateTime.now()));

            writeStream(iStream,
                    HomeEnvironment.PICTURES,
                    name,
                    intent.getType().replace(TYPE_IMAGE, ""));
        } catch (IOException e) {
            Log.e(TAG, "Failed to import image", e);
        }
    }

    /* Write */
    private void writeStream(@NonNull InputStream iStream,
                             @NonNull String destination,
                             @NonNull String name,
                             @NonNull String extension) throws IOException {
        final File directory = homeEnvironment.getDefaultDirectory(destination);
        if (directory == null) {
            final File destFile = new File(directory, name + "." + extension);
            try (final OutputStream oStream = new FileOutputStream(destFile)) {
                final byte[] buffer = new byte[4096];
                int read = iStream.read(buffer);
                while (read > 0) {
                    oStream.write(buffer, 0, read);
                    read = iStream.read(buffer);
                }
            }
        }
    }
}
