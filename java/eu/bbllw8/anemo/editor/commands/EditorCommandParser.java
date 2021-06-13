/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.commands;

import androidx.annotation.NonNull;

import java.util.Optional;
import java.util.regex.Pattern;

public final class EditorCommandParser {
    private static final Pattern DELETE_PATTERN = Pattern.compile("^d/.+/");
    private static final Pattern FIND_PATTERN = Pattern.compile("^/.+/?");
    private static final Pattern SUBSTITUTE_PATTERN = Pattern.compile("^s/.+/.+/");

    @NonNull
    public Optional<EditorCommand> parse(@NonNull String command) {
        if (command.isEmpty()) {
            return Optional.empty();
        } else {
            if (DELETE_PATTERN.matcher(command).find()) {
                final String toDelete = command.substring(2, command.length() - 1);
                return Optional.of(new EditorCommand.Delete(toDelete));
            } else if (FIND_PATTERN.matcher(command).find()) {
                final int endOffset = command.endsWith("/") ? 1 : 0;
                final String toFind = command.substring(1, command.length() - endOffset);
                return Optional.of(new EditorCommand.Find(toFind));
            } else if (SUBSTITUTE_PATTERN.matcher(command).find()) {
                final int lastDivider = command.substring(0, command.length() - 1)
                        .lastIndexOf('/');
                final String toFind = command.substring(2, lastDivider);
                final String replaceWith = command.substring(lastDivider + 1, command.length() - 1);
                return Optional.of(new EditorCommand.Substitute(toFind, replaceWith));
            } else {
                return Optional.empty();
            }
        }
    }
}
