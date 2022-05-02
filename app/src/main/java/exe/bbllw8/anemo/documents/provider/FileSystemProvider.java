/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.provider;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;
import android.system.Int64Ref;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import exe.bbllw8.anemo.tuple.Tuple2;
import exe.bbllw8.anemo.tuple.Tuple3;
import exe.bbllw8.either.Try;

/**
 * A helper class for {@link android.provider.DocumentsProvider} to perform file operations on local
 * files.
 * <p>
 * Based on {@code com.android.internal.content.FileSystemProvider}.
 */
@SuppressLint("ExifInterface")
@SuppressWarnings("UnusedReturnValue")
public abstract class FileSystemProvider extends DocumentsProvider {
    private static final String TAG = "FileSystemProvider";
    private static final int MAX_QUERY_RESULTS = 23;
    private static final String[] DEFAULT_PROJECTION = {Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS, Document.COLUMN_SIZE,};

    private final ArrayMap<Path, DirectoryObserver> observers = new ArrayMap<>();

    private Handler handler;
    protected ContentResolver cr;

    @Override
    public boolean onCreate() {
        handler = new Handler(Looper.myLooper());
        cr = getContext().getContentResolver();
        return true;
    }

    @Nullable
    @Override
    public Bundle getDocumentMetadata(@NonNull String documentId) {
        return getPathForId(documentId).filter(Files::exists)
                .filter(Files::isReadable)
                .map(path -> {
                    if (Build.VERSION.SDK_INT >= 29 && Files.isDirectory(path)) {
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

                        final Bundle bundle = new Bundle();
                        bundle.putLong(DocumentsContract.METADATA_TREE_SIZE, treeSize.value);
                        bundle.putLong(DocumentsContract.METADATA_TREE_COUNT, treeCount.value);
                        return bundle;
                    } else {
                        return null;
                    }
                })
                .getOrElse(null);
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) {
        final String docName = PathUtils.buildValidFilename(displayName);
        final Try<String> result = getPathForId(parentDocumentId).filter(Files::isDirectory)
                .map(parent -> {
                    final Path path = PathUtils.buildUniquePath(parent, mimeType, docName);
                    if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                        Files.createDirectory(path);
                    } else {
                        Files.createFile(path);
                    }
                    final String childId = getDocIdForPath(path);
                    onDocIdChanged(childId);
                    updateMediaStore(getContext(), path);
                    return childId;
                });
        if (result.isSuccess()) {
            return result.get();
        } else {
            Log.e(TAG, "Failed to create document", result.failed().get());
            throw new IllegalStateException();
        }
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId)
            throws FileNotFoundException {
        final Try<String> result = getPathForId(sourceDocumentId)
                .flatMap(source -> getPathForId(targetParentDocumentId)
                        .map(parent -> new Tuple2<>(source, parent)))
                .map(tup -> {
                    final Path source = tup._1;
                    final String fileName = source.getFileName().toString();
                    final Path target = PathUtils.buildUniquePath(tup._2.getParent(), fileName);
                    return new Tuple2<>(tup._1, target);
                })
                .map(tup -> {
                    Files.copy(tup._1, tup._2);
                    return tup._2;
                })
                .map(target -> {
                    updateMediaStore(getContext(), target);
                    return getDocIdForPath(target);
                })
                .map(docId -> {
                    onDocIdChanged(docId);
                    return docId;
                });
        if (result.isSuccess()) {
            return result.get();
        } else {
            Log.e(TAG, "Failed to copy document", result.failed().get());
            throw new IllegalStateException();
        }
    }

    @Override
    public String renameDocument(String documentId, String displayName) {
        final String docName = PathUtils.buildValidFilename(displayName);
        final Try<String> result = getPathForId(documentId).map(before -> {
            final Path after = PathUtils.buildUniquePath(before.getParent(), docName);
            // before, beforeVisible, after
            return new Tuple2<>(before, after);
        }).map(tup -> {
            Files.move(tup._1, tup._2);
            return tup;
        }).map(tup -> new Tuple3<>(tup._1, tup._2, getDocIdForPath(tup._2))).map(tup -> {
            final Context context = getContext();
            updateMediaStore(context, tup._1);
            updateMediaStore(context, tup._2);
            onDocIdChanged(documentId);
            onDocIdDeleted(documentId);
            onDocIdChanged(tup._3);
            return tup._3;
        }).map(afterId -> {
            if (TextUtils.equals(documentId, afterId)) {
                // Null is used when the source and destination are equal
                return null;
            } else {
                return afterId;
            }
        });
        if (result.isSuccess()) {
            return result.get();
        } else {
            Log.e(TAG, "Failed to rename document", result.failed().get());
            throw new IllegalStateException();
        }
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId,
            String targetParentDocumentId) {
        final Try<String> result = getPathForId(sourceDocumentId)
                .flatMap(before -> getPathForId(targetParentDocumentId)
                        .flatMap(parent -> getPathForId(before.getFileName().toString()))
                        .map(after -> new Tuple2<>(before, after)))
                .map(tup -> {
                    Files.move(tup._1, tup._2);
                    return tup;
                })
                .map(tup -> new Tuple3<>(tup, getDocIdForPath(tup._2)))
                .map(tup -> {
                    final Context context = getContext();
                    updateMediaStore(context, tup._1);
                    updateMediaStore(context, tup._2);
                    onDocIdChanged(sourceDocumentId);
                    onDocIdDeleted(sourceDocumentId);
                    onDocIdChanged(tup._3);
                    return tup._3;
                });
        if (result.isSuccess()) {
            return result.get();
        } else {
            Log.e(TAG, "Failed to move document", result.failed().get());
            throw new IllegalStateException();
        }
    }

    @Override
    public void deleteDocument(String documentId) {
        getPathForId(documentId).map(path -> {
            if (Files.isDirectory(path)) {
                PathUtils.deleteContents(path);
            } else {
                Files.deleteIfExists(path);
            }
            return path;
        }).forEach(path -> {
            onDocIdChanged(documentId);
            onDocIdDeleted(documentId);
            updateMediaStore(getContext(), path);
        });
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode,
            CancellationSignal signal) throws FileNotFoundException {
        final Try<ParcelFileDescriptor> result = getPathForId(documentId).map(path -> {
            final int pfdMode = ParcelFileDescriptor.parseMode(mode);
            if (pfdMode == ParcelFileDescriptor.MODE_READ_ONLY) {
                return ParcelFileDescriptor.open(path.toFile(), pfdMode);
            } else {
                // When finished writing, kick off media scanner
                return ParcelFileDescriptor.open(path.toFile(), pfdMode, handler, failure -> {
                    onDocIdChanged(documentId);
                    updateMediaStore(getContext(), path);
                });
            }
        });
        if (result.isFailure()) {
            Log.e(TAG, "Failed to open document " + documentId, result.failed().get());
            throw new FileNotFoundException("Couldn't open " + documentId);
        }
        return result.get();
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveProjection(projection));
        includePath(result, documentId);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection,
            String sortOrder) throws FileNotFoundException {
        final Try<Path> parentTry = getPathForId(parentDocumentId);
        if (parentTry.isFailure()) {
            throw new FileNotFoundException("Couldn't find " + parentDocumentId);
        }
        final Path parent = parentTry.get();
        final MatrixCursor result = new DirectoryCursor(resolveProjection(projection),
                parentDocumentId, parent);
        if (Files.isDirectory(parent)) {
            Try.from(() -> {
                Files.list(parent).forEach(file -> includePath(result, file));
                return null;
            });
        } else {
            Log.w(TAG, parentDocumentId + ": not a directory");
        }
        return result;
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        final Try<Path> pathTry = getPathForId(documentId);
        if (pathTry.isFailure()) {
            throw new FileNotFoundException("Can't find " + documentId);
        }
        return PathUtils.getDocumentType(documentId, pathTry.get());
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String docId, Point sizeHint,
            CancellationSignal signal) throws FileNotFoundException {
        final Try<AssetFileDescriptor> pathTry = getPathForId(docId).map(path -> {
            final ParcelFileDescriptor pfd = ParcelFileDescriptor.open(path.toFile(),
                    ParcelFileDescriptor.MODE_READ_ONLY);
            final ExifInterface exif = new ExifInterface(path.toFile().getAbsolutePath());

            final long[] thumb = exif.getThumbnailRange();
            if (thumb == null) {
                // Do full file decoding, we don't need to handle the orientation
                return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH, null);
            } else {
                // If we use thumb to decode, we need to handle the rotation by ourselves.
                Bundle extras = null;
                switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)) {
                    case ExifInterface.ORIENTATION_ROTATE_90 :
                        extras = new Bundle(1);
                        extras.putInt(DocumentsContract.EXTRA_ORIENTATION, 90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180 :
                        extras = new Bundle(1);
                        extras.putInt(DocumentsContract.EXTRA_ORIENTATION, 180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270 :
                        extras = new Bundle(1);
                        extras.putInt(DocumentsContract.EXTRA_ORIENTATION, 270);
                        break;
                }

                return new AssetFileDescriptor(pfd, thumb[0], thumb[1], extras);
            }
        });
        if (pathTry.isFailure()) {
            throw new FileNotFoundException("Couldn't open " + docId);
        }
        return pathTry.get();
    }

    @Override
    @SuppressLint("NewApi")
    public Cursor querySearchDocuments(String rootId, String[] projection, Bundle queryArgs)
            throws FileNotFoundException {
        final Try<Cursor> result = getPathForId(rootId).filter($ -> Build.VERSION.SDK_INT > 29)
                .map(path -> querySearchDocuments(path, projection, queryArgs));
        if (result.isFailure()) {
            throw new FileNotFoundException();
        }
        return result.get();
    }

    @RequiresApi(29)
    protected final Cursor querySearchDocuments(Path parent, String[] projection,
            Bundle queryArgs) {
        final MatrixCursor result = new MatrixCursor(resolveProjection(projection));
        final AtomicInteger count = new AtomicInteger(MAX_QUERY_RESULTS);
        Try.from(() -> Files.walkFileTree(parent, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (matchSearchQueryArguments(file, queryArgs)) {
                    includePath(result, file);
                }
                return count.decrementAndGet() == 0
                        ? FileVisitResult.TERMINATE
                        : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                if (matchSearchQueryArguments(dir, queryArgs)) {
                    includePath(result, dir);
                }
                return count.decrementAndGet() == 0
                        ? FileVisitResult.TERMINATE
                        : FileVisitResult.CONTINUE;
            }
        }));
        return result;
    }

    /**
     * Callback indicating that the given document has been modified. This gives the provider a hook
     * to invalidate cached data, such as {@code sdcardfs}.
     */
    protected abstract void onDocIdChanged(String docId);

    /**
     * Callback indicating that the given document has been deleted or moved. This gives the
     * provider a hook to revoke the uri permissions.
     */
    protected abstract void onDocIdDeleted(String docId);

    @RequiresApi(30)
    protected boolean shouldBlockFromTree(String docId) {
        return false;
    }

    protected boolean isNotEssential(Path path) {
        return true;
    }

    protected MatrixCursor.RowBuilder includePath(MatrixCursor result, String docId)
            throws FileNotFoundException {
        final Try<Path> pathTry = getPathForId(docId);
        if (pathTry.isFailure()) {
            throw new FileNotFoundException("Couldn't find " + docId);
        }
        return includePath(result, pathTry.get(), docId);
    }

    protected MatrixCursor.RowBuilder includePath(MatrixCursor result, Path path) {
        return includePath(result, path, getDocIdForPath(path));
    }

    protected MatrixCursor.RowBuilder includePath(MatrixCursor result, Path path, String docId) {
        final String[] columns = result.getColumnNames();
        final MatrixCursor.RowBuilder row = result.newRow();

        final String mimeType = PathUtils.getDocumentType(docId, path);
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_MIME_TYPE, mimeType);

        final int flagIndex = indexOf(columns, Document.COLUMN_FLAGS);
        if (flagIndex != -1) {
            int flags = 0;
            if (Files.isWritable(path)) {
                if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                    flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
                    if (isNotEssential(path)) {
                        flags |= Document.FLAG_SUPPORTS_DELETE;
                        flags |= Document.FLAG_SUPPORTS_RENAME;
                        flags |= Document.FLAG_SUPPORTS_MOVE;
                    }
                    if (Build.VERSION.SDK_INT >= 30 && shouldBlockFromTree(docId)) {
                        flags |= Document.FLAG_DIR_BLOCKS_OPEN_DOCUMENT_TREE;
                    }
                } else {
                    flags |= Document.FLAG_SUPPORTS_WRITE;
                    flags |= Document.FLAG_SUPPORTS_DELETE;
                    flags |= Document.FLAG_SUPPORTS_RENAME;
                    flags |= Document.FLAG_SUPPORTS_MOVE;
                }
            }

            if (mimeType.startsWith("image/")) {
                flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
            }
            row.add(Document.COLUMN_FLAGS, flags);
        }

        final int displayNameIndex = indexOf(columns, Document.COLUMN_DISPLAY_NAME);
        if (displayNameIndex != -1) {
            row.add(Document.COLUMN_DISPLAY_NAME, path.getFileName().toString());
        }

        final int lastModifiedIndex = indexOf(columns, Document.COLUMN_LAST_MODIFIED);
        if (lastModifiedIndex != -1) {
            Try.from(() -> Files.getLastModifiedTime(path))
                    .map(FileTime::toMillis)
                    // Only publish dates reasonably after epoch
                    .filter(lastModified -> lastModified > 31536000000L)
                    .forEach(lastModified -> row.add(Document.COLUMN_LAST_MODIFIED, lastModified));
        }

        final int sizeIndex = indexOf(columns, Document.COLUMN_SIZE);
        if (sizeIndex != -1) {
            Try.from(() -> Files.size(path)).forEach(size -> row.add(Document.COLUMN_SIZE, size));
        }

        // Return the row builder just in case any subclass want to add more stuff to it.
        return row;
    }

    protected abstract Try<Path> getPathForId(String docId);

    protected abstract String getDocIdForPath(Path path);

    protected abstract Uri buildNotificationUri(String docId);

    private String[] resolveProjection(String[] projection) {
        return projection == null ? DEFAULT_PROJECTION : projection;
    }

    private void startObserving(Path path, Uri notifyUri, DirectoryCursor cursor) {
        synchronized (observers) {
            DirectoryObserver observer = observers.get(path);
            if (observer == null) {
                observer = new DirectoryObserver(path, cr, notifyUri);
                observer.startWatching();
                observers.put(path, observer);
            }
            observer.cursors.add(cursor);
        }
    }

    private void stopObserving(Path path, DirectoryCursor cursor) {
        synchronized (observers) {
            final DirectoryObserver observer = observers.get(path);
            if (observer == null) {
                return;
            }

            observer.cursors.remove(cursor);
            if (observer.cursors.isEmpty()) {
                observers.remove(path);
                observer.stopWatching();
            }
        }
    }

    /**
     * Test if the file matches the query arguments.
     *
     * @param path
     *            the file to test
     * @param queryArgs
     *            the query arguments
     */
    @RequiresApi(29)
    private boolean matchSearchQueryArguments(Path path, Bundle queryArgs) throws IOException {
        if (queryArgs == null) {
            return true;
        }

        final String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        final String argDisplayName = queryArgs.getString(DocumentsContract.QUERY_ARG_DISPLAY_NAME,
                "");
        if (!argDisplayName.isEmpty()) {
            if (!fileName.contains(argDisplayName.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        final long argFileSize = queryArgs.getLong(DocumentsContract.QUERY_ARG_FILE_SIZE_OVER, -1);
        if (argFileSize != -1 && Files.size(path) < argFileSize) {
            return false;
        }

        final long argLastModified = queryArgs
                .getLong(DocumentsContract.QUERY_ARG_LAST_MODIFIED_AFTER, -1);
        if (argLastModified != -1 && Files.getLastModifiedTime(path).toMillis() < argLastModified) {
            return false;
        }

        final String[] argMimeTypes = queryArgs
                .getStringArray(DocumentsContract.QUERY_ARG_MIME_TYPES);
        if (argMimeTypes != null && argMimeTypes.length > 0) {
            final String fileMimeType;
            if (Files.isDirectory(path)) {
                fileMimeType = Document.MIME_TYPE_DIR;
            } else {
                int dotPos = fileName.lastIndexOf('.');
                if (dotPos < 0) {
                    return false;
                }
                final String extension = fileName.substring(dotPos + 1);
                fileMimeType = Intent.normalizeMimeType(
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
            }

            for (final String type : argMimeTypes) {
                if (mimeTypeMatches(fileMimeType, type)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private static void updateMediaStore(Context context, Path path) {
        final Intent intent;
        if (!Files.isDirectory(path) && path.getFileName().toString().endsWith("nomedia")) {
            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(path.getParent().toFile()));
        } else {
            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(path.toFile()));
        }
        context.sendBroadcast(intent);
    }

    public static boolean mimeTypeMatches(String mimeType, String filter) {
        if (mimeType == null) {
            return false;
        }

        final String[] mimeTypeParts = mimeType.split("/");
        final String[] filterParts = filter.split("/");

        if (filterParts.length != 2) {
            throw new IllegalArgumentException(
                    "Ill-formatted MIME type filter. Must be type/subtype");
        }
        if (filterParts[0].isEmpty() || filterParts[1].isEmpty()) {
            throw new IllegalArgumentException(
                    "Ill-formatted MIME type filter. Type or subtype empty");
        }
        if (mimeTypeParts.length != 2) {
            return false;
        }
        if (!"*".equals(filterParts[0]) && !filterParts[0].equals(mimeTypeParts[0])) {
            return false;
        }
        return "*".equals(filterParts[1]) || filterParts[1].equals(mimeTypeParts[1]);
    }

    private static <T> int indexOf(T[] array, T target) {
        if (array == null) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (Objects.equals(array[i], target)) {
                return i;
            }
        }
        return -1;
    }

    private class DirectoryCursor extends MatrixCursor {
        private final Path path;

        public DirectoryCursor(String[] columnNames, String docId, Path path) {
            super(columnNames);
            this.path = path;

            final Uri notifyUri = buildNotificationUri(docId);
            setNotificationUri(cr, notifyUri);
            startObserving(path, notifyUri, this);
        }

        public void notifyChanged() {
            onChange(false);
        }

        @Override
        public void close() {
            super.close();
            stopObserving(path, this);
        }
    }

    private static class DirectoryObserver extends FileObserver {
        private static final int NOTIFY_EVENTS = ATTRIB | CLOSE_WRITE | MOVED_FROM | MOVED_TO
                | CREATE | DELETE | DELETE_SELF | MOVE_SELF;

        private final ContentResolver resolver;
        private final Uri notifyUri;
        private final CopyOnWriteArrayList<DirectoryCursor> cursors;

        public DirectoryObserver(Path path, ContentResolver resolver, Uri notifyUri) {
            super(path.toFile().getAbsolutePath(), NOTIFY_EVENTS);
            this.resolver = resolver;
            this.notifyUri = notifyUri;
            this.cursors = new CopyOnWriteArrayList<>();
        }

        @Override
        public void onEvent(int event, @Nullable String path) {
            if ((event & NOTIFY_EVENTS) != 0) {
                for (final DirectoryCursor cursor : cursors) {
                    cursor.notifyChanged();
                }
                resolver.notifyChange(notifyUri, null, false);
            }
        }
    }
}
