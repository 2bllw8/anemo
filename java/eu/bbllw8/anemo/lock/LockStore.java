/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.lock;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class LockStore implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOCK_PREFERENCES = "lock_store";
    private static final String KEY_LOCK = "is_locked";
    private static final boolean DEFAULT_LOCK_VALUE = false;

    @NonNull
    private final SharedPreferences preferences;
    @NonNull
    private final AtomicBoolean isLocked;
    @NonNull
    private final SparseArray<Consumer<Boolean>> listeners;

    private static volatile LockStore instance;

    public static LockStore getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (LockStore.class) {
                if (instance == null) {
                    instance = new LockStore(context);
                }
            }
        }
        return instance;
    }

    private LockStore(@NonNull Context context) {
        preferences = context.getSharedPreferences(LOCK_PREFERENCES, Context.MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(this);

        listeners = new SparseArray<>();
        isLocked = new AtomicBoolean(preferences.getBoolean(KEY_LOCK, DEFAULT_LOCK_VALUE));
    }

    @Override
    protected void finalize() throws Throwable {
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        super.finalize();
    }

    @Override
    public void onSharedPreferenceChanged(@NonNull SharedPreferences sharedPreferences,
                                          @Nullable String key) {
        if (KEY_LOCK.equals(key)) {
            final boolean newValue = sharedPreferences.getBoolean(KEY_LOCK, DEFAULT_LOCK_VALUE);

            isLocked.set(newValue);
            for (int i = 0; i < listeners.size(); i++) {
                listeners.valueAt(i).accept(newValue);
            }
        }
    }

    public synchronized boolean isLocked() {
        return isLocked.get();
    }

    public synchronized void lock() {
        preferences.edit()
                .putBoolean(KEY_LOCK, true)
                .apply();
    }

    public synchronized void unlock() {
        preferences.edit()
                .putBoolean(KEY_LOCK, false)
                .apply();
    }

    public int addListener(@NonNull Consumer<Boolean> listener) {
        synchronized (listeners) {
            final int key = listeners.size();
            listeners.append(key, listener);
            return key;
        }
    }

    public void removeListener(int key) {
        synchronized (listeners) {
            listeners.removeAt(key);
        }
    }
}
