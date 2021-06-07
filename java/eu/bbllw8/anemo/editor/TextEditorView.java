/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.BiConsumer;

public final class TextEditorView extends EditText {

    @Nullable
    private BiConsumer<Integer, Integer> onCursorChanged = null;

    public TextEditorView(@NonNull Context context) {
        super(context);
    }

    public TextEditorView(@NonNull Context context,
                          @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TextEditorView(@NonNull Context context,
                          @Nullable AttributeSet attrs,
                          int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (onCursorChanged != null) {
            onCursorChanged.accept(selStart, selEnd);
        }
        super.onSelectionChanged(selStart, selEnd);
    }

    public void setOnCursorChanged(@NonNull BiConsumer<Integer, Integer> onCursorChanged) {
        this.onCursorChanged = onCursorChanged;
    }
}
