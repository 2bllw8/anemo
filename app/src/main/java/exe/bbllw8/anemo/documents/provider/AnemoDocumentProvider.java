/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.provider;

import android.app.AuthenticationRequiredException;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Root;
import android.util.Log;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Consumer;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.documents.home.HomeEnvironment;
import exe.bbllw8.anemo.lock.LockStore;
import exe.bbllw8.anemo.lock.UnlockActivity;
import exe.bbllw8.either.Failure;
import exe.bbllw8.either.Success;
import exe.bbllw8.either.Try;

public final class AnemoDocumentProvider extends FileSystemProvider {
    private static final String TAG = "AnemoDocumentProvider";

    private static final String[] DEFAULT_ROOT_PROJECTION = {Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS,
            Root.COLUMN_ICON, Root.COLUMN_TITLE, Root.COLUMN_DOCUMENT_ID,};

    private HomeEnvironment homeEnvironment;
    private LockStore lockStore;

    private boolean showInfo = true;

    @Override
    public boolean onCreate() {
        if (!super.onCreate()) {
            return false;
        }

        final Context context = getContext();
        lockStore = LockStore.getInstance(context);
        lockStore.addListener(onLockChanged);

        return Try.from(() -> HomeEnvironment.getInstance(context)).fold(failure -> {
            Log.e(TAG, "Failed to setup", failure);
            return false;
        }, homeEnvironment -> {
            this.homeEnvironment = homeEnvironment;
            return true;
        });
    }

    @Override
    public void shutdown() {
        lockStore.removeListener(onLockChanged);
        super.shutdown();
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        if (lockStore.isLocked()) {
            return new EmptyCursor();
        }

        final Context context = getContext();
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        final MatrixCursor.RowBuilder row = result.newRow();

        int flags = Root.FLAG_LOCAL_ONLY;
        flags |= Root.FLAG_SUPPORTS_CREATE;
        flags |= Root.FLAG_SUPPORTS_IS_CHILD;
        flags |= DocumentsContract.Root.FLAG_SUPPORTS_EJECT;
        if (Build.VERSION.SDK_INT >= 29) {
            flags |= Root.FLAG_SUPPORTS_SEARCH;
        }

        row.add(Root.COLUMN_ROOT_ID, HomeEnvironment.ROOT)
                .add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, HomeEnvironment.ROOT_DOC_ID)
                .add(Root.COLUMN_FLAGS, flags)
                .add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_storage)
                .add(DocumentsContract.Root.COLUMN_TITLE, context.getString(R.string.app_name))
                .add(DocumentsContract.Root.COLUMN_SUMMARY,
                        context.getString(R.string.anemo_description));
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection,
            String sortOrder) throws FileNotFoundException {
        if (lockStore.isLocked()) {
            return new EmptyCursor();
        }

        final Cursor c = super.queryChildDocuments(parentDocumentId, projection, sortOrder);
        if (showInfo && HomeEnvironment.ROOT_DOC_ID.equals(parentDocumentId)) {
            // Hide from now on
            showInfo = false;
            // Show info in root dir
            final Bundle extras = new Bundle();
            extras.putCharSequence(DocumentsContract.EXTRA_INFO,
                    getContext().getText(R.string.anemo_info));
            c.setExtras(extras);
        }
        return c;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        if (lockStore.isLocked()) {
            return new EmptyCursor();
        } else {
            return super.queryDocument(documentId, projection);
        }
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String[] projection, Bundle queryArgs)
            throws FileNotFoundException {
        if (lockStore.isLocked()) {
            return new EmptyCursor();
        } else {
            return super.querySearchDocuments(rootId, projection, queryArgs);
        }
    }

    @Override
    public DocumentsContract.Path findDocumentPath(String parentDocumentId,
            String childDocumentId) {
        if (lockStore.isLocked()) {
            return new DocumentsContract.Path(null, Collections.emptyList());
        } else {
            return super.findDocumentPath(parentDocumentId, childDocumentId);
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode,
            CancellationSignal signal) throws FileNotFoundException {
        assertUnlocked();
        return super.openDocument(documentId, mode, signal);
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String docId, Point sizeHint,
            CancellationSignal signal) throws FileNotFoundException {
        assertUnlocked();
        return super.openDocumentThumbnail(docId, sizeHint, signal);
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) {
        assertUnlocked();
        return super.createDocument(parentDocumentId, mimeType, displayName);
    }

    @Override
    public void deleteDocument(String documentId) {
        assertUnlocked();
        super.deleteDocument(documentId);
    }

    @Override
    public void removeDocument(String documentId, String parentDocumentId) {
        deleteDocument(documentId);
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId)
            throws FileNotFoundException {
        assertUnlocked();
        return super.copyDocument(sourceDocumentId, targetParentDocumentId);
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId,
            String targetParentDocumentId) {
        assertUnlocked();
        return super.moveDocument(sourceDocumentId, sourceParentDocumentId, targetParentDocumentId);
    }

    @Override
    public String renameDocument(String documentId, String displayName) {
        assertUnlocked();
        return super.renameDocument(documentId, displayName);
    }

    @Override
    public void ejectRoot(String rootId) {
        if (HomeEnvironment.ROOT.equals(rootId)) {
            lockStore.lock();
        }
    }

    @Override
    protected Uri buildNotificationUri(String docId) {
        return DocumentsContract.buildChildDocumentsUri(HomeEnvironment.AUTHORITY, docId);
    }

    @Override
    protected Try<Path> getPathForId(String docId) {
        final Path baseDir = homeEnvironment.getBaseDir();
        if (HomeEnvironment.ROOT_DOC_ID.equals(docId)) {
            return new Success<>(baseDir);
        } else {
            final int splitIndex = docId.indexOf('/', 1);
            if (splitIndex < 0) {
                return new Failure<>(new FileNotFoundException("No root for " + docId));
            } else {
                final String targetPath = docId.substring(splitIndex + 1);
                final Path target = Paths.get(baseDir.toString(), targetPath);
                if (Files.exists(target)) {
                    return new Success<>(target);
                } else {
                    return new Failure<>(
                            new FileNotFoundException("No path for " + docId + " at " + target));
                }
            }
        }
    }

    @Override
    protected String getDocIdForPath(Path path) {
        final Path rootPath = homeEnvironment.getBaseDir();
        if (rootPath.equals(path)) {
            return HomeEnvironment.ROOT_DOC_ID;
        } else {
            return HomeEnvironment.ROOT_DOC_ID
                    + path.toString().replaceFirst(rootPath.toString(), "");
        }
    }

    @Override
    protected boolean isNotEssential(Path path) {
        return !homeEnvironment.isDefaultDirectory(path);
    }

    @Override
    protected void onDocIdChanged(String docId) {
        // no-op
    }

    @Override
    protected void onDocIdDeleted(String docId) {
        // no-op
    }

    /**
     * @throws AuthenticationRequiredException
     *             if {@link LockStore#isLocked()} is true.
     */
    private void assertUnlocked() {
        if (lockStore.isLocked()) {
            final Context context = getContext();
            final Intent intent = new Intent(context, UnlockActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            throw new AuthenticationRequiredException(new Throwable("Locked storage"),
                    PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE));
        }
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection == null ? DEFAULT_ROOT_PROJECTION : projection;
    }

    private final Consumer<Boolean> onLockChanged = isLocked -> cr
            .notifyChange(DocumentsContract.buildRootsUri(HomeEnvironment.AUTHORITY), null);
}
