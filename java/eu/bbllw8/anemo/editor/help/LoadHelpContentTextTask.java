/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.help;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.Callable;

import eu.bbllw8.anemo.editor.markdown.MarkdownFormatter;

final class LoadHelpContentTextTask implements Callable<Optional<CharSequence>>  {
    private static final String HELP_DOCUMENT = "editor_help.md";
    private static final String TAG = "LoadHelpTextTask";
    private final AssetManager am;

    public LoadHelpContentTextTask(AssetManager am) {
        this.am = am;
    }

    @Override
    public Optional<CharSequence> call() {
        final StringBuilder sb = new StringBuilder();
        try (InputStream reader = am.open(HELP_DOCUMENT)) {
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
