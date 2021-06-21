/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.io;

import android.content.ContentResolver;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

public final class EditorFileWriterTask implements Callable<Boolean> {
    private static final String TAG = "EditorFileWriterTask";

    @NonNull
    private final ContentResolver cr;
    @NonNull
    private final EditorFile editorFile;
    @NonNull
    private final String content;

    public EditorFileWriterTask(@NonNull ContentResolver cr,
                                @NonNull EditorFile editorFile,
                                @NonNull String content) {
        this.cr = cr;
        this.editorFile = editorFile;
        this.content = content;
    }

    @Override
    public Boolean call() {
        try (final OutputStream outputStream = cr.openOutputStream(editorFile.getUri())) {
            try (final InputStream inputStream = new ByteArrayInputStream(content.getBytes())) {
                final byte[] buffer = new byte[4096];
                int read = inputStream.read(buffer, 0, 4096);
                while (read > 0) {
                    outputStream.write(buffer, 0, read);
                    read = inputStream.read(buffer, 0, 4096);
                }
            }

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write file content", e);
            return false;
        }
    }
}
