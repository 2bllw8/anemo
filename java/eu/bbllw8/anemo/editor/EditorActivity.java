/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.bbllw8.anemo.editor.history.EditorHistory;
import eu.bbllw8.anemo.editor.tasks.EditorFileLoaderTask;
import eu.bbllw8.anemo.editor.tasks.EditorFileReaderTask;
import eu.bbllw8.anemo.editor.tasks.EditorFileWriterTask;
import eu.bbllw8.anemo.editor.tasks.GetCursorCoordinatesTask;
import eu.bbllw8.anemo.task.TaskExecutor;
import eu.bbllw8.anemo.tip.TipDialog;

public final class EditorActivity extends Activity implements TextWatcher {
    private static final String KEY_EDITOR_FILE = "editor_file";
    private static final String KEY_HISTORY_STATE = "editor_history";

    private boolean dirty = false;

    @Nullable
    private EditorFile editorFile = null;

    private View loadView;
    private TextView summaryView;
    private TextEditorView textEditorView;
    private EditorHistory editorHistory;

    private MenuItem undoButton;
    private MenuItem saveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final Uri inputUri = intent.getData();
        if (inputUri == null) {
            finish();
        } else {
            setContentView(R.layout.editor_ui);
            loadView = findViewById(R.id.editorProgress);
            summaryView = findViewById(R.id.editorSummary);
            textEditorView = findViewById(R.id.editorContent);

            editorHistory = new EditorHistory(textEditorView::getEditableText,
                    getResources().getInteger(R.integer.editor_history_buffer_size));

            summaryView.setText(getString(R.string.editor_summary_info, 1, 1));
            textEditorView.setOnCursorChanged(this::updateSummary);

            if (savedInstanceState == null) {
                openFile(inputUri, intent.getType());
            } else {
                registerTextListeners();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (editorFile != null) {
            outState.putParcelable(KEY_EDITOR_FILE, editorFile);
        }
        if (editorHistory != null) {
            outState.putParcelable(KEY_HISTORY_STATE, editorHistory.saveInstance());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final EditorFile savedEditorFile = savedInstanceState.getParcelable(KEY_EDITOR_FILE);
        if (savedEditorFile != null) {
            editorFile = savedEditorFile;
            updateTitle();
        }
        final Parcelable historyState = savedInstanceState.getParcelable(KEY_HISTORY_STATE);
        if (historyState != null && editorHistory != null) {
            editorHistory.restoreInstance(historyState);
            if (editorHistory.canUndo()) {
                setDirty();
            } else {
                setNotDirty();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        if (menuInflater == null) {
            return super.onCreateOptionsMenu(menu);
        } else {
            menuInflater.inflate(R.menu.editor_menu, menu);
            undoButton = menu.findItem(R.id.editorUndo);
            saveButton = menu.findItem(R.id.editorSave);
            return true;
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.editorSave) {
            saveContents();
            return true;
        } else if (id == R.id.editorUndo) {
            undoAction();
            return true;
        } else {
            return super.onMenuItemSelected(featureId, item);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        setDirty();
    }

    private void openFile(@NonNull Uri uri, @Nullable String type) {
        summaryView.setText(R.string.editor_summary_loading);
        loadView.setVisibility(View.VISIBLE);

        TaskExecutor.runTask(new EditorFileLoaderTask(getContentResolver(), uri, type),
                this::readFile,
                this::showOpenErrorMessage);
    }

    private void readFile(@NonNull EditorFile editorFile) {
        TaskExecutor.runTask(new EditorFileReaderTask(getContentResolver(), editorFile),
                content -> setContent(editorFile, content),
                () -> showReadErrorMessage(editorFile));
    }

    private void setContent(@NonNull EditorFile editorFile, @NonNull String content) {
        this.editorFile = editorFile;

        updateTitle();

        loadView.setVisibility(View.GONE);
        textEditorView.setVisibility(View.VISIBLE);
        textEditorView.setText(content);

        // Set listener after the contents
        registerTextListeners();
    }

    private void registerTextListeners() {
        textEditorView.post(() -> {
            textEditorView.addTextChangedListener(this);
            textEditorView.addTextChangedListener(editorHistory);
        });
    }

    private void saveContents() {
        if (editorFile == null || !dirty) {
            return;
        }

        final TipDialog savingDialog = new TipDialog.Builder(this)
                .setCancelable(false)
                .setDismissOnTouchOutside(false)
                .setProgress()
                .setMessage(getString(R.string.editor_save_in_progress, editorFile.getName()))
                .show();

        final String contents = textEditorView.getText().toString();
        TaskExecutor.runTask(new EditorFileWriterTask(getContentResolver(), editorFile, contents),
                success -> {
                    if (success) {
                        savingDialog.dismiss();
                        showSavedMessage();
                    } else {
                        showWriteErrorMessage(editorFile);
                    }
                });
    }

    private void undoAction() {
        editorHistory.undo();
        if (!editorHistory.canUndo()) {
            setNotDirty();
        }
    }

    private void updateTitle() {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null && editorFile != null) {
            actionBar.setTitle(editorFile.getName());
        }
    }

    private void updateSummary(int cursorStart, int cursorEnd) {
        final String content = textEditorView.getText().toString();
        TaskExecutor.runTask(new GetCursorCoordinatesTask(content, cursorStart),
                point -> {
                    final String summary = cursorStart == cursorEnd
                            ? getString(R.string.editor_summary_info,
                            point.y, point.x)
                            : getString(R.string.editor_summary_select,
                            cursorEnd - cursorStart, point.y, point.x);
                    summaryView.post(() -> summaryView.setText(summary));
                });
    }

    /* Dirty */

    private void setNotDirty() {
        if (dirty) {
            dirty = false;
            undoButton.setEnabled(false);
            saveButton.setEnabled(false);
        }
    }

    private void setDirty() {
        if (!dirty) {
            dirty = true;
            undoButton.setEnabled(true);
            saveButton.setEnabled(true);
        }
    }

    /* Dialogs */

    private void showSavedMessage() {
        final TipDialog dialog = new TipDialog.Builder(this)
                .setIcon(eu.bbllw8.anemo.tip.R.drawable.tip_ic_success)
                .setMessage(getString(R.string.editor_save_success))
                .setCancelable(false)
                .show();

        new Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 1500L);
    }

    private void showOpenErrorMessage() {
        showErrorMessage(getString(R.string.editor_error_open));
    }

    private void showReadErrorMessage(@NonNull EditorFile editorFile) {
        showErrorMessage(getString(R.string.editor_error_read, editorFile.getName()));
    }

    private void showWriteErrorMessage(@NonNull EditorFile editorFile) {
        showErrorMessage(getString(R.string.editor_save_failed, editorFile.getName()));
    }

    private void showErrorMessage(@NonNull CharSequence message) {
        new TipDialog.Builder(this)
                .setMessage(message)
                .setIcon(eu.bbllw8.anemo.tip.R.drawable.tip_ic_error)
                .setDismissOnTouchOutside(true)
                .setOnDismissListener(this::finish)
                .show();
    }
}
