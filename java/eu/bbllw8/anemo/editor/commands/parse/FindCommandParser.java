/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.commands.parse;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import eu.bbllw8.anemo.editor.commands.EditorCommand;

public final class FindCommandParser implements CommandParser<EditorCommand.Find> {
    private static final Pattern FIND_PATTERN = Pattern.compile("^/.+/?$");

    @Override
    public boolean matches(@NonNull String command) {
        return FIND_PATTERN.matcher(command).find();
    }

    @NonNull
    @Override
    public EditorCommand.Find parse(@NonNull String command) {
        final int endOffset = command.endsWith("/") ? 1 : 0;
        final String toFind = command.substring(1, command.length() - endOffset);
        return new EditorCommand.Find(toFind);
    }
}
