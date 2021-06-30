/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.io;

import android.content.ContentResolver;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.Callable;

public final class EditorFileReaderTask implements Callable<Optional<String>> {
    private static final String TAG = "EditorFileReaderTask";

    @NonNull
    private final ContentResolver cr;
    @NonNull
    private final EditorFile editorFile;
    private final int maxSize;

    public EditorFileReaderTask(@NonNull ContentResolver cr,
                                @NonNull EditorFile editorFile,
                                int maxSize) {
        this.cr = cr;
        this.editorFile = editorFile;
        this.maxSize = maxSize;
    }

    @NonNull
    @Override
    public Optional<String> call() {
        final StringBuilder sb = new StringBuilder();

        if (editorFile.getSize() > maxSize) {
            Log.e(TAG, "File is bigger than the max size supported by the configuration"
                    + editorFile.getSize() + " / " + maxSize);
            return Optional.empty();
        } else {
            try (final InputStream reader = cr.openInputStream(editorFile.getUri())) {
                final byte[] buffer = new byte[4096];
                int read = reader.read(buffer, 0, 4096);
                while (read > 0) {
                    sb.append(new String(buffer, 0, read));
                    read = reader.read(buffer, 0, 4096);
                }

                return Optional.of(sb.toString());
            } catch (IOException e) {
                Log.e(TAG, "Failed to read file", e);
                return Optional.empty();
            }
        }
    }
}
