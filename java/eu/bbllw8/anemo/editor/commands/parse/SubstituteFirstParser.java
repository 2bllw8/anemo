/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.commands.parse;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import eu.bbllw8.anemo.editor.commands.EditorCommand;

public final class SubstituteFirstParser implements CommandParser<EditorCommand.SubstituteFirst> {
    private static final Pattern SUBSTITUTE_FIRST_PATTERN = Pattern.compile("^\\d+ s/.+/.+/?$");

    @Override
    public boolean matches(@NonNull String command) {
        return SUBSTITUTE_FIRST_PATTERN.matcher(command).find();
    }

    @NonNull
    @Override
    public EditorCommand.SubstituteFirst parse(@NonNull String command) {
        final int countDivider = command.indexOf(' ');
        final int count = Integer.parseInt(command.substring(0, countDivider));
        final int lastDivider = command.substring(countDivider, command.length() - 1)
                .lastIndexOf('/') + countDivider;
        final String toFind = command.substring(countDivider + 3, lastDivider);
        final String replaceWith = command.substring(lastDivider + 1, command.length() - 1);
        return new EditorCommand.SubstituteFirst(count, toFind, replaceWith);
    }
}
