/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.config;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Config {

    private Config() {
    }

    @IntDef(value = {
            Size.SMALL,
            Size.MEDIUM,
            Size.LARGE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Size {
        int SMALL = 0;
        int MEDIUM = 1;
        int LARGE = 2;
    }

    @IntDef(value = {
            Style.MONO,
            Style.SANS,
            Style.SERIF,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Style {
        int MONO = 0;
        int SANS = 1;
        int SERIF = 2;
    }

    public static final int DEFAULT_SIZE = Size.MEDIUM;
    public static final int DEFAULT_STYLE = Style.MONO;
    public static final boolean DEFAULT_AUTO_PAIR = true;
    public static final boolean DEFAULT_SHOW_COMMAND_BAR = false;
}
