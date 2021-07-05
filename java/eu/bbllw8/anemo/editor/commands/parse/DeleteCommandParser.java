/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.commands.parse;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import eu.bbllw8.anemo.editor.commands.EditorCommand;

public final class DeleteCommandParser implements CommandParser<EditorCommand.Delete> {
    private static final Pattern DELETE_PATTERN = Pattern.compile("^d/.+/$");

    @Override
    public boolean matches(@NonNull String command) {
        return DELETE_PATTERN.matcher(command).find();
    }

    @NonNull
    @Override
    public EditorCommand.Delete parse(@NonNull String command) {
        final String toDelete = command.substring(2, command.length() - 1);
        return new EditorCommand.Delete(toDelete);
    }
}
