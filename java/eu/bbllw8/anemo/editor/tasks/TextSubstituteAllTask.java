/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.tasks;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public final class TextSubstituteAllTask implements Callable<String> {
    @NonNull
    private final String toFind;
    @NonNull
    private final String replacement;
    @NonNull
    private final String content;

    public TextSubstituteAllTask(@NonNull String toFind,
                                 @NonNull String replacement,
                                 @NonNull String content) {
        this.toFind = toFind;
        this.replacement = replacement;
        this.content = content;
    }

    @Override
    public String call() {
        return Pattern.compile(toFind, Pattern.LITERAL)
                .matcher(content)
                .replaceAll(replacement);
    }
}
