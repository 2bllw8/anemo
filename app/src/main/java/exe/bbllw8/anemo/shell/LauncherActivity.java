/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.shell;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Optional;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.documents.home.HomeEnvironment;
import exe.bbllw8.anemo.lock.LockStore;
import exe.bbllw8.anemo.lock.UnlockActivity;

public class LauncherActivity extends Activity {
    // https://cs.android.com/android/platform/superproject/+/master:packages/apps/DocumentsUI/AndroidManifest.xml
    private static final String DOCUMENTS_UI_PACKAGE = "com.android.documentsui";
    private static final String DOCUMENTS_UI_ACTIVITY = DOCUMENTS_UI_PACKAGE
            + ".files.FilesActivity";
    private static final String DOCUMENTS_UI_ALIAS_ACTIVITY = DOCUMENTS_UI_PACKAGE
            + ".FilesActivity";
    private static final String GOOGLE_DOCUMENTS_UI_PACKAGE = "com.google.android.documentsui";
    private static final String TYPE_DOCS_DIRECTORY = "vnd.android.document/directory";
    private static final Uri ANEMO_URI = DocumentsContract.buildRootsUri(HomeEnvironment.AUTHORITY);

    private final Intent[] LAUNCHER_INTENTS = {
            // AOSP, up to Android 11
            new Intent(Intent.ACTION_VIEW, ANEMO_URI).setClassName(DOCUMENTS_UI_PACKAGE,
                    DOCUMENTS_UI_ACTIVITY),
            new Intent(Intent.ACTION_VIEW, ANEMO_URI).setClassName(DOCUMENTS_UI_PACKAGE,
                    DOCUMENTS_UI_ALIAS_ACTIVITY),
            // Pixels, Android 12+
            new Intent(Intent.ACTION_VIEW, ANEMO_URI).setClassName(GOOGLE_DOCUMENTS_UI_PACKAGE,
                    DOCUMENTS_UI_ACTIVITY),
            // Android 13+
            new Intent(Intent.ACTION_VIEW, ANEMO_URI).setType(TYPE_DOCS_DIRECTORY)
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT
                            | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP),};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (LockStore.getInstance(this).isLocked()) {
            startActivity(new Intent(this, UnlockActivity.class)
                    .putExtra(UnlockActivity.OPEN_AFTER_UNLOCK, true));
        } else {
            final PackageManager pm = getPackageManager();
            final Optional<Intent> fileIntent = Arrays.stream(LAUNCHER_INTENTS)
                    .filter(intent -> canHandle(pm, intent))
                    .findAny();
            if (fileIntent.isPresent()) {
                startActivity(fileIntent.get());
                overridePendingTransition(0, 0);
            } else {
                Toast.makeText(this, R.string.launcher_no_activity, Toast.LENGTH_LONG).show();
            }
        }
        finish();
    }

    private boolean canHandle(PackageManager pm, Intent intent) {
        return pm.resolveActivity(intent, PackageManager.MATCH_ALL) != null;
    }
}
