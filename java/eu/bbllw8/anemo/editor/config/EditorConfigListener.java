/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.config;

public interface EditorConfigListener {

    void onTextSizeChanged(@Config.Size int newSize);

    void onTextStyleChanged(@Config.Style int newStyle);

    void onAutoPairEnabledChanged(boolean enabled);

    void onShowCommandBarChanged(boolean show);

    void onShowSuggestionChanged(boolean show);
}
