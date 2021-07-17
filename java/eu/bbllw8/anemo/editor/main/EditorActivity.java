/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.main;

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
import android.text.PrecomputedText;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import eu.bbllw8.anemo.editor.auto.AutoPair;
import eu.bbllw8.anemo.editor.commands.EditorCommand;
import eu.bbllw8.anemo.editor.commands.EditorCommandParser;
import eu.bbllw8.anemo.editor.commands.EditorCommandsExecutor;
import eu.bbllw8.anemo.editor.commands.task.DeleteAllCommandTask;
import eu.bbllw8.anemo.editor.commands.task.DeleteFirstCommandTask;
import eu.bbllw8.anemo.editor.commands.task.FindCommandTask;
import eu.bbllw8.anemo.editor.commands.task.SubstituteAllCommandTask;
import eu.bbllw8.anemo.editor.commands.task.SubstituteFirstCommandTask;
import eu.bbllw8.anemo.editor.config.Config;
import eu.bbllw8.anemo.editor.config.EditorConfig;
import eu.bbllw8.anemo.editor.config.EditorConfigListener;
import eu.bbllw8.anemo.editor.history.EditorHistory;
import eu.bbllw8.anemo.editor.io.EditorFile;
import eu.bbllw8.anemo.editor.io.EditorFileLoaderTask;
import eu.bbllw8.anemo.editor.io.EditorFileReaderTask;
import eu.bbllw8.anemo.editor.io.EditorFileWriterTask;
import eu.bbllw8.anemo.task.TaskExecutor;
import eu.bbllw8.anemo.tip.TipDialog;

public final class EditorActivity extends Activity implements
        EditorConfigListener,
        EditorCommandsExecutor,
        TextWatcher {
    private static final String KEY_EDITOR_FILE = "editor_file";
    private static final String KEY_HISTORY_STATE = "editor_history";
    private static final String TYPE_PLAIN_TEXT = "text/plain";
    private static final int REQUEST_CREATE_FILE_AND_QUIT = 10;
    private static final int REQUEST_CREATE_FILE = 11;
    private static final int REQUEST_OPEN_FILE = 12;

    private boolean dirty = false;
    private boolean alwaysAllowSave = false;

    @Nullable
    private EditorFile editorFile = null;

    private ActionBar actionBar;
    private View loadView;
    private TextView summaryView;
    private TextEditorView textEditorView;
    private ViewGroup commandBar;
    private EditText commandField;

    private EditorConfig editorConfig;
    private EditorHistory editorHistory;
    private AutoPair autoPair;

    private final EditorCommandParser editorCommandParser = new EditorCommandParser();

    private MenuItem undoMenuItem;
    private MenuItem saveMenuItem;
    private MenuItem sizeSmallMenuItem;
    private MenuItem sizeMediumMenuItem;
    private MenuItem sizeLargeMenuItem;
    private MenuItem styleMonoMenuItem;
    private MenuItem styleSansMenuItem;
    private MenuItem styleSerifMenuItem;
    private MenuItem autoPairMenuItem;
    private MenuItem showCommandBarMenuItem;

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

        editorConfig = new EditorConfig(this, this);
        editorHistory = new EditorHistory(textEditorView::getEditableText,
                getResources().getInteger(R.integer.editor_history_buffer_size));
        autoPair = new AutoPair(textEditorView::getEditableText);

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
        if (savedInstanceState == null) {
            if (Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())) {
                final String textInput = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT);
                setTextContent(textInput);
            } else {
                final Uri inputUri = intent.getData();
                if (inputUri == null) {
                    registerTextListeners();
                    loadConfig();
                } else {
                    loadFile(inputUri, intent.getType());
                }
            }
        } else {
            registerTextListeners();
            loadConfig();
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
            undoMenuItem = menu.findItem(R.id.editorUndo);
            saveMenuItem = menu.findItem(R.id.editorSave);
            sizeSmallMenuItem = menu.findItem(R.id.editorFontSizeSmall);
            sizeMediumMenuItem = menu.findItem(R.id.editorFontSizeMedium);
            sizeLargeMenuItem = menu.findItem(R.id.editorFontSizeLarge);
            styleMonoMenuItem = menu.findItem(R.id.editorFontStyleMono);
            styleSansMenuItem = menu.findItem(R.id.editorFontStyleSans);
            styleSerifMenuItem = menu.findItem(R.id.editorFontStyleSerif);
            autoPairMenuItem = menu.findItem(R.id.editorAutoPair);
            showCommandBarMenuItem = menu.findItem(R.id.editorShowCommandBar);
            final MenuItem showShellMenuItem = menu.findItem(R.id.editorShowShell);

            switch (editorConfig.getTextSize()) {
                case Config.Size.LARGE:
                    sizeLargeMenuItem.setChecked(true);
                    break;
                case Config.Size.MEDIUM:
                    sizeMediumMenuItem.setChecked(true);
                    break;
                case Config.Size.SMALL:
                    sizeSmallMenuItem.setChecked(true);
                    break;
            }
            switch (editorConfig.getTextStyle()) {
                case Config.Style.MONO:
                    styleMonoMenuItem.setChecked(true);
                    break;
                case Config.Style.SANS:
                    styleSansMenuItem.setChecked(true);
                    break;
                case Config.Style.SERIF:
                    styleSerifMenuItem.setChecked(true);
                    break;
            }
            autoPairMenuItem.setChecked(editorConfig.getAutoPairEnabled());
            showCommandBarMenuItem.setChecked(editorConfig.getShowCommandBar());
            showShellMenuItem.setChecked(EditorShell.isEnabled(this));

            // If always dirty (snippet) always allow
            saveMenuItem.setEnabled(alwaysAllowSave);

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
        } else if (id == R.id.editorFontSizeSmall) {
            editorConfig.setTextSize(Config.Size.SMALL);
            return true;
        } else if (id == R.id.editorFontSizeMedium) {
            editorConfig.setTextSize(Config.Size.MEDIUM);
            return true;
        } else if (id == R.id.editorFontSizeLarge) {
            editorConfig.setTextSize(Config.Size.LARGE);
            return true;
        } else if (id == R.id.editorFontStyleMono) {
            editorConfig.setTextStyle(Config.Style.MONO);
            return true;
        } else if (id == R.id.editorFontStyleSans) {
            editorConfig.setTextStyle(Config.Style.SANS);
            return true;
        } else if (id == R.id.editorFontStyleSerif) {
            editorConfig.setTextStyle(Config.Style.SERIF);
            return true;
        } else if (id == R.id.editorAutoPair) {
            editorConfig.setAutoPairEnabled(!item.isChecked());
            return true;
        } else if (id == R.id.editorShowCommandBar) {
            editorConfig.setShowCommandBar(!item.isChecked());
            return true;
        } else if (id == R.id.editorNew) {
            openNewWindow();
            return true;
        } else if (id == R.id.editorOpen) {
            openFileSelector();
            return true;
        } else if (id == R.id.editorShowShell) {
            EditorShell.setEnabled(this, !item.isChecked());
            item.setChecked(!item.isChecked());
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
        final int maxSize = getResources().getInteger(R.integer.editor_max_file_size);
        TaskExecutor.runTask(new EditorFileReaderTask(getContentResolver(), editorFile, maxSize),
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

    private void openNewWindow() {
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
        String title = textEditorView.getText().toString();
        if (title.length() > 20) {
            title = title.substring(0, 20);
        }
        title = title.replace('\n', ' ') + ".txt";

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

    private void setTextContent(@NonNull String content) {
        loadConfig();
        setContentInView(content);
        alwaysAllowSave = true;
    }

    private void setContent(@NonNull EditorFile editorFile, @NonNull String content) {
        this.editorFile = editorFile;

        updateTitle();

        loadView.setVisibility(View.GONE);
        textEditorView.setVisibility(View.VISIBLE);

        loadConfig();
        setContentInView(content);
    }

    private void setContentInView(@NonNull String content) {
        final PrecomputedText.Params params = textEditorView.getTextMetricsParams();
        final Reference<TextEditorView> editorViewRef = new WeakReference<>(textEditorView);
        TaskExecutor.submit(() -> {
            final TextEditorView ev = editorViewRef.get();
            if (ev == null) {
                return;
            }
            final PrecomputedText preCompText = PrecomputedText.create(content, params);
            ev.post(() -> {
                final TextEditorView ev2 = editorViewRef.get();
                if (ev2 == null) {
                    return;
                }
                ev2.setText(preCompText);

                // Set listener after the contents
                registerTextListeners();
            });
        });
    }

    private void saveContents(boolean quitWhenSaved) {
        if (dirty || alwaysAllowSave) {
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

            // We don't need save to be forcefully enabled anymore
            alwaysAllowSave = false;
            saveMenuItem.setEnabled(false);
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
            textEditorView.addTextChangedListener(autoPair);
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

    private void loadConfig() {
        onTextSizeChanged(editorConfig.getTextSize());
        onTextStyleChanged(editorConfig.getTextStyle());
        onAutoPairEnabledChanged(editorConfig.getAutoPairEnabled());
        onShowCommandBarChanged(editorConfig.getShowCommandBar());
        editorConfig.setReady();
    }

    @Override
    public void onTextSizeChanged(@Config.Size int newSize) {
        final int newTextSizeRes;
        final MenuItem menuItem;
        switch (newSize) {
            case Config.Size.SMALL:
                newTextSizeRes = R.dimen.editorFontSizeSmall;
                menuItem = sizeSmallMenuItem;
                break;
            case Config.Size.LARGE:
                newTextSizeRes = R.dimen.editorFontSizeLarge;
                menuItem = sizeLargeMenuItem;
                break;
            case Config.Size.MEDIUM:
            default:
                newTextSizeRes = R.dimen.editorFontSizeMedium;
                menuItem = sizeMediumMenuItem;
                break;
        }
        textEditorView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(newTextSizeRes));
        if (menuItem != null) {
            menuItem.setChecked(true);
        }
    }

    @Override
    public void onTextStyleChanged(@Config.Style int newStyle) {
        final Typeface newTypeface;
        final MenuItem menuItem;
        switch (newStyle) {
            case Config.Style.SANS:
                newTypeface = Typeface.SANS_SERIF;
                menuItem = styleSansMenuItem;
                break;
            case Config.Style.SERIF:
                newTypeface = Typeface.SERIF;
                menuItem = styleSerifMenuItem;
                break;
            case Config.Style.MONO:
            default:
                newTypeface = Typeface.MONOSPACE;
                menuItem = styleMonoMenuItem;
                break;
        }
        textEditorView.setTypeface(newTypeface);
        if (menuItem != null) {
            menuItem.setChecked(true);
        }
    }

    @Override
    public void onAutoPairEnabledChanged(boolean enabled) {
        autoPair.setEnabled(enabled);
        if (autoPairMenuItem != null) {
            autoPairMenuItem.setChecked(enabled);
        }
    }

    @Override
    public void onShowCommandBarChanged(boolean show) {
        commandBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (showCommandBarMenuItem != null) {
            showCommandBarMenuItem.setChecked(show);
        }
    }

    /* Commands */

    private void runCurrentCommand() {
        final String input = commandField.getText().toString();
        final boolean success = editorCommandParser.parse(input)
                .map(this::runCommand)
                .orElse(false);
        if (!success) {
            showTmpMessage(getString(R.string.editor_command_unknown),
                    eu.bbllw8.anemo.tip.R.drawable.tip_ic_error);
        }
    }

    @Override
    public void runFindCommand(@NonNull EditorCommand.Find command) {
        final String content = textEditorView.getText().toString();
        final int selectionEnd = textEditorView.getSelectionEnd();
        final int cursor = selectionEnd == -1
                ? textEditorView.getSelectionStart()
                : selectionEnd;
        TaskExecutor.runTask(new FindCommandTask(command.getToFind(), content, cursor),
                range -> {
                    textEditorView.requestFocus();
                    textEditorView.setSelection(range.getLower(), range.getUpper());
                },
                () -> showTmpMessage(getString(R.string.editor_command_find_none),
                        eu.bbllw8.anemo.tip.R.drawable.tip_ic_error));
    }

    @Override
    public void runDeleteAllCommand(@NonNull EditorCommand.DeleteAll command) {
        final String content = textEditorView.getText().toString();
        TaskExecutor.runTask(new DeleteAllCommandTask(command.getToDelete(), content),
                textEditorView::setText);
    }

    @Override
    public void runDeleteFirstCommand(@NonNull EditorCommand.DeleteFirst command) {
        final String content = textEditorView.getText().toString();
        final int cursor = textEditorView.getSelectionStart();
        TaskExecutor.runTask(new DeleteFirstCommandTask(command.getToDelete(),
                content, command.getCount(), cursor),
                textEditorView::setText);
    }

    @Override
    public void runSetCommand(@NonNull EditorCommand.Set command) {
        final boolean success = editorConfig.setByKeyVal(command.getKey(), command.getValue());
        if (success) {
            showTmpMessage(getString(R.string.editor_command_set_success),
                    eu.bbllw8.anemo.tip.R.drawable.tip_ic_success);
        } else {
            showTmpMessage(getString(R.string.editor_command_unknown),
                    eu.bbllw8.anemo.tip.R.drawable.tip_ic_error);
        }
    }

    @Override
    public void runSubstituteAllCommand(@NonNull EditorCommand.SubstituteAll command) {
        final String content = textEditorView.getText().toString();
        TaskExecutor.runTask(new SubstituteAllCommandTask(command.getToFind(),
                        command.getReplaceWith(), content),
                textEditorView::setText);
    }

    @Override
    public void runSubstituteFirstCommand(@NonNull EditorCommand.SubstituteFirst command) {
        final String content = textEditorView.getText().toString();
        final int cursor = textEditorView.getSelectionStart();
        TaskExecutor.runTask(new SubstituteFirstCommandTask(command.getToFind(),
                command.getReplaceWith(), content, command.getCount(), cursor),
                textEditorView::setText);
    }

    /* Dirty */

    private void setNotDirty() {
        if (dirty) {
            dirty = false;
            undoMenuItem.setEnabled(false);
            saveMenuItem.setEnabled(alwaysAllowSave);
        }
    }

    private void setDirty() {
        if (!dirty) {
            dirty = true;
            undoMenuItem.setEnabled(true);
            saveMenuItem.setEnabled(true);
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
                : '"' + editorFile.getName() + '"';

        new AlertDialog.Builder(this, R.style.DialogTheme)
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
        new AlertDialog.Builder(this, R.style.DialogTheme)
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

    private void showTmpMessage(@NonNull CharSequence message,
                                @DrawableRes int icon) {
        final TipDialog dialog = new TipDialog.Builder(this)
                .setMessage(message)
                .setIcon(icon)
                .setDismissOnTouchOutside(true)
                .show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }, 1000L);
    }
}
