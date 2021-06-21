/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.io;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Optional;
import java.util.concurrent.Callable;

public final class EditorFileLoaderTask implements Callable<Optional<EditorFile>> {
    private static final String[] FILE_INFO_QUERY = {
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE
    };
    private static final String DEFAULT_TYPE = "text/plain";

    @NonNull
    private final ContentResolver cr;
    @NonNull
    private final Uri uri;
    @NonNull
    private final String type;


    public EditorFileLoaderTask(@NonNull ContentResolver cr,
                                @NonNull Uri uri,
                                @Nullable String type) {
        this.cr = cr;
        this.uri = uri;
        this.type = type == null ? DEFAULT_TYPE : type;
    }

    @NonNull
    @Override
    public Optional<EditorFile> call() {
        try (final Cursor infoCursor = cr.query(uri, FILE_INFO_QUERY, null, null, null)) {
            if (infoCursor.moveToFirst()) {
                final String name = infoCursor.getString(0);
                final long size = infoCursor.getLong(1);
                return Optional.of(new EditorFile(uri, name, type, size));
            } else {
                return Optional.empty();
            }
        }
    }
}
