/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.history;

import androidx.annotation.NonNull;

import java.util.Optional;

final class HistoryBuffer {

    @NonNull
    private final HistoryEntry[] stack;
    private final int size;
    private int i;

    public HistoryBuffer(int size) {
        this.stack = new HistoryEntry[size];
        this.size = size;
        this.i = -1;
    }

    public void push(@NonNull HistoryEntry entry) {
        stack[++i % size] = entry;
    }

    @NonNull
    public Optional<HistoryEntry> pop() {
        if (i < 0) {
            i = size - 1;
        }
        return Optional.ofNullable(stack[i-- % size]);
    }
}
