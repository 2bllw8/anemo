/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.editor.help;

import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.Callable;

import exe.bbllw8.anemo.editor.R;
import exe.bbllw8.anemo.editor.markdown.MarkdownFormatter;

final class LoadHelpContentTextTask implements Callable<Optional<CharSequence>>  {
    private static final String TAG = "LoadHelpTextTask";
    @NonNull
    private final Resources resources;

    public LoadHelpContentTextTask(@NonNull Resources resources) {
        this.resources = resources;
    }

    @Override
    public Optional<CharSequence> call() {
        final StringBuilder sb = new StringBuilder();
        try (InputStream reader = resources.openRawResource(R.raw.editor_help)) {
            final byte[] buffer = new byte[4096];
            int read = reader.read(buffer, 0, 4096);
            while (read > 0) {
                sb.append(new String(buffer, 0, read));
                read = reader.read(buffer, 0, 4096);
            }

            final CharSequence original = sb.toString();
            return Optional.of(MarkdownFormatter.format(original));
        } catch (IOException e) {
            Log.e(TAG, "Failed to read file", e);
            return Optional.empty();
        }
    }
}
