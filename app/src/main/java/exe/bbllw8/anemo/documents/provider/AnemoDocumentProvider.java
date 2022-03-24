/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.util.function.Consumer;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.documents.home.HomeEnvironment;
import exe.bbllw8.anemo.lock.LockStore;
import exe.bbllw8.either.Try;

public final class AnemoDocumentProvider extends DocumentsProvider {
    private static final String TAG = "AnemoDocumentProvider";

    private static final int MAX_SEARCH_RESULTS = 20;
    private static final int MAX_LAST_MODIFIED = 5;

    private ContentResolver cr;
    private DocumentOperations operations;
    private LockStore lockStore;

    private boolean showInfo = true;

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        cr = context.getContentResolver();
        lockStore = LockStore.getInstance(context);

        return Try.from(() -> HomeEnvironment.getInstance(context))
                .fold(failure -> {
                    Log.e(TAG, "Failed to setup", failure);
                    return false;
                }, homeEnvironment -> {
                    this.operations = new DocumentOperations(homeEnvironment);
                    return true;
                });
    }

    @Override
    public void shutdown() {
        lockStore.removeListener(onLockChanged);
        super.shutdown();
    }

    /* Query */

    @Override
    public Cursor queryRoots(@NonNull String[] projection) {
        final Context context = getContext();
        return operations.queryRoot(context.getString(R.string.app_name),
                context.getString(R.string.anemo_description),
                R.drawable.ic_storage,
                lockStore.isLocked());
    }

    @Override
    public Cursor queryDocument(@NonNull String documentId,
                                @Nullable String[] projection)
            throws FileNotFoundException {
        final Try<Cursor> result = operations.queryDocument(documentId);
        if (result.isFailure()) {
            result.failed().forEach(failure ->
                    Log.e(TAG, "Failed to query document", failure));
            throw new FileNotFoundException(documentId);
        } else {
            return result.get();
        }
    }

    @Override
    public Cursor queryChildDocuments(@NonNull String parentDocumentId,
                                      @NonNull String[] projection,
                                      @Nullable String sortOrder)
            throws FileNotFoundException {
        final Try<Cursor> result = operations.queryChildDocuments(parentDocumentId);
        if (result.isFailure()) {
            result.failed().forEach(failure ->
                    Log.e(TAG, "Failed to query child documents", failure));
            throw new FileNotFoundException(parentDocumentId);
        } else {
            final Cursor c = result.get();
            if (showInfo && operations.isRoot(parentDocumentId)) {
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
    }

    @Override
    public Cursor queryRecentDocuments(@NonNull String rootId,
                                       @NonNull String[] projection)
            throws FileNotFoundException {
        final Try<Cursor> result = operations.queryRecentDocuments(rootId, MAX_LAST_MODIFIED);
        if (result.isFailure()) {
            result.failed().forEach(failure ->
                    Log.e(TAG, "Failed to query recent documents", failure));
            throw new FileNotFoundException(rootId);
        } else {
            return result.get();
        }
    }

    @Override
    public Cursor querySearchDocuments(@NonNull String rootId,
                                       @NonNull String query,
                                       @Nullable String[] projection)
            throws FileNotFoundException {
        final Try<Cursor> result = operations.querySearchDocuments(rootId, query,
                MAX_SEARCH_RESULTS);
        if (result.isFailure()) {
            result.failed().forEach(failure ->
                    Log.e(TAG, "Failed to query search documents", failure));
            throw new FileNotFoundException(rootId);
        } else {
            return result.get();
        }
    }

    /* Open */

    @Override
    public ParcelFileDescriptor openDocument(@NonNull String documentId,
                                             @NonNull String mode,
                                             @Nullable CancellationSignal signal)
            throws FileNotFoundException {
        final Try<ParcelFileDescriptor> pfd = operations.openDocument(documentId, mode);
        if (pfd.isFailure()) {
            pfd.failed().forEach(failure ->
                    Log.e(TAG, "Failed to open document", failure));
            throw new FileNotFoundException(documentId);
        } else {
            return pfd.get();
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(@NonNull String documentId,
                                                     @Nullable Point sizeHint,
                                                     @Nullable CancellationSignal signal)
            throws FileNotFoundException {
        final Try<AssetFileDescriptor> afd = operations.openDocumentThumbnail(documentId);
        if (afd.isFailure()) {
            afd.failed().forEach(failure ->
                    Log.e(TAG, "Failed to open document thumbnail", failure));
            throw new FileNotFoundException(documentId);
        } else {
            return afd.get();
        }
    }

    /* Manage */

    @Override
    public String createDocument(@NonNull String parentDocumentId,
                                 @NonNull String mimeType,
                                 @NonNull String displayName)
            throws FileNotFoundException {
        final Try<String> result = operations.createDocument(parentDocumentId, mimeType,
                displayName);
        if (result.isFailure()) {
            result.failed().forEach(failure ->
                    Log.e(TAG, "Failed to create document", failure));
            throw new FileNotFoundException(parentDocumentId);
        } else {
            notifyChildChange(parentDocumentId);
            return result.get();
        }
    }

    @Override
    public void deleteDocument(@NonNull String documentId) throws FileNotFoundException {
        final Try<String> result = operations.deleteDocument(documentId);
        if (result.isFailure()) {
            result.failed().forEach(failure ->
                    Log.e(TAG, "Failed to delete document", failure));
            throw new FileNotFoundException(documentId);
        } else {
            notifyChildChange(result.get());
        }
    }

    @Override
    public void removeDocument(@NonNull String documentId,
                               @NonNull String parentDocumentId) throws FileNotFoundException {
        deleteDocument(documentId);
    }

    @Override
    public String copyDocument(@NonNull String sourceDocumentId,
                               @NonNull String targetParentDocumentId)
            throws FileNotFoundException {
        final Try<String> result = operations.copyDocument(sourceDocumentId,
                targetParentDocumentId);
        if (result.isFailure()) {
            result.failed().forEach(failure ->
                    Log.e(TAG, "Failed to copy document", failure));
            throw new FileNotFoundException(targetParentDocumentId);
        } else {
            notifyChildChange(targetParentDocumentId);
            return result.get();
        }
    }

    @Override
    public String moveDocument(@NonNull String sourceDocumentId,
                               @NonNull String sourceParentDocumentId,
                               @NonNull String targetParentDocumentId)
            throws FileNotFoundException {
        final Try<String> result = operations.moveDocument(sourceDocumentId,
                targetParentDocumentId);
        if (result.isFailure()) {
            result.failed().forEach(failure ->
                    Log.e(TAG, "Failed to move document", failure));
            throw new FileNotFoundException(sourceDocumentId);
        } else {
            notifyChildChange(sourceParentDocumentId);
            notifyChildChange(targetParentDocumentId);
            return result.get();
        }
    }

    @Override
    public String renameDocument(@NonNull String documentId,
                                 @NonNull String displayName)
            throws FileNotFoundException {
        final Try<Pair<String, String>> result = operations.renameDocument(documentId, displayName);
        if (result.isFailure()) {
            result.failed().forEach(failure ->
                    Log.e(TAG, "Failed to rename document", failure));
            throw new FileNotFoundException(documentId);
        } else {
            final Pair<String, String> pair = result.get();
            notifyChildChange(pair.first);
            return pair.second;
        }
    }

    @Override
    public String getDocumentType(@NonNull String documentId) throws FileNotFoundException {
        final Try<String> result = operations.getDocumentType(documentId);
        if (result.isFailure()) {
            result.failed().forEach(failure ->
                    Log.e(TAG, "Failed to get document type", failure));
            throw new FileNotFoundException(documentId);
        } else {
            return result.get();
        }
    }

    @Override
    public Bundle getDocumentMetadata(@NonNull String documentId) {
        if (Build.VERSION.SDK_INT >= 29) {
            return operations.getSizeAndCount(documentId)
                    .fold(failure -> {
                        Log.e(TAG, "Failed to retrieve metadata", failure);
                        return null;
                    }, info -> {
                        final Bundle bundle = new Bundle();
                        bundle.putLong(DocumentsContract.METADATA_TREE_SIZE, info.first);
                        bundle.putLong(DocumentsContract.METADATA_TREE_COUNT, info.second);
                        return bundle;
                    });
        } else {
            return null;
        }
    }

    @Override
    public void ejectRoot(String rootId) {
        if (HomeEnvironment.ROOT.equals(rootId)) {
            lockStore.lock();
        }
    }

    /* Notify */

    private void notifyChildChange(String parentId) {
        cr.notifyChange(DocumentsContract.buildChildDocumentsUri(
                HomeEnvironment.AUTHORITY, parentId), null);
    }

    private final Consumer<Boolean> onLockChanged = isLocked ->
            cr.notifyChange(DocumentsContract.buildRootsUri(HomeEnvironment.AUTHORITY), null);
}
