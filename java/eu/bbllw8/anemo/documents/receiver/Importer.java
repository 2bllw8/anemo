/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.receiver;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import eu.bbllw8.anemo.documents.home.HomeEnvironment;
import eu.bbllw8.anemo.task.TaskExecutor;

public final class Importer {
    private static final String TAG = "Importer";
    private static final String[] NAME_PROJECTION = {OpenableColumns.DISPLAY_NAME};
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";

    @NonNull
    protected final HomeEnvironment homeEnvironment;
    @NonNull
    private final ContentResolver contentResolver;
    @NonNull
    private final Path destinationFolder;
    @NonNull
    private final String typePrefix;
    @NonNull
    protected DateTimeFormatter dateTimeFormatter;
    @NonNull
    private final String defaultNameBase;

    public Importer(@NonNull Context context,
                    @NonNull Path destinationFolder,
                    @NonNull String typePrefix,
                    @StringRes int defaultNameRes) throws IOException {
        this.homeEnvironment = HomeEnvironment.getInstance(context);
        this.destinationFolder = destinationFolder;
        this.typePrefix = typePrefix;
        this.contentResolver = context.getContentResolver();
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
        this.defaultNameBase = context.getString(defaultNameRes);
    }

    public boolean typeMatch(@NonNull String string) {
        return string.startsWith(typePrefix);
    }

    public void execute(@NonNull Uri uri,
                        @NonNull Consumer<String> onStartImport,
                        @NonNull BiConsumer<String, String> onImportSuccess,
                        @NonNull Consumer<String> onImportFail) {
        final String fileName = getFileName(uri)
                .orElseGet(this::getDefaultName);

        final Path destination = destinationFolder.resolve(fileName);

        onStartImport.accept(fileName);
        TaskExecutor.runTask(() -> {
            try (final InputStream inputStream = contentResolver.openInputStream(uri)) {
                writeStream(inputStream, destination);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to import", e);
                return false;
            }
        }, success -> {
            if (success) {
                onImportSuccess.accept(destinationFolder.getFileName().toString(), fileName);
            } else {
                onImportFail.accept(fileName);
            }
        });
    }

    private void writeStream(@NonNull InputStream inputStream,
                             @NonNull Path destination) throws IOException {
        try (final OutputStream oStream = Files.newOutputStream(destination)) {
            final byte[] buffer = new byte[4096];
            int read = inputStream.read(buffer);
            while (read > 0) {
                oStream.write(buffer, 0, read);
                read = inputStream.read(buffer);
            }
        }
    }

    @NonNull
    private Optional<String> getFileName(@NonNull Uri uri) {
        try (final Cursor cursor = contentResolver.query(uri, NAME_PROJECTION, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return Optional.empty();
            } else {
                final String name = cursor.getString(cursor.getColumnIndex(NAME_PROJECTION[0]));
                return Optional.ofNullable(name);
            }
        }
    }

    @NonNull
    private String getDefaultName() {
        return String.format(defaultNameBase, dateTimeFormatter.format(Instant.now()));
    }
}
