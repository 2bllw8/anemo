/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.editor.commands.parse;

import androidx.annotation.NonNull;

import exe.bbllw8.anemo.editor.commands.EditorCommand;

public interface CommandParser<T extends EditorCommand> {

    boolean matches(@NonNull String command);

    @NonNull
    T parse(@NonNull String command);
}
