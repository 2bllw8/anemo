/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.commands;

import androidx.annotation.NonNull;

public class EditorCommand {

    private EditorCommand() {
    }

    public static class DeleteAll extends EditorCommand {
        @NonNull
        private final String toDelete;

        public DeleteAll(@NonNull String toDelete) {
            this.toDelete = toDelete;
        }

        @NonNull
        public String getToDelete() {
            return toDelete;
        }
    }

    public static class DeleteFirst extends EditorCommand {
        private final int count;
        @NonNull
        private final String toDelete;

        public DeleteFirst(int count,
                         @NonNull String toDelete) {
            this.count = count;
            this.toDelete = toDelete;
        }

        public int getCount() {
            return count;
        }

        @NonNull
        public String getToDelete() {
            return toDelete;
        }
    }

    public static class Find extends EditorCommand {
        @NonNull
        private final String toFind;

        public Find(@NonNull String toFind) {
            this.toFind = toFind;
        }

        @NonNull
        public String getToFind() {
            return toFind;
        }
    }

    public final static class Set extends EditorCommand {
        @NonNull
        private final String key;
        @NonNull
        private final String value;

        public Set(@NonNull String key,
                   @NonNull String value) {
            this.key = key;
            this.value = value;
        }

        @NonNull
        public String getKey() {
            return key;
        }

        @NonNull
        public String getValue() {
            return value;
        }
    }

    public static class SubstituteAll extends EditorCommand {
        @NonNull
        private final String toFind;
        @NonNull
        private final String replaceWith;

        public SubstituteAll(@NonNull String toFind,
                             @NonNull String replaceWith) {
            this.toFind = toFind;
            this.replaceWith = replaceWith;
        }

        @NonNull
        public String getToFind() {
            return toFind;
        }

        @NonNull
        public String getReplaceWith() {
            return replaceWith;
        }
    }

    public static class SubstituteFirst extends EditorCommand {
        private final int count;
        @NonNull
        private final String toFind;
        @NonNull
        private final String replaceWith;

        public SubstituteFirst(int count,
                               @NonNull String toFind,
                               @NonNull String replaceWith) {
            this.count = count;
            this.toFind = toFind;
            this.replaceWith = replaceWith;
        }

        public int getCount() {
            return count;
        }

        @NonNull
        public String getToFind() {
            return toFind;
        }

        @NonNull
        public String getReplaceWith() {
            return replaceWith;
        }
    }
}
