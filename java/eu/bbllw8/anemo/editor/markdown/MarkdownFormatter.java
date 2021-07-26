/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.markdown;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;

public final class MarkdownFormatter {
    private static final RelativeSizeSpan SPAN_H1 = new RelativeSizeSpan(1.75f);
    private static final RelativeSizeSpan SPAN_H2 = new RelativeSizeSpan(1.5f);
    private static final RelativeSizeSpan SPAN_H3 = new RelativeSizeSpan(1.25f);
    private static final StyleSpan SPAN_BOLD = new StyleSpan(Typeface.BOLD);
    private static final StyleSpan SPAN_ITALICS = new StyleSpan(Typeface.ITALIC);
    private static final StrikethroughSpan SPAN_STRIKE = new StrikethroughSpan();
    private static final TypefaceSpan SPAN_MONO = new TypefaceSpan(Typeface.MONOSPACE);
    private static final TypefaceSpan SPAN_SERIF = new TypefaceSpan(Typeface.SERIF);

    private MarkdownFormatter() {
    }

    @NonNull
    public static CharSequence format(@NonNull CharSequence text) {
        final SpannableStringBuilder sb = new SpannableStringBuilder(text);
        final int n = text.length();
        int i = 0;
        boolean wasLastLine = true;

        while (i < n) {
            final char c = text.charAt(i++);
            if (wasLastLine) {
                switch (c) {
                    case '#': {
                        final int j = nextEol(text, n, i);
                        if (peek(text, n, i) == '#') {
                            if (peek(text, n, i + 1) == '#') {
                                sb.setSpan(SPAN_H3, i - 1, j, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                            } else {
                                sb.setSpan(SPAN_H2, i - 1, j, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                            }
                        } else {
                            sb.setSpan(SPAN_H1, i - 1, j, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        }
                        i = j;
                        continue;
                    }
                    case '>': {
                        final int j = nextEol(text, n, i);
                        sb.setSpan(SPAN_SERIF, i - 1, j, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        i = j;
                        continue;
                    }
                }
            }

            if (c == '\n') {
                wasLastLine = true;
            } else {
                wasLastLine = false;
                switch (c) {
                    case '*':
                    case '_': {
                        if (peek(text, n, i) == c) {
                            final int j = nextMatch(text, c, n, i + 1);
                            if (peek(text, n, j) == c) {
                                sb.setSpan(SPAN_BOLD, i - 1, j + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                            } else {
                                sb.setSpan(SPAN_ITALICS, i + 1, j + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                            }
                            i = j + 1;
                        } else {
                            final int j = nextMatch(text, c, n, i);
                            sb.setSpan(SPAN_ITALICS, i - 1, j, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                            i = j;
                        }
                    }
                    break;
                    case '`': {
                        final int j = nextMatch(text, '`', n, i);
                        sb.setSpan(SPAN_MONO, i - 1, j, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        i = j;
                    }
                    break;
                    case '~': {
                        final int j = nextMatch(text, '`', n, i);
                        sb.setSpan(SPAN_STRIKE, i - 1, j, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        i = j;
                    }
                    break;
                }
            }
        }

        return sb;
    }

    private static char peek(@NonNull CharSequence s, int max, int index) {
        return index < max ? s.charAt(index) : 0;
    }

    private static int nextEol(@NonNull CharSequence s, int max, int from) {
        return nextMatch(s, '\n', max, from);
    }

    private static int nextMatch(@NonNull CharSequence s,
                                 char c,
                                 int max,
                                 int from) {
        int i = from;
        while (i < max) {
            if (s.charAt(i++) == c) {
                return i;
            }
        }
        return i;
    }
}
