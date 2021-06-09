/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.history;

import androidx.annotation.Nullable;

final class HistoryEntry {
    @Nullable
    private final CharSequence before;
    @Nullable
    private final CharSequence after;
    private final int start;

    public HistoryEntry(@Nullable CharSequence before,
                        @Nullable CharSequence after,
                        int start) {
        this.before = before;
        this.after = after;
        this.start = start;
    }

    @Nullable
    public CharSequence getBefore() {
        return before;
    }

    @Nullable
    public CharSequence getAfter() {
        return after;
    }

    public int getStart() {
        return start;
    }
}
