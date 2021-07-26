/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.help;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import eu.bbllw8.anemo.task.TaskExecutor;

public final class EditorHelpActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.help_ui);

        final TextView contentView = findViewById(R.id.helpContent);

        TaskExecutor.runTask(new LoadHelpContentTextTask(getAssets()),
                contentView::setText,
                () -> contentView.setText(R.string.editor_help_error));
    }
}
