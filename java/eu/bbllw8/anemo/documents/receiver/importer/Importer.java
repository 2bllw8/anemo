/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.receiver.importer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import eu.bbllw8.anemo.documents.home.HomeEnvironment;
import eu.bbllw8.anemo.task.TaskExecutor;

public abstract class Importer {
    private static final String TAG = "Importer";
    private static final String[] NAME_PROJECTION = {OpenableColumns.DISPLAY_NAME};
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";

    @NonNull
    protected final HomeEnvironment homeEnvironment;
    @NonNull
    protected DateTimeFormatter dateTimeFormatter;
    @NonNull
    protected final Resources resources;
    @NonNull
    private final ContentResolver contentResolver;

    public Importer(@NonNull Context context) throws IOException {
        this.homeEnvironment = HomeEnvironment.getInstance(context);
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
        this.resources = context.getResources();
        this.contentResolver = context.getContentResolver();
    }

    public final boolean typeMatch(@NonNull String string) {
        return string.startsWith(getTypePrefix());
    }

    public final void execute(@NonNull Uri uri,
                              @NonNull Consumer<String> onStartImport,
                              @NonNull BiConsumer<String, String> onImportSuccess,
                              @NonNull Consumer<String> onImportFail) {
        final String fileName = getFileName(uri)
                .orElseGet(this::getDefaultName);

        final Optional<File> destinationFolderOpt = getDestinationFolder();
        if (!destinationFolderOpt.isPresent()) {
            Log.e(TAG, "Missing destination folder");
            onImportFail.accept(fileName);
            return;
        }

        final File destinationFolder = destinationFolderOpt.get();
        final File destinationFile = new File(destinationFolder, fileName);

        onStartImport.accept(fileName);
        TaskExecutor.runTask(() -> {
            try (final InputStream inputStream = contentResolver.openInputStream(uri)) {
                writeStream(inputStream, destinationFile);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to import", e);
                return false;
            }
        }, success -> {
            if (success) {
                onImportSuccess.accept(destinationFolder.getName(), fileName);
            } else {
                onImportFail.accept(fileName);
            }
        });
    }

    private void writeStream(@NonNull InputStream inputStream,
                             @NonNull File destination) throws IOException {
        try (final OutputStream oStream = new FileOutputStream(destination)) {
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
    protected abstract String getTypePrefix();

    @NonNull
    protected abstract Optional<File> getDestinationFolder();

    @NonNull
    protected abstract String getDefaultName();
}
