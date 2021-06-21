/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.config;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.bbllw8.anemo.editor.EditorShell;

public final class EditorConfig {
    private static final String CONFIG_PREFERENCES = "editor_config";
    private static final String KEY_SIZE = "text_size";
    private static final String KEY_STYLE = "text_style";
    private static final String KEY_SHOW_COMMAND_BAR = "show_command_bar";

    @NonNull
    private final Context context;
    @Nullable
    private final EditorConfigListener configListener;

    @NonNull
    private final SharedPreferences preferences;
    private boolean ready;

    public EditorConfig(@NonNull Context context,
                        @Nullable EditorConfigListener listener) {
        this.context = context;
        this.configListener = listener;
        this.preferences = context.getSharedPreferences(CONFIG_PREFERENCES, Context.MODE_PRIVATE);
        this.ready = false;
    }

    public void setReady() {
        ready = true;
    }

    @Config.Size
    public int getTextSize() {
        return preferences.getInt(KEY_SIZE, Config.DEFAULT_SIZE);
    }

    public void setTextSize(@Config.Size int size) {
        preferences.edit()
                .putInt(KEY_SIZE, size)
                .apply();
        if (configListener != null && ready) {
            configListener.onTextSizeChanged(size);
        }
    }

    @Config.Style
    public int getTextStyle() {
        return preferences.getInt(KEY_STYLE, Config.DEFAULT_STYLE);
    }

    public void setTextStyle(@Config.Style int style) {
        preferences.edit()
                .putInt(KEY_STYLE, style)
                .apply();
        if (configListener != null && ready) {
            configListener.onTextStyleChanged(style);
        }
    }

    public boolean getShowCommandBar() {
        return preferences.getBoolean(KEY_SHOW_COMMAND_BAR, Config.DEFAULT_SHOW_COMMAND_BAR);
    }

    public void setShowCommandBar(boolean show) {
        preferences.edit()
                .putBoolean(KEY_SHOW_COMMAND_BAR, show)
                .apply();
        if (configListener != null && ready) {
            configListener.onShowCommandBarChanged(show);
        }
    }

    public boolean getShowShell() {
        return EditorShell.isEnabled(context);
    }

    public void setShowShell(boolean show) {
        EditorShell.setEnabled(context, show);
    }
}
