/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.file;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import eu.bbllw8.anemo.documents.home.HomeEnvironment;

final class FileSource {
    private static final String[] DEFAULT_PROJECTION = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
    };

    @NonNull
    private final ContentResolver cr;

    public FileSource(@NonNull ContentResolver cr) {
        this.cr = cr;
    }

    public List<FileEntry> browseRoot() {
        return filesInUri(uriForChildrenOf(HomeEnvironment.ROOT));
    }

    @NonNull
    public List<FileEntry> browseDir(@NonNull FileEntry fileEntry) {
        return filesInUri(uriForChildrenOf(fileEntry.getId()));
    }

    @NonNull
    public List<FileEntry> getRecentFiles() {
        return filesInUri(uriForRecent());
    }

    @NonNull
    public List<FileEntry> getForSearch(@NonNull String query) {
        return filesInUri(uriForSearch(query));
    }

    @NonNull
    public Uri uriFor(@NonNull FileEntry fileEntry) {
        return DocumentsContract.buildDocumentUri(HomeEnvironment.AUTHORITY, fileEntry.getId());
    }

    @NonNull
    public Optional<FileEntry> parentOf(@NonNull String id) {
        if (HomeEnvironment.ROOT.equals(id)) {
            return Optional.empty();
        } else {
            final int lastDivider = id.lastIndexOf('/');
            if (lastDivider < 1) {
                return Optional.empty();
            } else {
                return Optional.of(new FileEntry(id.substring(0, lastDivider),
                        "..",
                        0,
                        0,
                        FileEntry.MIME_DIR,
                        0));
            }
        }
    }

    @NonNull
    private List<FileEntry> filesInUri(@NonNull Uri uri) {
        final List<FileEntry> list = new ArrayList<>();
        try (final Cursor cursor = cr.query(uri,
                DEFAULT_PROJECTION,
                null,
                null,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(new FileEntry(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getInt(2),
                            cursor.getLong(3),
                            cursor.getString(4),
                            cursor.getLong(5)));
                } while (cursor.moveToNext());
            }
        }
        return list;
    }

    @NonNull
    private Uri uriForChildrenOf(@NonNull String id) {
        return DocumentsContract.buildChildDocumentsUri(HomeEnvironment.AUTHORITY, id);
    }

    @NonNull
    private Uri uriForSearch(@NonNull String query) {
        return DocumentsContract.buildSearchDocumentsUri(HomeEnvironment.AUTHORITY,
                HomeEnvironment.ROOT,
                query);
    }

    @NonNull
    private Uri uriForRecent() {
        return DocumentsContract.buildRecentDocumentsUri(HomeEnvironment.AUTHORITY,
                HomeEnvironment.ROOT);
    }
}
