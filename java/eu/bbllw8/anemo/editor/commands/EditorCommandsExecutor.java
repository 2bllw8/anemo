/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.commands;

import androidx.annotation.NonNull;

public interface EditorCommandsExecutor {

    void runFindCommand(@NonNull EditorCommand.Find command);

    void runDeleteCommand(@NonNull EditorCommand.Delete command);

    void runSubstituteAllCommand(@NonNull EditorCommand.SubstituteAll command);

    void runSubstituteFirstCommand(@NonNull EditorCommand.SubstituteFirst command);

    default boolean runCommand(@NonNull EditorCommand command) {
        if (command instanceof EditorCommand.Find) {
            runFindCommand((EditorCommand.Find) command);
            return true;
        } else if (command instanceof EditorCommand.Delete) {
            runDeleteCommand((EditorCommand.Delete) command);
            return true;
        } else if (command instanceof EditorCommand.SubstituteAll) {
            runSubstituteAllCommand((EditorCommand.SubstituteAll) command);
            return true;
        } else if (command instanceof EditorCommand.SubstituteFirst) {
            runSubstituteFirstCommand((EditorCommand.SubstituteFirst) command);
            return true;
        } else {
            return false;
        }
    }
}
