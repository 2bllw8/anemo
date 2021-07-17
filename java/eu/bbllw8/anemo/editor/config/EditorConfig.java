/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.config;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Consumer;

public final class EditorConfig {
    private static final String CONFIG_PREFERENCES = "editor_config";

    private static final String KEY_SIZE = "text_size";
    private static final String KEY_STYLE = "text_style";
    private static final String KEY_AUTO_PAIR = "auto_pair";
    private static final String KEY_SHOW_COMMAND_BAR = "show_command_bar";

    private static final String CMD_KEY_AUTO_PAIR = "pair";
    private static final String CMD_KEY_SHOW_CMD_BAR = "commands";
    private static final String CMD_KEY_TEXT_SIZE = "size";
    private static final String CMD_KEY_TEXT_STYLE = "style";

    private static final String CMD_VAL_DEFAULT = "default";
    private static final String CMD_VAL_OFF = "off";
    private static final String CMD_VAL_ON = "on";
    private static final String CMD_VAL_SIZE_LARGE = "large";
    private static final String CMD_VAL_SIZE_MEDIUM = "medium";
    private static final String CMD_VAL_SIZE_SMALL = "small";
    private static final String CMD_VAL_STYLE_MONO = "mono";
    private static final String CMD_VAL_STYLE_SANS = "sans";
    private static final String CMD_VAL_STYlE_SERIF = "serif";

    @Nullable
    private final EditorConfigListener configListener;

    @NonNull
    private final SharedPreferences preferences;
    private boolean ready;

    public EditorConfig(@NonNull Context context,
                        @Nullable EditorConfigListener listener) {
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

    public boolean getAutoPairEnabled() {
        return preferences.getBoolean(KEY_AUTO_PAIR, Config.DEFAULT_AUTO_PAIR);
    }

    public void setAutoPairEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_AUTO_PAIR, enabled)
                .apply();
        if (configListener != null && ready) {
            configListener.onAutoPairEnabledChanged(enabled);
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

    public boolean setByKeyVal(@NonNull String key,
                               @NonNull String value) {
        switch (key) {
            case CMD_KEY_AUTO_PAIR:
                return applyBooleanCommand(this::setAutoPairEnabled,
                        value, Config.DEFAULT_AUTO_PAIR);
            case CMD_KEY_SHOW_CMD_BAR:
                return applyBooleanCommand(this::setShowCommandBar,
                        value, Config.DEFAULT_SHOW_COMMAND_BAR);
            case CMD_KEY_TEXT_SIZE:
                return applyTextSizeCommand(value);
            case CMD_KEY_TEXT_STYLE:
                return applyTextStyleCommand(value);
            default:
                return false;
        }
    }

    private boolean applyBooleanCommand(@NonNull Consumer<Boolean> applier,
                                        @NonNull String value,
                                        boolean defaultValue) {
        switch (value) {
            case CMD_VAL_DEFAULT:
                applier.accept(defaultValue);
                return true;
            case CMD_VAL_ON:
                applier.accept(true);
                return true;
            case CMD_VAL_OFF:
                applier.accept(false);
                return true;
            default:
                return false;
        }
    }

    private boolean applyTextSizeCommand(@NonNull String value) {
        switch (value) {
            case CMD_VAL_DEFAULT:
                setTextSize(Config.DEFAULT_SIZE);
                return true;
            case CMD_VAL_SIZE_LARGE:
                setTextSize(Config.Size.LARGE);
                return true;
            case CMD_VAL_SIZE_MEDIUM:
                setTextSize(Config.Size.MEDIUM);
                return true;
            case CMD_VAL_SIZE_SMALL:
                setTextSize(Config.Size.SMALL);
                return true;
            default:
                return false;
        }
    }

    private boolean applyTextStyleCommand(@NonNull String value) {
        switch (value) {
            case CMD_VAL_DEFAULT:
                setTextStyle(Config.DEFAULT_STYLE);
                return true;
            case CMD_VAL_STYLE_MONO:
                setTextStyle(Config.Style.MONO);
                return true;
            case CMD_VAL_STYLE_SANS:
                setTextStyle(Config.Style.SANS);
                return true;
            case CMD_VAL_STYlE_SERIF:
                setTextStyle(Config.Style.SERIF);
                return true;
            default:
                return false;
        }
    }
}
