/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Optional;

import eu.bbllw8.anemo.editor.commands.EditorCommand;
import eu.bbllw8.anemo.editor.commands.EditorCommandParser;
import eu.bbllw8.anemo.editor.history.EditorHistory;
import eu.bbllw8.anemo.editor.tasks.EditorFileLoaderTask;
import eu.bbllw8.anemo.editor.tasks.EditorFileReaderTask;
import eu.bbllw8.anemo.editor.tasks.EditorFileWriterTask;
import eu.bbllw8.anemo.editor.tasks.GetCursorCoordinatesTask;
import eu.bbllw8.anemo.editor.tasks.TextDeleteTask;
import eu.bbllw8.anemo.editor.tasks.TextFindTask;
import eu.bbllw8.anemo.editor.tasks.TextSubstituteAllTask;
import eu.bbllw8.anemo.editor.tasks.TextSubstituteFirstTask;
import eu.bbllw8.anemo.task.TaskExecutor;
import eu.bbllw8.anemo.tip.TipDialog;

public final class EditorActivity extends Activity implements TextWatcher {
    private static final String KEY_EDITOR_FILE = "editor_file";
    private static final String KEY_HISTORY_STATE = "editor_history";
    private static final String KEY_SHOW_COMMAND_BAR = "editor_show_command_bar";
    private static final String TYPE_PLAIN_TEXT = "text/plain";
    private static final int REQUEST_CREATE_FILE_AND_QUIT = 10;
    private static final int REQUEST_CREATE_FILE = 11;
    private static final int REQUEST_OPEN_FILE = 12;

    private boolean dirty = false;

    @Nullable
    private EditorFile editorFile = null;

    private ActionBar actionBar;
    private View loadView;
    private TextView summaryView;
    private TextEditorView textEditorView;
    private ViewGroup commandBar;
    private EditText commandField;

    private EditorHistory editorHistory;

    private final EditorCommandParser editorCommandParser = new EditorCommandParser();
    private boolean showCommandBar = false;

    private MenuItem undoButton;
    private MenuItem saveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.editor_ui);
        actionBar = getActionBar();
        loadView = findViewById(R.id.editorProgress);
        summaryView = findViewById(R.id.editorSummary);
        textEditorView = findViewById(R.id.editorContent);
        commandBar = findViewById(R.id.editorCommandBar);
        commandField = findViewById(R.id.editorCommandField);
        final ImageView commandHelpButton = findViewById(R.id.editorCommandHelp);
        final ImageView commandRunButton = findViewById(R.id.editorCommandRun);

        editorHistory = new EditorHistory(textEditorView::getEditableText,
                getResources().getInteger(R.integer.editor_history_buffer_size));

        summaryView.setText(getString(R.string.editor_summary_info, 1, 1));
        textEditorView.setOnCursorChanged(this::updateSummary);
        commandField.setOnKeyListener((v, code, ev) -> {
            if (code == KeyEvent.KEYCODE_ENTER) {
                runCurrentCommand();
                return true;
            } else {
                return false;
            }
        });

        commandHelpButton.setOnClickListener(v -> showCommandHelpMessage());
        commandRunButton.setOnClickListener(v -> runCurrentCommand());

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_editor_close);
            actionBar.setHomeActionContentDescription(R.string.editor_action_quit);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final Intent intent = getIntent();
        final Uri inputUri = intent.getData();
        if (savedInstanceState == null) {
            if (inputUri == null) {
                registerTextListeners();
            } else {
                loadFile(inputUri, intent.getType());
            }
        } else {
            registerTextListeners();
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
        outState.putBoolean(KEY_SHOW_COMMAND_BAR, showCommandBar);
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
        showCommandBar = savedInstanceState.getBoolean(KEY_SHOW_COMMAND_BAR);
        if (showCommandBar) {
            commandBar.setVisibility(View.VISIBLE);
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

            final MenuItem shellButton = menu.findItem(R.id.editorShowShell);
            shellButton.setChecked(EditorShell.isEnabled(this));
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
        } else if (id == R.id.editorCommandVisibility) {
            changeCommandBarVisibility(item);
            return true;
        } else if (id == R.id.editorNew) {
            openFileSaver();
            return true;
        } else if (id == R.id.editorOpen) {
            openFileSelector();
            return true;
        } else if (id == R.id.editorShowShell) {
            changeShellStatus(item);
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
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null || resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CREATE_FILE:
                loadNewSaveFile(data.getData(), data.getType(), false);
                break;
            case REQUEST_CREATE_FILE_AND_QUIT:
                loadNewSaveFile(data.getData(), data.getType(), true);
                break;
            case REQUEST_OPEN_FILE:
                openInNewWindow(data.getData(), data.getType());
                break;
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

    private void loadFile(@NonNull Uri uri, @Nullable String type) {
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

    private void loadNewSaveFile(@NonNull Uri uri,
                                 @Nullable String type,
                                 boolean quitWhenSaved) {
        TaskExecutor.runTask(new EditorFileLoaderTask(getContentResolver(), uri, type),
                editorFile -> saveNewFile(editorFile, quitWhenSaved),
                this::showOpenErrorMessage);
    }

    private void openFileSaver() {
        final Intent intent = new Intent(this, EditorActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivity(intent);
    }

    private void openFileSelector() {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/*");
        startActivityForResult(intent, REQUEST_OPEN_FILE);
    }

    private void openFileSaver(boolean quitWhenSaved) {
        String title = textEditorView.getText().toString().split("\n")[0];
        if (title.length() > 20) {
            title = title.substring(0, 20);
        }
        title += ".txt";

        final Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(TYPE_PLAIN_TEXT)
                .putExtra(Intent.EXTRA_TITLE, title);
        startActivityForResult(intent, quitWhenSaved
                ? REQUEST_CREATE_FILE_AND_QUIT
                : REQUEST_CREATE_FILE);
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
        if (dirty) {
            if (editorFile == null) {
                openFileSaver(quitWhenSaved);
            } else {
                writeContents(editorFile, quitWhenSaved);
            }
        }
    }

    private void saveNewFile(@NonNull EditorFile editorFile,
                             boolean quitWhenSaved) {
        this.editorFile = editorFile;
        if (!quitWhenSaved) {
            updateTitle();
        }
        writeContents(editorFile, quitWhenSaved);
    }

    private void writeContents(@NonNull EditorFile editorFile,
                               boolean quitWhenSaved) {
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
                        // Change only the variable, still allow undo
                        dirty = false;
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
        if (editorFile != null) {
            final String title = editorFile.getName();
            actionBar.setTitle(title);
            setTaskDescription(new ActivityManager.TaskDescription(title));
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

    private void changeShellStatus(@NonNull MenuItem item) {
        if (item.isChecked()) {
            EditorShell.setEnabled(this, false);
            item.setChecked(false);
        } else {
            EditorShell.setEnabled(this, true);
            item.setChecked(true);
        }
    }

    /* Commands */

    private void changeCommandBarVisibility(@NonNull MenuItem item) {
        if (item.isChecked()) {
            // Hide
            commandBar.setVisibility(View.GONE);
            item.setChecked(false);
            showCommandBar = false;
        } else {
            // Show
            commandBar.setVisibility(View.VISIBLE);
            item.setChecked(true);
            showCommandBar = true;
        }
    }

    private void runCurrentCommand() {
        final String input = commandField.getText().toString();
        final Optional<EditorCommand> commandOpt = editorCommandParser.parse(input);
        if (commandOpt.isPresent()) {
            final EditorCommand command = commandOpt.get();
            if (command instanceof EditorCommand.Find) {
                runFindCommand((EditorCommand.Find) command);
            } else if (command instanceof EditorCommand.Delete) {
                runDeleteCommand((EditorCommand.Delete) command);
            } else if (command instanceof EditorCommand.SubstituteAll) {
                runSubstituteAllCommand((EditorCommand.SubstituteAll) command);
            } else if (command instanceof EditorCommand.SubstituteFirst) {
                runSubstituteFirstCommand((EditorCommand.SubstituteFirst) command);
            } else {
                showTmpErrorMessage(getString(R.string.editor_command_not_implemented));
            }
        } else {
            showTmpErrorMessage(getString(R.string.editor_command_unknown));
        }
    }

    private void runFindCommand(@NonNull EditorCommand.Find command) {
        final String content = textEditorView.getText().toString();
        final int selectionEnd = textEditorView.getSelectionEnd();
        final int cursor = selectionEnd == -1
                ? textEditorView.getSelectionStart()
                : selectionEnd;
        TaskExecutor.runTask(new TextFindTask(command.getToFind(), content, cursor),
                range -> {
                    textEditorView.requestFocus();
                    textEditorView.setSelection(range.getLower(), range.getUpper());
                },
                () -> showTmpErrorMessage(getString(R.string.editor_command_find_none)));
    }

    private void runDeleteCommand(@NonNull EditorCommand.Delete command) {
        final String content = textEditorView.getText().toString();
        TaskExecutor.runTask(new TextDeleteTask(command.getToDelete(), content),
                textEditorView::setText);
    }

    private void runSubstituteAllCommand(@NonNull EditorCommand.SubstituteAll command) {
        final String content = textEditorView.getText().toString();
        TaskExecutor.runTask(new TextSubstituteAllTask(command.getToFind(),
                        command.getReplaceWith(), content),
                textEditorView::setText);
    }

    private void runSubstituteFirstCommand(@NonNull EditorCommand.SubstituteFirst command) {
        final String content = textEditorView.getText().toString();
        final int cursor = textEditorView.getSelectionStart();
        TaskExecutor.runTask(new TextSubstituteFirstTask(command.getToFind(),
                command.getReplaceWith(), content, command.getCount(), cursor),
                textEditorView::setText);
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
                .setNeutralButton(android.R.string.cancel,
                        (d, which) -> d.dismiss())
                .show();
    }

    private void showCommandHelpMessage() {
        new AlertDialog.Builder(this, R.style.AppTheme)
                .setTitle(R.string.editor_menu_command)
                .setMessage(R.string.editor_command_help)
                .setPositiveButton(R.string.editor_action_dismiss,
                        (d, which) -> d.dismiss())
                .show();
    }

    private void showOpenErrorMessage() {
        showFatalErrorMessage(getString(R.string.editor_error_open));
    }

    private void showReadErrorMessage(@NonNull EditorFile editorFile) {
        showFatalErrorMessage(getString(R.string.editor_error_read, editorFile.getName()));
    }

    private void showWriteErrorMessage(@NonNull EditorFile editorFile) {
        showFatalErrorMessage(getString(R.string.editor_save_failed, editorFile.getName()));
    }

    private void showFatalErrorMessage(@NonNull CharSequence message) {
        new TipDialog.Builder(this)
                .setMessage(message)
                .setIcon(eu.bbllw8.anemo.tip.R.drawable.tip_ic_error)
                .setDismissOnTouchOutside(true)
                .setOnDismissListener(this::finish)
                .show();
    }

    private void showTmpErrorMessage(@NonNull CharSequence message) {
        final TipDialog dialog = new TipDialog.Builder(this)
                .setMessage(message)
                .setIcon(eu.bbllw8.anemo.tip.R.drawable.tip_ic_error)
                .setDismissOnTouchOutside(true)
                .show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }, 1000L);
    }
}
