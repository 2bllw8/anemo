/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.provider;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.system.Int64Ref;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;

import java.io.FileNotFoundException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import exe.bbllw8.anemo.documents.home.HomeEnvironment;
import exe.bbllw8.either.Failure;
import exe.bbllw8.either.Success;
import exe.bbllw8.either.Try;

public final class DocumentOperations {
    private static final String MIME_TYPE_GENERIC = "application/octet-stream";

    private static final String[] ROOT_PROJECTION = {DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON, DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_TITLE,};
    private static final String[] DOCUMENT_PROJECTION = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE,};

    private final HomeEnvironment homeEnvironment;

    public DocumentOperations(HomeEnvironment homeEnvironment) {
        this.homeEnvironment = homeEnvironment;
    }

    public Cursor queryRoot(String title, String summary, @DrawableRes int icon) {
        final MatrixCursor result = new MatrixCursor(ROOT_PROJECTION);
        final Path baseDir = homeEnvironment.getBaseDir();

        final int flags = DocumentsContract.Root.FLAG_LOCAL_ONLY
                | DocumentsContract.Root.FLAG_SUPPORTS_CREATE
                | DocumentsContract.Root.FLAG_SUPPORTS_RECENTS
                | DocumentsContract.Root.FLAG_SUPPORTS_SEARCH
                | DocumentsContract.Root.FLAG_SUPPORTS_EJECT;

        result.newRow()
                .add(DocumentsContract.Root.COLUMN_ROOT_ID, HomeEnvironment.ROOT)
                .add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, getDocIdForPath(baseDir))
                .add(DocumentsContract.Root.COLUMN_FLAGS, flags)
                .add(DocumentsContract.Root.COLUMN_MIME_TYPES, getChildMimeTypes(baseDir))
                .add(DocumentsContract.Root.COLUMN_ICON, icon)
                .add(DocumentsContract.Root.COLUMN_TITLE, title)
                .add(DocumentsContract.Root.COLUMN_SUMMARY, summary);
        return result;
    }

    public Try<Cursor> queryDocument(String documentId) {
        final MatrixCursor result = new MatrixCursor(DOCUMENT_PROJECTION);
        return getPathForId(documentId).flatMap(path -> buildEntry(path, documentId)).map(entry -> {
            addToCursor(result, entry);
            return result;
        });
    }

    public Try<Cursor> queryChildDocuments(String parentDocumentId) {
        final MatrixCursor result = new MatrixCursor(DOCUMENT_PROJECTION);
        return getPathForId(parentDocumentId).map(Files::list)
                .map(children -> children.flatMap(path -> buildEntry(path).stream()))
                .map(entries -> {
                    entries.forEach(entry -> addToCursor(result, entry));
                    return result;
                });
    }

    public Try<Cursor> queryRecentDocuments(String rootId, int atMost) {
        final MatrixCursor result = new MatrixCursor(DOCUMENT_PROJECTION);
        return getPathForId(rootId).filter(Files::isDirectory).map(root -> {
            final Queue<Path> lastModifiedFiles = new PriorityQueue<>((a, b) -> Try.from(() -> {
                final long timeA = Files.getLastModifiedTime(a).toMillis();
                final long timeB = Files.getLastModifiedTime(b).toMillis();
                return Long.compare(timeA, timeB);
            }).getOrElse(1));

            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    lastModifiedFiles.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });

            lastModifiedFiles.stream()
                    .limit(atMost)
                    .flatMap(path -> buildEntry(path).stream())
                    .forEachOrdered(entry -> addToCursor(result, entry));
            return result;
        });
    }

    public Try<Cursor> querySearchDocuments(String parentDocumentId, String query, int atMost) {
        final MatrixCursor result = new MatrixCursor(DOCUMENT_PROJECTION);
        return getPathForId(parentDocumentId).filter(Files::isDirectory)
                .filter(Files::isReadable)
                .map(parent -> {
                    final List<Path> list = new ArrayList<>(atMost / 2);

                    Files.walkFileTree(parent, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (query.contains(file.getFileName().toString())) {
                                list.add(file);
                            }
                            return list.size() < atMost
                                    ? FileVisitResult.CONTINUE
                                    : FileVisitResult.TERMINATE;
                        }

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
                    });

                    list.stream()
                            .flatMap(path -> buildEntry(path).stream())
                            .forEach(entry -> addToCursor(result, entry));
                    return result;
                });
    }

    public Try<ParcelFileDescriptor> openDocument(String documentId, String mode) {
        return getPathForId(documentId).map(path -> ParcelFileDescriptor.open(path.toFile(),
                ParcelFileDescriptor.parseMode(mode)));
    }

    public Try<AssetFileDescriptor> openDocumentThumbnail(String documentId) {
        return getPathForId(documentId)
                .map(path -> ParcelFileDescriptor.open(path.toFile(),
                        ParcelFileDescriptor.MODE_READ_ONLY))
                .map(pfd -> new AssetFileDescriptor(pfd, 0, pfd.getStatSize()));
    }

    public Try<String> createDocument(String parentDocumentId, String mimeType,
            String displayName) {
        return getPathForId(parentDocumentId).map(parent -> parent.resolve(displayName))
                .map(target -> {
                    if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                        Files.createDirectory(target);
                    } else {
                        Files.createFile(target);
                    }
                    Files.setPosixFilePermissions(target, HomeEnvironment.ATTR_DEFAULT_POSIX);
                    return getDocIdForPath(target);
                });
    }

    /**
     * @return The id of the parent of the deleted document for content change notification.
     */
    public Try<String> deleteDocument(String documentId) {
        return getPathForId(documentId).map(path -> {
            final String parentId = getDocIdForPath(path.getParent());
            Files.delete(path);
            return parentId;
        });
    }

    public Try<String> copyDocument(String sourceDocumentId, String targetDocumentId) {
        return getPathForId(sourceDocumentId)
                .map(source -> new Pair<>(source, getPathForId(targetDocumentId)))
                .filter(pair -> pair.second.isSuccess())
                .map(pair -> new Pair<>(pair.first, pair.second.get()))
                .map(pair -> {
                    final Path source = pair.first;
                    final Path targetParent = pair.second;
                    final Path target = targetParent.resolve(source.getFileName().toString());

                    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                    return getDocIdForPath(target);
                });
    }

    public Try<String> moveDocument(String sourceDocumentId, String targetParentDocumentId) {
        return getPathForId(sourceDocumentId)
                .map(source -> new Pair<>(source, getPathForId(targetParentDocumentId)))
                .filter(pair -> pair.second.isSuccess())
                .map(pair -> new Pair<>(pair.first, pair.second.get()))
                .map(pair -> {
                    final Path source = pair.first;
                    final Path targetParent = pair.second;
                    final Path target = targetParent.resolve(source.getFileName().toString());

                    Files.move(source, target, StandardCopyOption.COPY_ATTRIBUTES,
                            StandardCopyOption.REPLACE_EXISTING);

                    return getDocIdForPath(target);
                });
    }

    /**
     * @return A pair of ids: the first is the parent for content change notification and the second
     *         is the id of the renamed document.
     */
    public Try<Pair<String, String>> renameDocument(String documentId, String displayName) {
        return getPathForId(documentId).map(source -> {
            final Path parent = source.getParent();
            final Path target = parent.resolve(displayName);

            Files.move(source, target);

            return new Pair<>(getDocIdForPath(parent), getDocIdForPath(target));
        });
    }

    public Try<String> getDocumentType(String documentId) {
        return getPathForId(documentId).map(this::getTypeForPath);
    }

    @RequiresApi(29)
    public Try<Pair<Long, Long>> getSizeAndCount(String documentId) {
        return getPathForId(documentId).filter(Files::exists)
                .filter(Files::isReadable)
                .map(path -> {
                    if (Files.isDirectory(path)) {
                        final Int64Ref treeSize = new Int64Ref(0);
                        final Int64Ref treeCount = new Int64Ref(0);

                        Files.walkFileTree(path, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                treeSize.value += attrs.size();
                                treeCount.value += 1;
                                return FileVisitResult.CONTINUE;
                            }
                        });

                        return new Pair<>(treeSize.value, treeCount.value);
                    } else {
                        return new Pair<>(Files.size(path), 1L);
                    }
                });
    }

    public boolean isRoot(String documentId) {
        return HomeEnvironment.ROOT.equals(documentId);
    }

    /*
     * CursorEntry
     */

    private Try<CursorEntry> buildEntry(Path path) {
        return buildEntry(path, getDocIdForPath(path));
    }

    private Try<CursorEntry> buildEntry(Path path, String documentId) {
        return Try.from(() -> {
            final String fileName = path.getFileName().toString();
            final String mimeType = getTypeForPath(path);
            final long lastModifiedTime = Files.getLastModifiedTime(path).toMillis();
            final long fileSize = Files.size(path);

            final boolean isWritable = Files.isWritable(path);
            int flags = 0;
            if (Files.isDirectory(path)) {
                if (isWritable) {
                    flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
                }

                if (!homeEnvironment.isDefaultDirectory(path)) {
                    flags |= Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_MOVE
                            | Document.FLAG_SUPPORTS_RENAME;
                }
            } else {
                if (isWritable) {
                    flags |= Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_MOVE
                            | Document.FLAG_SUPPORTS_RENAME | Document.FLAG_SUPPORTS_WRITE;
                }

                if (mimeType.startsWith("image/")) {
                    flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
                }
            }

            return new CursorEntry(documentId, fileName, mimeType, flags, lastModifiedTime,
                    fileSize);
        });
    }

    private void addToCursor(MatrixCursor cursor, CursorEntry entry) {
        cursor.newRow()
                .add(Document.COLUMN_DOCUMENT_ID, entry.getId())
                .add(Document.COLUMN_DISPLAY_NAME, entry.getDisplayName())
                .add(Document.COLUMN_MIME_TYPE, entry.getMimeType())
                .add(Document.COLUMN_FLAGS, entry.getFlags())
                .add(Document.COLUMN_LAST_MODIFIED, entry.getLastModified())
                .add(Document.COLUMN_SIZE, entry.getSize());
    }

    /*
     * Path-Id
     */

    private String getDocIdForPath(Path path) {
        final Path rootPath = homeEnvironment.getBaseDir();
        if (rootPath.equals(path)) {
            return HomeEnvironment.ROOT + ':';
        } else {
            return HomeEnvironment.ROOT + ':'
                    + path.toString().replaceFirst(rootPath.toString(), "");
        }
    }

    private Try<Path> getPathForId(String documentId) {
        final Path baseDir = homeEnvironment.getBaseDir();
        if (isRoot(documentId)) {
            return new Success<>(baseDir);
        } else {
            final int splitIndex = documentId.indexOf(':', 1);
            if (splitIndex < 0) {
                return new Failure<>(new FileNotFoundException("No root for " + documentId));
            } else {
                final String targetPath = documentId.substring(splitIndex + 1);
                final Path target = Paths.get(baseDir.toString(), targetPath);
                if (Files.exists(target)) {
                    return new Success<>(target);
                } else {
                    return new Failure<>(new FileNotFoundException(
                            "No path for " + documentId + " at " + target));
                }
            }
        }
    }

    /*
     * MimeType
     */

    private String getChildMimeTypes(Path parent) {
        return Try.from(() -> Files.list(parent))
                .map(files -> files.map(this::getTypeForPath))
                .map(Stream::distinct)
                .map(paths -> paths.collect(Collectors.joining("\n")))
                .getOrElse("");
    }

    private String getTypeForPath(Path path) {
        if (Files.isDirectory(path)) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        } else {
            return getTypeForName(path.getFileName().toString());
        }
    }

    private String getTypeForName(String name) {
        final int idxDot = name.lastIndexOf('.');
        if (idxDot < 0) {
            return MIME_TYPE_GENERIC;
        } else {
            final String extension = name.substring(idxDot + 1);
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            return mime == null ? MIME_TYPE_GENERIC : mime;
        }
    }
}
