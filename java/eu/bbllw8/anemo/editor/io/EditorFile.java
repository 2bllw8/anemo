/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.io;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public final class EditorFile implements Parcelable {
    @NonNull
    private final Uri uri;
    @NonNull
    private final String name;
    @NonNull
    private final String mimeType;
    private final long size;

    public EditorFile(@NonNull Uri uri,
                      @NonNull String name,
                      @NonNull String mimeType,
                      long size) {
        this.uri = uri;
        this.name = name;
        this.mimeType = mimeType;
        this.size = size;
    }

    @NonNull
    public Uri getUri() {
        return uri;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    public long getSize() {
        return size;
    }

    // Parcelable

    protected EditorFile(@NonNull Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        name = in.readString();
        mimeType = in.readString();
        size = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
        dest.writeString(name);
        dest.writeString(mimeType);
        dest.writeLong(size);
    }

    public static final Creator<EditorFile> CREATOR = new Creator<EditorFile>() {
        @NonNull
        @Override
        public EditorFile createFromParcel(@NonNull Parcel in) {
            return new EditorFile(in);
        }

        @NonNull
        @Override
        public EditorFile[] newArray(int size) {
            return new EditorFile[size];
        }
    };
}
