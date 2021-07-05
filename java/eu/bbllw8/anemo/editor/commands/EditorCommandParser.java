/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.commands;

import androidx.annotation.NonNull;

import java.util.Optional;

import eu.bbllw8.anemo.editor.commands.parse.CommandParser;
import eu.bbllw8.anemo.editor.commands.parse.DeleteCommandParser;
import eu.bbllw8.anemo.editor.commands.parse.FindCommandParser;
import eu.bbllw8.anemo.editor.commands.parse.SubstituteAllParser;
import eu.bbllw8.anemo.editor.commands.parse.SubstituteFirstParser;

public final class EditorCommandParser {
    private static final CommandParser<?>[] COMMAND_PARSERS = {
            new DeleteCommandParser(),
            new FindCommandParser(),
            new SubstituteAllParser(),
            new SubstituteFirstParser(),
    };

    @NonNull
    public Optional<EditorCommand> parse(@NonNull String command) {
        if (!command.isEmpty()) {
            for (CommandParser<?> parser : COMMAND_PARSERS) {
                if (parser.matches(command)) {
                    return Optional.of(parser.parse(command));
                }
            }
        }
        return Optional.empty();
    }
}
