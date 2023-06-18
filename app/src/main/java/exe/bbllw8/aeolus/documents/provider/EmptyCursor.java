/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.aeolus.documents.provider;

import android.database.AbstractCursor;

public final class EmptyCursor extends AbstractCursor {

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public String[] getColumnNames() {
        return new String[0];
    }

    @Override
    public String getString(int column) {
        return null;
    }

    @Override
    public short getShort(int column) {
        return 0;
    }

    @Override
    public int getInt(int column) {
        return 0;
    }

    @Override
    public long getLong(int column) {
        return 0L;
    }

    @Override
    public float getFloat(int column) {
        return 0f;
    }

    @Override
    public double getDouble(int column) {
        return 0.0;
    }

    @Override
    public boolean isNull(int column) {
        return true;
    }
}
