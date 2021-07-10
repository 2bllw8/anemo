/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.file;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class FileActivity extends Activity {

    private FileSource fileSource;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileSource = new FileSource(getContentResolver());

        setContentView(R.layout.file_activity);
    }

    private void openFile(@NonNull FileEntry file) {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT,
                fileSource.uriFor(file));
        startActivity(intent);
    }
}
