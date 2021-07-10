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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import eu.bbllw8.anemo.task.TaskExecutor;

public final class FileActivity extends Activity {

    private FileSource fileSource;
    private final FileListAdapter adapter = new FileListAdapter(this::visit);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileSource = new FileSource(getContentResolver());

        setContentView(R.layout.file_activity);

        final RecyclerView listView = findViewById(R.id.fileList);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setAdapter(adapter);

        TaskExecutor.runTask(() -> fileSource.browseRoot(), this::onVisitDir);
    }

    private void openFile(@NonNull FileEntry entry) {
        final Intent intent = new Intent(Intent.ACTION_VIEW)
                .setData(fileSource.uriFor(entry))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivity(intent);
    }

    private void visit(@NonNull FileEntry entry) {
        if (entry.isDirectory()) {
            TaskExecutor.runTask(() -> fileSource.browseDir(entry), this::onVisitDir);
        } else {
            openFile(entry);
        }
    }

    private void onVisitDir(@NonNull List<FileEntry> contents) {
        adapter.setData(contents);
    }
}
