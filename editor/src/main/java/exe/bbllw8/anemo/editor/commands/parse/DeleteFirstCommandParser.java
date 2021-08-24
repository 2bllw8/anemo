/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.editor.commands.parse;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import exe.bbllw8.anemo.editor.commands.EditorCommand;

public final class DeleteFirstCommandParser implements CommandParser<EditorCommand.DeleteFirst> {
    private static final Pattern DELETE_FIRST_PATTERN = Pattern.compile("^\\d+ d/.+$");

    @Override
    public boolean matches(@NonNull String command) {
        return DELETE_FIRST_PATTERN.matcher(command).find();
    }

    @NonNull
    @Override
    public EditorCommand.DeleteFirst parse(@NonNull String command) {
        final int countDivider = command.indexOf(' ');
        final int count = Integer.parseInt(command.substring(0, countDivider));
        final String toDelete = command.substring(countDivider + 3);
        return new EditorCommand.DeleteFirst(count, toDelete);
    }
}
