/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.auto;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class AutoPair implements TextWatcher {

    private static final Map<Character, String> PAIR_MAP = new HashMap<>();

    static {
        PAIR_MAP.put('\'', "'");
        PAIR_MAP.put('"', "\"");
        PAIR_MAP.put('`', "`");
        PAIR_MAP.put('(', ")");
        PAIR_MAP.put('[', "]");
        PAIR_MAP.put('{', "}");
    }

    @NonNull
    private final Supplier<Editable> editableTextSupplier;
    private boolean enabled;
    private boolean trackChanges;

    public AutoPair(@NonNull Supplier<Editable> editableTextSupplier) {
        this.editableTextSupplier = editableTextSupplier;
        this.enabled = false;
        this.trackChanges = true;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (enabled && trackChanges) {
            if (count == 1) {
                final int i = start + 1;
                final char typed = s.subSequence(start, i).charAt(0);
                final String closePair = PAIR_MAP.get(typed);

                if (closePair != null) {
                    final Editable editable = editableTextSupplier.get();
                    trackChanges = false;
                    editable.insert(i, closePair);
                    trackChanges = true;

                    Selection.setSelection(editable, i);
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
