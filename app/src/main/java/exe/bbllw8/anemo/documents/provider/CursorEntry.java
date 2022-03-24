/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.provider;

import java.util.Objects;

public final class CursorEntry {
    private final String id;
    private final String displayName;
    private final String mimeType;
    private final int flags;
    private final long lastModified;
    private final long size;

    public CursorEntry(String id, String displayName, String mimeType,
                       int flags, long lastModified, long size) {
        this.id = id;
        this.displayName = displayName;
        this.mimeType = mimeType;
        this.flags = flags;
        this.lastModified = lastModified;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getFlags() {
        return flags;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof CursorEntry) {
            final CursorEntry that = (CursorEntry) o;
            return flags == that.flags
                    && lastModified == that.lastModified
                    && size == that.size
                    && Objects.equals(id, that.id)
                    && Objects.equals(displayName, that.displayName)
                    && Objects.equals(mimeType, that.mimeType);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, mimeType, flags, lastModified, size);
    }
}
