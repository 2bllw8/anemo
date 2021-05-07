/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.receiver;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
import java.util.Optional;

import eu.bbllw8.anemo.home.HomeEnvironment;

public final class ReceiverActivity extends Activity {

    private static final String TAG = "ReceiverActivity";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String TYPE_TEXT = "text/plain";
    public static final String TYPE_AUDIO = "audio/";
    public static final String TYPE_IMAGE = "image/";
    public static final String TYPE_VIDEO = "video/";

    private static final String[] NAME_PROJECTION = {OpenableColumns.DISPLAY_NAME};

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
        } else if (type.startsWith(TYPE_AUDIO)) {
            importAudio(intent);
        } else if (type.startsWith(TYPE_IMAGE)) {
            importImage(intent);
        } else if (type.startsWith(TYPE_VIDEO)) {
            importVideo(intent);
        }
    }

    private void importText(@NonNull Intent intent) {
        final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (text == null) {
            return;
        }

        try (final InputStream iStream = new ByteArrayInputStream(text.getBytes())) {
            final String name = getString(R.string.receiver_text_file_name,
                    dateTimeFormatter.format(LocalDateTime.now()))
                    + ".txt";

            writeStream(iStream,
                    HomeEnvironment.SNIPPETS,
                    name);
        } catch (IOException e) {
            Log.e(TAG, "Failed to import text", e);
        }
    }

    private void importAudio(@NonNull Intent intent) {
        final Uri audioUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (audioUri == null) {
            return;
        }

        try (final InputStream iStream = getContentResolver().openInputStream(audioUri)) {
            final String name = getFileName(audioUri)
                    .orElseGet(() -> {
                        final String base = getString(R.string.receiver_text_audio_name,
                                dateTimeFormatter.format(LocalDateTime.now()));
                        return base + "." + intent.getType().replace(TYPE_AUDIO, "");
                    });

            writeStream(iStream,
                    HomeEnvironment.MUSIC,
                    name);
        } catch (IOException e) {
            Log.e(TAG, "Failed to import audio", e);
        }
    }

    private void importImage(@NonNull Intent intent) {
        final Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri == null) {
            return;
        }

        try (final InputStream iStream = getContentResolver().openInputStream(imageUri)) {
            final String name = getFileName(imageUri)
                    .orElseGet(() -> {
                        final String base = getString(R.string.receiver_text_image_name,
                                dateTimeFormatter.format(LocalDateTime.now()));
                        return base + "." + intent.getType().replace(TYPE_IMAGE, "");
                    });

            writeStream(iStream,
                    HomeEnvironment.PICTURES,
                    name);
        } catch (IOException e) {
            Log.e(TAG, "Failed to import image", e);
        }
    }

    private void importVideo(@NonNull Intent intent) {
        final Uri videoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (videoUri == null) {
            return;
        }

        try (final InputStream iStream = getContentResolver().openInputStream(videoUri)) {
            final String name = getFileName(videoUri)
                    .orElseGet(() -> {
                        final String base = getString(R.string.receiver_text_video_name,
                                dateTimeFormatter.format(LocalDateTime.now()));
                        return base + intent.getType().replace(TYPE_VIDEO, "");
                    });

            writeStream(iStream,
                    HomeEnvironment.MOVIES,
                    name);
        } catch (IOException e) {
            Log.e(TAG, "Failed to import video", e);
        }
    }

    @NonNull
    private Optional<String> getFileName(@NonNull Uri uri) {
        final ContentResolver cr = getContentResolver();
        try (final Cursor cursor = cr.query(uri, NAME_PROJECTION, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return Optional.empty();
            } else {
                final String name = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                return Optional.ofNullable(name);
            }
        }
    }

    /* Write */
    private void writeStream(@NonNull InputStream iStream,
                             @NonNull String destination,
                             @NonNull String name) throws IOException {
        final File directory = homeEnvironment.getDefaultDirectory(destination);
        if (directory != null) {
            final File destFile = new File(directory, name);
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
