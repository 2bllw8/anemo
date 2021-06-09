/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.history;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Optional;

public final class HistoryBuffer implements Parcelable {

    @NonNull
    private final HistoryEntry[] stack;
    private final int size;
    private int i;

    public HistoryBuffer(int size) {
        this.stack = new HistoryEntry[size];
        this.size = size;
        this.i = -1;
    }

    protected HistoryBuffer(@NonNull Parcel in) {
        stack = in.createTypedArray(HistoryEntry.CREATOR);
        size = in.readInt();
        i = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedArray(stack, flags);
        dest.writeInt(size);
        dest.writeInt(i);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HistoryBuffer> CREATOR = new Creator<HistoryBuffer>() {
        @Override
        public HistoryBuffer createFromParcel(Parcel in) {
            return new HistoryBuffer(in);
        }

        @Override
        public HistoryBuffer[] newArray(int size) {
            return new HistoryBuffer[size];
        }
    };

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
