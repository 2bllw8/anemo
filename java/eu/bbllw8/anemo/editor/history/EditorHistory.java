/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.history;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;

import androidx.annotation.NonNull;

import java.util.function.Supplier;

public final class EditorHistory implements TextWatcher {

    @NonNull
    private final Supplier<Editable> editableTextSupplier;

    @NonNull
    private final HistoryBuffer buffer;

    private CharSequence contentBefore = "";
    private boolean trackChanges = true;

    public EditorHistory(@NonNull Supplier<Editable> editableTextSupplier,
                         int bufferSize) {
        this.editableTextSupplier = editableTextSupplier;
        this.buffer = new HistoryBuffer(bufferSize);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (trackChanges) {
            contentBefore = s.subSequence(start, start + count);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (trackChanges) {
            final CharSequence contentAfter = s.subSequence(start, start + count);
            buffer.push(new HistoryEntry(contentBefore, contentAfter, start));
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    public void undo() {
        buffer.pop().ifPresent(entry -> {
            final Editable editable = editableTextSupplier.get();

            final CharSequence before = entry.getBefore();
            final CharSequence after = entry.getAfter();
            final int start = entry.getStart();
            final int end = start + (after == null ? 0 : after.length());

            trackChanges = false;
            editable.replace(start, end, before);
            trackChanges = true;

            // Remove underline spans (from keyboard suggestions)
            for (final Object span : editable.getSpans(start,
                    editable.length(),
                    UnderlineSpan.class)) {
                editable.removeSpan(span);
            }

            Selection.setSelection(editable, before == null
                    ? start
                    : start + before.length());

        });
    }
}
