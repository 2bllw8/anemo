/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.password;

import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;

public interface TextListener extends TextWatcher {

    @Override
    default void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    default void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    default void afterTextChanged(Editable s) {
        onTextChanged(s.toString());
    }

    void onTextChanged(@NonNull String text);
}
