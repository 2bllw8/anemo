/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents;

import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

final class DocumentUtils {
    private static final String MIME_TYPE_GENERIC = "application/octet-stream";

    private DocumentUtils() {
    }

    @NonNull
    public static List<File> getLastModifiedFiles(@NonNull File parent,
                                                  int atMost) {
        if (!parent.isDirectory()) {
            return Collections.emptyList();
        }

        final Queue<File> lastModifiedFiles = new PriorityQueue<>(atMost,
                (i, j) -> Long.compare(i.lastModified(), j.lastModified()));

        final Queue<File> toVisit = new ArrayDeque<>();
        toVisit.add(parent);
        while (!toVisit.isEmpty()) {
            final File visiting = toVisit.remove();
            if (visiting.isDirectory()) {
                final File[] children = visiting.listFiles();
                if (children != null) {
                    Collections.addAll(toVisit, children);
                }
            } else {
                lastModifiedFiles.add(visiting);
            }
        }

        final List<File> list = new ArrayList<>(atMost);
        int numAdded = 0;
        while (!lastModifiedFiles.isEmpty() && numAdded < atMost) {
            final File file = lastModifiedFiles.remove();
            list.add(numAdded++, file);
        }
        return list;
    }

    @NonNull
    public static List<File> queryFiles(@NonNull File parent,
                                        @NonNull String query,
                                        int atMost) {
        if (!parent.isDirectory()) {
            return Collections.emptyList();
        }

        final Queue<File> toVisit = new ArrayDeque<>();
        toVisit.add(parent);

        final List<File> list = new ArrayList<>(atMost);
        int numAdded = 0;
        while (!toVisit.isEmpty() && numAdded < atMost) {
            final File visiting = toVisit.remove();
            if (visiting.isDirectory()) {
                final File[] children = visiting.listFiles();
                if (children != null) {
                    Collections.addAll(toVisit, children);
                }
            } else {
                if (visiting.getName().toLowerCase().contains(query)) {
                    list.add(numAdded++, visiting);
                }
            }
        }
        return list;
    }

    @NonNull
    public static String getTypeForFile(File file) {
        return file.isDirectory()
                ? DocumentsContract.Document.MIME_TYPE_DIR
                : getTypeForName(file.getName());
    }

    @NonNull
    public static String getTypeForName(@NonNull String name) {
        final int idxDot = name.lastIndexOf('.');
        if (idxDot < 0) {
            return MIME_TYPE_GENERIC;
        } else {
            final String extension = name.substring(idxDot + 1);
            final String mime = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension);
            return mime == null
                    ? MIME_TYPE_GENERIC
                    : mime;
        }
    }

    @NonNull
    public static String getChildMimeTypes(@NonNull File parent) {
        final Set<String> mimeTypes = new HashSet<>();
        final File[] children = parent.listFiles();
        if (children != null) {
            for (final File file : children) {
                mimeTypes.add(getTypeForFile(file));
            }
        }

        return String.join("\n", mimeTypes);
    }
}
