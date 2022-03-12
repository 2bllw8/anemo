/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.shell;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.annotation.Nullable;

import exe.bbllw8.anemo.R;
import exe.bbllw8.anemo.documents.home.HomeEnvironment;
import exe.bbllw8.anemo.documents.lock.LockStore;
import exe.bbllw8.anemo.documents.password.PasswordActivity;

public class ShortcutActivity extends Activity {
    // https://cs.android.com/android/platform/superproject/+/master:packages/apps/DocumentsUI/AndroidManifest.xml
    private static final String DOCUMENTS_UI_PACKAGE = "com.android.documentsui";
    private static final String DOCUMENTS_UI_ACTIVITY =
            DOCUMENTS_UI_PACKAGE + ".files.FilesActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (LockStore.getInstance(this).isLocked()) {
            startActivity(new Intent(this, PasswordActivity.class)
                    .putExtra(PasswordActivity.OPEN_AFTER_UNLOCK, true));
        } else {
            final PackageManager pm = getPackageManager();
            final Intent documentsUiIntent = new Intent(Intent.ACTION_VIEW)
                    .setData(DocumentsContract.buildRootsUri(HomeEnvironment.AUTHORITY))
                    .setClassName(DOCUMENTS_UI_PACKAGE, DOCUMENTS_UI_ACTIVITY);
            if (pm.resolveActivity(documentsUiIntent, PackageManager.MATCH_SYSTEM_ONLY) == null) {
                Toast.makeText(this, R.string.shortcut_no_activity, Toast.LENGTH_LONG).show();
            } else {
                startActivity(documentsUiIntent);
            }
        }
        finish();
    }
}
