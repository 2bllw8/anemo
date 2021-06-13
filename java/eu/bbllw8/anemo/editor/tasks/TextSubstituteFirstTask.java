/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.tasks;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public final class TextSubstituteFirstTask implements Callable<String> {
    @NonNull
    private final String toFind;
    @NonNull
    private final String replacement;
    @NonNull
    private final String content;
    private final int count;
    private final int cursor;

    public TextSubstituteFirstTask(@NonNull String toFind,
                                   @NonNull String replacement,
                                   @NonNull String content,
                                   int count,
                                   int cursor) {
        this.toFind = toFind;
        this.replacement = replacement;
        this.content = content;
        this.count = count;
        this.cursor = cursor;
    }

    @Override
    public String call() {
        return content.substring(0, cursor) + substitute(Pattern.compile(toFind, Pattern.LITERAL),
                content.substring(cursor),
                count);
    }

    @NonNull
    private String substitute(@NonNull Pattern pattern,
                              @NonNull String content,
                              int count) {
        if (count == 0) {
            return content;
        } else {
            return substitute(pattern,
                    pattern.matcher(content).replaceFirst(replacement),
                    count - 1);
        }
    }
}
