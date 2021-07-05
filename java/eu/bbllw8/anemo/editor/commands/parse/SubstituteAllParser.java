/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.commands.parse;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import eu.bbllw8.anemo.editor.commands.EditorCommand;

public final class SubstituteAllParser implements CommandParser<EditorCommand.SubstituteAll> {
    private static final Pattern SUBSTITUTE_ALL_PATTERN = Pattern.compile("^s/.+/.+/$");

    @Override
    public boolean matches(@NonNull String command) {
        return SUBSTITUTE_ALL_PATTERN.matcher(command).find();
    }

    @NonNull
    @Override
    public EditorCommand.SubstituteAll parse(@NonNull String command) {
        final int lastDivider = command.substring(0, command.length() - 1)
                .lastIndexOf('/');
        final String toFind = command.substring(2, lastDivider);
        final String replaceWith = command.substring(lastDivider + 1, command.length() - 1);
        return new EditorCommand.SubstituteAll(toFind, replaceWith);
    }
}
