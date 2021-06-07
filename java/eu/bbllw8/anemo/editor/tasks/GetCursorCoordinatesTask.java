/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.tasks;

import android.graphics.Point;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;

public final class GetCursorCoordinatesTask implements Callable<Point> {

    @NonNull
    private final String text;
    private final int cursorPosition;

    public GetCursorCoordinatesTask(@NonNull String text,
                                    int cursorPosition) {
        this.text = text;
        this.cursorPosition = cursorPosition;
    }

    @Override
    public Point call() {
        int column = 1;
        int row = 1;
        int i = 0;
        while (i < cursorPosition) {
            if (text.charAt(i++) == '\n') {
                column = 1;
                row++;
            } else {
                column++;
            }
        }

        return new Point(column, row);
    }
}
