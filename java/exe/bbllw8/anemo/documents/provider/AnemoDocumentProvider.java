/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.system.Int64Ref;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import exe.bbllw8.anemo.documents.home.HomeEnvironment;
import exe.bbllw8.anemo.documents.lock.LockStore;

public final class AnemoDocumentProvider extends DocumentsProvider {
    private static final String TAG = "AnemoDocumentProvider";

    private static final String[] DEFAULT_ROOT_PROJECTION = {
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_TITLE,
    };
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_FLAGS,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
    };

    private static final int MAX_SEARCH_RESULTS = 20;
    private static final int MAX_LAST_MODIFIED = 5;

    private ContentResolver cr;
    private HomeEnvironment homeEnvironment;

    private LockStore lockStore;
    private int lockStoreListenerToken = LockStore.NULL_LISTENER_ID;

    private boolean showInfo = true;

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        cr = context.getContentResolver();

        try {
            homeEnvironment = HomeEnvironment.getInstance(context);
            lockStore = LockStore.getInstance(context);

            lockStoreListenerToken = lockStore.addListener(this::onLockChanged);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to setup", e);
            return false;
        }
    }

    @Override
    public void shutdown() {
        if (lockStoreListenerToken != LockStore.NULL_LISTENER_ID) {
            lockStore.removeListener(lockStoreListenerToken);
            lockStoreListenerToken = LockStore.NULL_LISTENER_ID;
        }

        super.shutdown();
    }

    /* Query */

    @NonNull
    @Override
    public Cursor queryRoots(@NonNull String[] projection) {
        final Context context = getContext();
        final MatrixCursor result = new MatrixCursor(rootProjection(projection));
        if (lockStore.isLocked()) {
            return result;
        }

        final MatrixCursor.RowBuilder row = result.newRow();
        final Path baseDir = homeEnvironment.getBaseDir();

        int flags = Root.FLAG_SUPPORTS_CREATE
                | Root.FLAG_SUPPORTS_RECENTS
                | Root.FLAG_SUPPORTS_SEARCH
                | Root.FLAG_LOCAL_ONLY;
        if (context.getResources().getBoolean(R.bool.documents_support_eject)) {
            flags |= Root.FLAG_SUPPORTS_EJECT;
        }

        row.add(Root.COLUMN_ROOT_ID, HomeEnvironment.ROOT);
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForPath(baseDir));
        row.add(Root.COLUMN_FLAGS, flags);
        row.add(Root.COLUMN_ICON, R.drawable.ic_storage);
        row.add(Root.COLUMN_MIME_TYPES, DocumentUtils.getChildMimeTypes(baseDir));
        row.add(Root.COLUMN_TITLE, context.getString(R.string.app_name));
        row.add(Root.COLUMN_SUMMARY, context.getString(R.string.anemo_description));
        return result;
    }

    @NonNull
    @Override
    public Cursor queryDocument(@NonNull String documentId,
                                @Nullable String[] projection)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(documentProjection(projection));
        includeFile(result, documentId);
        return result;
    }

    @NonNull
    @Override
    public Cursor queryChildDocuments(@NonNull String parentDocumentId,
                                      @NonNull String[] projection,
                                      @Nullable String sortOrder)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(documentProjection(projection));
        final Path parent = getPathForId(parentDocumentId);

        try {
            Files.list(parent).forEach(path -> includeFile(result, path));
        } catch (IOException ignored) {
            // Include less items
        }

        if (parent.equals(homeEnvironment.getBaseDir()) && showInfo) {
            // Show info in root dir
            final Bundle extras = new Bundle();
            extras.putCharSequence(DocumentsContract.EXTRA_INFO,
                    getContext().getText(R.string.anemo_info));
            result.setExtras(extras);
            // Hide from now on
            showInfo = false;
        }
        return result;
    }

    @NonNull
    @Override
    public Cursor queryRecentDocuments(@NonNull String rootId,
                                       @NonNull String[] projection)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(documentProjection(projection));
        DocumentUtils.getLastModifiedFiles(getPathForId(rootId), MAX_LAST_MODIFIED)
                .forEach(file -> includeFile(result, file));
        return result;
    }

    @NonNull
    @Override
    public Cursor querySearchDocuments(@NonNull String rootId,
                                       @NonNull String query,
                                       @Nullable String[] projection)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(documentProjection(projection));
        DocumentUtils.queryFiles(getPathForId(rootId), query, MAX_SEARCH_RESULTS)
                .forEach(file -> includeFile(result, file));
        return result;
    }

    /* Open */

    @NonNull
    @Override
    public ParcelFileDescriptor openDocument(@NonNull String documentId,
                                             @NonNull String mode,
                                             @Nullable CancellationSignal signal)
            throws FileNotFoundException {
        return ParcelFileDescriptor.open(getPathForId(documentId).toFile(),
                ParcelFileDescriptor.parseMode(mode));
    }

    @NonNull
    @Override
    public AssetFileDescriptor openDocumentThumbnail(@NonNull String documentId,
                                                     @Nullable Point sizeHint,
                                                     @Nullable CancellationSignal signal)
            throws FileNotFoundException {
        final ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                getPathForId(documentId).toFile(),
                ParcelFileDescriptor.MODE_READ_ONLY);
        return new AssetFileDescriptor(pfd, 0, pfd.getStatSize());
    }

    /* Manage */

    @NonNull
    @Override
    public String createDocument(@NonNull String parentDocumentId,
                                 @NonNull String mimeType,
                                 @NonNull String displayName)
            throws FileNotFoundException {
        final Path parent = getPathForId(parentDocumentId);
        final Path target = parent.resolve(displayName);

        try {
            if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                Files.createDirectory(target);
            } else {
                Files.createFile(target);
            }

            Files.setPosixFilePermissions(target, HomeEnvironment.ATTR_DEFAULT_POSIX);

            notifyChildChange(parentDocumentId);
            return getDocIdForPath(target);
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to create document with name "
                    + displayName + " and in " + parentDocumentId);
        }
    }

    @Override
    public void deleteDocument(@NonNull String documentId) throws FileNotFoundException {
        final Path path = getPathForId(documentId);
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to delete document with id " + documentId);
        }

        notifyChildChange(getDocIdForPath(path.getParent()));
    }

    @Override
    public void removeDocument(@NonNull String documentId,
                               @NonNull String parentDocumentId) throws FileNotFoundException {
        deleteDocument(documentId);
    }

    @NonNull
    @Override
    public String copyDocument(@NonNull String sourceDocumentId,
                               @NonNull String targetParentDocumentId)
            throws FileNotFoundException {
        final Path source = getPathForId(sourceDocumentId);
        final Path targetParent = getPathForId(targetParentDocumentId);
        final Path target = targetParent.resolve(source.getFileName().toString());

        try {
            Files.copy(source,
                    target,
                    StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to copy " + sourceDocumentId
                    + " to " + targetParentDocumentId + ": " + e.getMessage());
        }

        notifyChildChange(targetParentDocumentId);
        return getDocIdForPath(target);
    }

    @NonNull
    @Override
    public String moveDocument(@NonNull String sourceDocumentId,
                               @NonNull String sourceParentDocumentId,
                               @NonNull String targetParentDocumentId)
            throws FileNotFoundException {
        final Path source = getPathForId(sourceDocumentId);
        final Path targetParent = getPathForId(targetParentDocumentId);
        final Path target = targetParent.resolve(source.getFileName().toString());

        try {
            Files.move(source,
                    target,
                    StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to move " + sourceDocumentId
                    + " to " + targetParentDocumentId + ": " + e.getMessage());
        }

        notifyChildChange(sourceParentDocumentId);
        notifyChildChange(targetParentDocumentId);
        return getDocIdForPath(target);
    }

    @NonNull
    @Override
    public String renameDocument(@NonNull String documentId,
                                 @NonNull String displayName)
            throws FileNotFoundException {
        final Path path = getPathForId(documentId);
        final Path parent = path.getParent();
        final Path target = parent.resolve(displayName);

        try {
            Files.move(path, target);
        } catch (IOException e) {
            throw new FileNotFoundException("Couldn't rename " + documentId
                    + " to " + displayName);
        }

        notifyChildChange(getDocIdForPath(parent));
        return getDocIdForPath(target);
    }

    @NonNull
    @Override
    public String getDocumentType(@NonNull String documentId) throws FileNotFoundException {
        return DocumentUtils.getTypeForPath(getPathForId(documentId));
    }

    @Nullable
    @Override
    public Bundle getDocumentMetadata(@NonNull String documentId) throws FileNotFoundException {
        final Path path = getPathForId(documentId);
        Bundle bundle = null;
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                final Int64Ref treeCount = new Int64Ref(0);
                final Int64Ref treeSize = new Int64Ref(0);

                try {
                    Files.walkFileTree(path, new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir,
                                                                 BasicFileAttributes attrs) {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file,
                                                         BasicFileAttributes attrs) {
                            treeCount.value += 1;
                            treeSize.value += attrs.size();
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file,
                                                               IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir,
                                                                  IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });

                    bundle = new Bundle();
                    bundle.putLong(DocumentsContract.METADATA_TREE_COUNT, treeCount.value);
                    bundle.putLong(DocumentsContract.METADATA_TREE_SIZE, treeSize.value);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to retrieve metadata", e);
                }
            } else if (Files.isRegularFile(path) && Files.isReadable(path)) {
                bundle = new Bundle();
                try {
                    bundle.putLong(DocumentsContract.METADATA_TREE_SIZE, Files.size(path));
                } catch (IOException ignored) {
                    // Skip this column
                }
            }
        }

        return bundle;
    }

    @Override
    public void ejectRoot(String rootId) {
        if (HomeEnvironment.ROOT.equals(rootId)) {
            lockStore.lock();
        }
    }

    /* Projection */

    @NonNull
    private static String[] rootProjection(@Nullable String[] projection) {
        return projection != null
                ? projection
                : DEFAULT_ROOT_PROJECTION;
    }

    @NonNull
    private static String[] documentProjection(@Nullable String[] projection) {
        return projection == null
                ? DEFAULT_DOCUMENT_PROJECTION
                : projection;
    }

    /* Results */

    private void includeFile(@NonNull MatrixCursor result,
                             @NonNull String docId)
            throws FileNotFoundException {
        includeFile(result, docId, getPathForId(docId));
    }

    private void includeFile(@NonNull MatrixCursor result,
                             @NonNull Path path) {
        includeFile(result, getDocIdForPath(path), path);
    }

    private void includeFile(@NonNull MatrixCursor result,
                             @NonNull String docId,
                             @NonNull Path path) {
        int flags = 0;
        if (Files.isDirectory(path)) {
            if (Files.isWritable(path)) {
                flags |= Document.FLAG_DIR_SUPPORTS_CREATE;

                // Additional features for user-created directories
                if (!homeEnvironment.isDefaultDirectory(path)) {
                    flags |= Document.FLAG_SUPPORTS_DELETE
                            | Document.FLAG_SUPPORTS_MOVE
                            | Document.FLAG_SUPPORTS_RENAME;
                }
            }
        } else if (Files.isWritable(path)) {
            flags |= Document.FLAG_SUPPORTS_DELETE
                    | Document.FLAG_SUPPORTS_MOVE
                    | Document.FLAG_SUPPORTS_RENAME
                    | Document.FLAG_SUPPORTS_WRITE;
        }

        final String fileName = path.getFileName().toString();
        final String mimeType = DocumentUtils.getTypeForPath(path);

        if (mimeType.startsWith("image/")) {
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, fileName);
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_FLAGS, flags);

        try {
            row.add(Document.COLUMN_SIZE, Files.size(path));
            row.add(Document.COLUMN_LAST_MODIFIED, Files.getLastModifiedTime(path).toMillis());
        } catch (IOException ignored) {
            // Skip these columns
        }
    }

    /* Document ids */

    @NonNull
    private String getDocIdForPath(@NonNull Path path) {
        final Path rootPath = homeEnvironment.getBaseDir();
        final String id = rootPath.equals(path)
                ? ""
                : path.toString().replaceFirst(rootPath.toString(), "");
        return HomeEnvironment.ROOT + ':' + id;
    }

    @NonNull
    private Path getPathForId(@NonNull String documentId) throws FileNotFoundException {
        final Path baseDir = homeEnvironment.getBaseDir();
        if (documentId.equals(HomeEnvironment.ROOT)) {
            return baseDir;
        }

        final int splitIndex = documentId.indexOf(':', 1);
        if (splitIndex < 0) {
            throw new FileNotFoundException("No root for " + documentId);
        }

        final String targetPath = documentId.substring(splitIndex + 1);
        final Path target = Paths.get(baseDir.toString(), targetPath);
        if (!Files.exists(target)) {
            throw new FileNotFoundException("No path for " + documentId + " at " + target);
        }
        return target;
    }

    /* Notify */

    private void onLockChanged(boolean isLocked) {
        cr.notifyChange(DocumentsContract.buildRootsUri(HomeEnvironment.AUTHORITY), null);
    }

    private void notifyChildChange(@NonNull String parentId) {
        cr.notifyChange(DocumentsContract.buildChildDocumentsUri(
                HomeEnvironment.AUTHORITY, parentId), null);
    }
}
