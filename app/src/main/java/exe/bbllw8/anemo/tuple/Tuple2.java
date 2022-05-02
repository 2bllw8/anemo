/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.tuple;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Tuple2<A, B> {
    public final A _1;
    public final B _2;

    public Tuple2(A _1, B _2) {
        this._1 = _1;
        this._2 = _2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Tuple2) {
            Tuple2<?, ?> other = (Tuple2<?, ?>) o;
            return Objects.equals(_1, other._1) && Objects.equals(_2, other._2);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2);
    }

    @NonNull
    @Override
    public String toString() {
        return "(" + _1 + ", " + _2 + ")";
    }
}
