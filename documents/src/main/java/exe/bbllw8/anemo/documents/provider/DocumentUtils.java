/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.provider;

import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

final class DocumentUtils {
    private static final String MIME_TYPE_GENERIC = "application/octet-stream";

    private DocumentUtils() {
    }

    @NonNull
    public static List<Path> getLastModifiedFiles(@NonNull Path parent,
                                                  int atMost) {
        if (!Files.isDirectory(parent)) {
            return Collections.emptyList();
        }

        final Queue<Path> lastModifiedFiles = new PriorityQueue<>((a, b) -> {
            try {
                final long timeA = Files.getLastModifiedTime(a).toMillis();
                final long timeB = Files.getLastModifiedTime(b).toMillis();
                return Long.compare(timeA, timeB);
            } catch (IOException e) {
                return 1;
            }
        });

        try {
            Files.walkFileTree(parent, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    lastModifiedFiles.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // Terminate search early
        }

        final List<Path> list = new ArrayList<>(atMost);
        int numAdded = 0;
        while (!lastModifiedFiles.isEmpty() && numAdded < atMost) {
            final Path file = lastModifiedFiles.remove();
            list.add(numAdded++, file);
        }
        return list;
    }

    @NonNull
    public static List<Path> queryFiles(@NonNull Path parent,
                                        @NonNull String query,
                                        int atMost) {
        if (!Files.isDirectory(parent)) {
            return Collections.emptyList();
        }

        final List<Path> list = new ArrayList<>();

        try {
            Files.walkFileTree(parent, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                                                         BasicFileAttributes attrs) {
                    if (query.contains(dir.getFileName().toString())) {
                        list.add(dir);
                    }
                    return list.size() < atMost
                            ? FileVisitResult.CONTINUE
                            : FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (query.contains(file.getFileName().toString())) {
                        list.add(file);
                    }
                    return list.size() < atMost
                            ? FileVisitResult.CONTINUE
                            : FileVisitResult.TERMINATE;
                }
            });
        } catch (IOException ignored) {
            // Terminate search early
        }
        return list;
    }

    @NonNull
    public static String getTypeForPath(Path path) {
        return Files.isDirectory(path)
                ? DocumentsContract.Document.MIME_TYPE_DIR
                : getTypeForName(path.getFileName().toString());
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
    public static String getChildMimeTypes(@NonNull Path parent) {
        try {
            final Set<String> mimeTypes = Files.list(parent)
                    .filter(Objects::nonNull)
                    .map(DocumentUtils::getTypeForPath)
                    .collect(Collectors.toSet());
            return String.join("\n", mimeTypes);
        } catch (IOException e) {
            return "";
        }
    }
}
