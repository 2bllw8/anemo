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

    private MarkdownFormatter() {
    }

    @NonNull
    public static CharSequence format(@NonNull CharSequence text) {
        final SpannableStringBuilder sb = new SpannableStringBuilder();
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
                                if (i + 2 < n) {
                                    final CharSequence h3Text = text.subSequence(i + 2, j)
                                            .toString()
                                            .trim();
                                    sb.append(h3Text,
                                            new RelativeSizeSpan(1.25f),
                                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                }
                            } else {
                                if (i + 1 < n) {
                                    final CharSequence h2Text = text.subSequence(i + 1, j)
                                            .toString()
                                            .trim();
                                    sb.append(h2Text,
                                            new RelativeSizeSpan(1.5f),
                                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                }
                            }
                        } else {
                            if (i < n) {
                                final CharSequence h1Text = text.subSequence(i, j)
                                        .toString()
                                        .trim();
                                sb.append(h1Text,
                                        new RelativeSizeSpan(1.75f),
                                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                            }
                        }
                        sb.append('\n');
                        i = j;
                        continue;
                    }
                    case '>': {
                        final int j = nextEol(text, n, i);
                        final CharSequence quotedText = text.subSequence(i, j)
                                .toString()
                                .trim();
                        sb.append(quotedText,
                                new TypefaceSpan(Typeface.SERIF),
                                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        sb.append('\n');
                        i = j;
                        continue;
                    }
                }
            }

            if (c == '\n') {
                wasLastLine = true;
                sb.append('\n');
            } else {
                wasLastLine = false;
                switch (c) {
                    case '*':
                    case '_': {
                        if (peek(text, n, i) == c) {
                            final int j = nextMatch(text, c, n, i + 1);
                            if (peek(text, n, j) == c) {
                                if (i + 1 < n) {
                                    sb.append(text.subSequence(i + 1, j - 1),
                                            new StyleSpan(Typeface.BOLD),
                                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                }
                                i = j + 1;
                            } else {
                                sb.append(c);
                            }
                        } else {
                            final int j = nextMatch(text, c, n, i);
                            if (i < n) {
                                sb.append(text.subSequence(i, j - 1),
                                        new StyleSpan(Typeface.ITALIC),
                                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                            }
                            i = j;
                        }
                    }
                    break;
                    case '`': {
                        final int j = nextMatch(text, '`', n, i);
                        sb.append(text.subSequence(i, j - 1),
                                new TypefaceSpan(Typeface.MONOSPACE),
                                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        i = j;
                    }
                    break;
                    case '~': {
                        final int j = nextMatch(text, '~', n, i);
                        sb.append(text.subSequence(i, j - 1),
                                new StrikethroughSpan(),
                                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        i = j;
                    }
                    break;
                    default: {
                        sb.append(c);
                    }
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
