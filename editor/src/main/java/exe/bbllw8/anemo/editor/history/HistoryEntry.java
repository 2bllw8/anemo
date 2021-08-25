/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.editor.history;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class HistoryEntry implements Parcelable {
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

    protected HistoryEntry(@NonNull Parcel in) {
        before = in.readBoolean()
                ? in.readString()
                : null;
        after = in.readBoolean()
                ? in.readString()
                : null;
        start = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        if (before == null) {
            dest.writeBoolean(false);
        } else {
            dest.writeBoolean(true);
            dest.writeString(before.toString());
        }
        if (after == null) {
            dest.writeBoolean(false);
        } else {
            dest.writeBoolean(true);
            dest.writeString(after.toString());
        }
        dest.writeInt(start);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HistoryEntry> CREATOR = new Creator<>() {
        @NonNull
        @Override
        public HistoryEntry createFromParcel(@NonNull Parcel in) {
            return new HistoryEntry(in);
        }

        @NonNull
        @Override
        public HistoryEntry[] newArray(int size) {
            return new HistoryEntry[size];
        }
    };

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
