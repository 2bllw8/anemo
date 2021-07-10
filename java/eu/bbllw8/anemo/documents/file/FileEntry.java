/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.file;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

final class FileEntry {
    @NonNull
    private final String id;
    @NonNull
    private final String displayName;
    private final int flags;
    private final long lastModified;
    @NonNull
    public final String mimeType;
    private final long size;

    public FileEntry(@NonNull String id,
                     @NonNull String displayName,
                     int flags,
                     long lastModified,
                     @NonNull String mimeType,
                     long size) {
        this.id = id;
        this.displayName = displayName;
        this.flags = flags;
        this.lastModified = lastModified;
        this.mimeType = mimeType;
        this.size = size;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    public int getFlags() {
        return flags;
    }

    public long getLastModified() {
        return lastModified;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    public long getSize() {
        return size;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileEntry)) {
            return false;
        }
        final FileEntry that = (FileEntry) o;
        return flags == that.flags
                && lastModified == that.lastModified
                && size == that.size
                && id.equals(that.id)
                && displayName.equals(that.displayName)
                && mimeType.equals(that.mimeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,
                displayName,
                flags,
                lastModified,
                mimeType,
                size);
    }
}
