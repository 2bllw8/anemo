/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.commands;

import androidx.annotation.NonNull;

import java.util.Optional;
import java.util.regex.PatternSyntaxException;

import eu.bbllw8.anemo.editor.commands.parse.CommandParser;
import eu.bbllw8.anemo.editor.commands.parse.DeleteAllCommandParser;
import eu.bbllw8.anemo.editor.commands.parse.DeleteFirstCommandParser;
import eu.bbllw8.anemo.editor.commands.parse.FindCommandParser;
import eu.bbllw8.anemo.editor.commands.parse.SetCommandParser;
import eu.bbllw8.anemo.editor.commands.parse.SubstituteAllParser;
import eu.bbllw8.anemo.editor.commands.parse.SubstituteFirstParser;

public final class EditorCommandParser {
    private static final CommandParser<?>[] COMMAND_PARSERS = {
            new DeleteAllCommandParser(),
            new DeleteFirstCommandParser(),
            new SetCommandParser(),
            new SubstituteAllParser(),
            new SubstituteFirstParser(),
            // Find always last resort
            new FindCommandParser(),
    };

    @NonNull
    public Optional<EditorCommand> parse(@NonNull String command) {
        if (!command.isEmpty()) {
            for (CommandParser<?> parser : COMMAND_PARSERS) {
                if (parser.matches(command)) {
                    try {
                        return Optional.of(parser.parse(command));
                    } catch (PatternSyntaxException e) {
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.empty();
    }
}
