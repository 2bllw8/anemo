/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.tuple;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Tuple3<A, B, C> extends Tuple2<A, B> {
    public final C _3;

    public Tuple3(Tuple2<A, B> tup, C _3) {
        this(tup._1, tup._2, _3);
    }

    public Tuple3(A _1, B _2, C _3) {
        super(_1, _2);
        this._3 = _3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Tuple3) {
            final Tuple3<?, ?, ?> other = (Tuple3<?, ?, ?>) o;
            return super.equals(o) && Objects.equals(_3, other._3);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), _3);
    }

    @NonNull
    @Override
    public String toString() {
        return "(" + _1 + ", " + _2 + ", " + _3 + ")";
    }
}
