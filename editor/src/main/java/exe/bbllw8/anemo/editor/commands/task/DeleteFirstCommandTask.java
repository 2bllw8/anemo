/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.editor.commands.task;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public final class DeleteFirstCommandTask implements Callable<String> {
    @NonNull
    private final String toDelete;
    @NonNull
    private final String content;
    private final int count;
    private final int cursor;

    public DeleteFirstCommandTask(@NonNull String toDelete,
                                  @NonNull String content,
                                  int count,
                                  int cursor) {
        this.toDelete = toDelete;
        this.content = content;
        this.count = count;
        this.cursor = cursor;
    }

    @Override
    public String call() {
        return content.substring(0, cursor) + delete(Pattern.compile(toDelete),
                content.substring(cursor),
                count);
    }

    @NonNull
    private String delete(@NonNull Pattern pattern,
                          @NonNull String content,
                          int count) {
        if (count == 0) {
            return content;
        } else {
            return delete(pattern,
                    pattern.matcher(content).replaceFirst(""),
                    count - 1);
        }
    }
}
