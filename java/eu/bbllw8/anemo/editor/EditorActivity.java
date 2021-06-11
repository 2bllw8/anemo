/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
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

            final ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_editor_close);
                actionBar.setHomeActionContentDescription(R.string.editor_action_quit);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

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
            saveContents(false);
            return true;
        } else if (id == R.id.editorUndo) {
            undoAction();
            return true;
        } else if (id == R.id.editorFontSizeSmall
                || id == R.id.editorFontSizeMedium
                || id == R.id.editorFontSizeLarge) {
            changeFontSize(item);
            return true;
        } else if (id == R.id.editorFontStyleMono
                || id == R.id.editorFontStyleSans
                || id == R.id.editorFontStyleSerif) {
            changeFontStyle(item);
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onMenuItemSelected(featureId, item);
        }
    }

    @Override
    public void onBackPressed() {
        if (dirty) {
            showQuitMessage();
        } else {
            super.onBackPressed();
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

    /* File loading */

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

    /* Content operations */

    private void setContent(@NonNull EditorFile editorFile, @NonNull String content) {
        this.editorFile = editorFile;

        updateTitle();

        loadView.setVisibility(View.GONE);
        textEditorView.setVisibility(View.VISIBLE);
        textEditorView.setText(content);

        // Set listener after the contents
        registerTextListeners();
    }

    private void saveContents(boolean quitWhenSaved) {
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
                        showSavedMessage(quitWhenSaved);
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

    /* UI */

    private void registerTextListeners() {
        textEditorView.post(() -> {
            textEditorView.addTextChangedListener(this);
            textEditorView.addTextChangedListener(editorHistory);
        });
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

    /* Config */

    private void changeFontSize(@NonNull MenuItem item) {
        final int id = item.getItemId();
        final int newTextSizeRes;
        if (id == R.id.editorFontSizeSmall) {
            newTextSizeRes = R.dimen.editorFontSizeSmall;
        } else if (id == R.id.editorFontSizeLarge) {
            newTextSizeRes = R.dimen.editorFontSizeLarge;
        } else {
            newTextSizeRes = R.dimen.editorFontSizeMedium;
        }

        textEditorView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(newTextSizeRes));
        item.setChecked(true);
    }

    private void changeFontStyle(@NonNull MenuItem item) {
        final int id = item.getItemId();
        final Typeface newTypeface;
        if (id == R.id.editorFontStyleSans) {
            newTypeface = Typeface.SANS_SERIF;
        } else if (id == R.id.editorFontStyleSerif) {
            newTypeface = Typeface.SERIF;
        } else {
            newTypeface = Typeface.MONOSPACE;
        }

        textEditorView.setTypeface(newTypeface);
        item.setChecked(true);
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

    private void showSavedMessage(boolean finishOnDismiss) {
        final TipDialog dialog = new TipDialog.Builder(this)
                .setIcon(eu.bbllw8.anemo.tip.R.drawable.tip_ic_success)
                .setMessage(getString(R.string.editor_save_success))
                .setCancelable(false)
                .setOnDismissListener(() -> {
                    if (finishOnDismiss) {
                        finish();
                    }
                })
                .show();

        new Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 1500L);
    }

    private void showQuitMessage() {
        final String fileName = editorFile == null
                ? getString(R.string.editor_title_generic)
                : editorFile.getName();

        new AlertDialog.Builder(this, R.style.AppTheme)
                .setTitle(fileName)
                .setMessage(getString(R.string.editor_save_quit_ask, fileName))
                .setCancelable(false)
                .setPositiveButton(R.string.editor_action_save_and_quit,
                        (d, which) -> {
                            d.dismiss();
                            saveContents(true);
                        })
                .setNegativeButton(R.string.editor_action_quit,
                        (d, which) -> {
                            d.dismiss();
                            finish();
                        })
                .show();
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
