/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.receiver;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.StringRes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

import exe.bbllw8.anemo.task.TaskExecutor;
import exe.bbllw8.either.Try;

public final class Importer {
    private static final String TAG = "Importer";
    private static final String[] NAME_PROJECTION = {OpenableColumns.DISPLAY_NAME};
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";

    private final TaskExecutor taskExecutor;
    private final ContentResolver contentResolver;
    private final Path destinationFolder;
    private final String typePrefix;
    private final DateTimeFormatter dateTimeFormatter;
    private final String defaultNameBase;

    public Importer(Context context,
                    TaskExecutor taskExecutor,
                    Path destinationFolder,
                    String typePrefix,
                    @StringRes int defaultNameRes) {
        this.taskExecutor = taskExecutor;
        this.destinationFolder = destinationFolder;
        this.typePrefix = typePrefix;
        this.contentResolver = context.getContentResolver();
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
        this.defaultNameBase = context.getString(defaultNameRes);
    }

    public boolean typeMatch(String string) {
        return string.startsWith(typePrefix);
    }

    public void execute(Uri uri,
                        Consumer<String> onStartImport,
                        Consumer<String> onImportSuccess,
                        Consumer<String> onImportFail) {
        final String fileName = getFileName(uri)
                .orElseGet(this::getDefaultName);

        onStartImport.accept(fileName);
        taskExecutor.runTask(() -> Try.from(() -> {
                    final Path destination = destinationFolder.resolve(fileName);
                    try (final InputStream inputStream = contentResolver.openInputStream(uri)) {
                        writeStream(inputStream, destination);
                    }
                    return destinationFolder.getFileName() + "/" + fileName;
                }),
                result -> result.forEach(onImportSuccess, failure -> {
                    Log.e(TAG, "Failed to import", failure);
                    onImportFail.accept(fileName);
                }));
    }

    private void writeStream(InputStream inputStream, Path destination) throws IOException {
        try (final OutputStream oStream = Files.newOutputStream(destination)) {
            final byte[] buffer = new byte[4096];
            int read = inputStream.read(buffer);
            while (read > 0) {
                oStream.write(buffer, 0, read);
                read = inputStream.read(buffer);
            }
        }
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

    private String getDefaultName() {
        return String.format(defaultNameBase, dateTimeFormatter.format(Instant.now()));
    }
}
