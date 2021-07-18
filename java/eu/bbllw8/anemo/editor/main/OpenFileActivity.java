/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.main;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class OpenFileActivity extends Activity {
    private static final String TYPE_PLAIN_TEXT = "text/plain";
    private static final int REQUEST_OPEN_FILE = 12;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/*");
        startActivityForResult(intent, REQUEST_OPEN_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK
                && requestCode == REQUEST_OPEN_FILE
                && data != null) {
            openInNewWindow(data.getData(), data.getType());
        }
        finish();
    }

    private void openInNewWindow(@NonNull Uri uri,
                                 @Nullable String type) {
        final Intent intent = new Intent(this, EditorActivity.class)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                .setDataAndType(uri, type == null ? TYPE_PLAIN_TEXT : type);
        startActivity(intent);
    }

}
