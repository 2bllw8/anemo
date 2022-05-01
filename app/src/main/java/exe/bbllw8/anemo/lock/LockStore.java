/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.lock;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class LockStore implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "LockStore";

    private static final String LOCK_PREFERENCES = "lock_store";
    private static final String KEY_LOCK = "is_locked";
    private static final String KEY_PASSWORD = "password_hash";
    private static final String KEY_AUTO_LOCK = "auto_lock";
    private static final boolean DEFAULT_LOCK_VALUE = false;
    private static final boolean DEFAULT_AUTO_LOCK_VALUE = false;

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final int AUTO_LOCK_JOB_ID = 64;
    // 15
    // minutes
    // in
    // milliseconds
    private static final long AUTO_LOCK_DELAY = 1000L * 60L * 15L;

    private final SharedPreferences preferences;
    private final List<Consumer<Boolean>> listeners = new ArrayList<>();

    private final JobScheduler jobScheduler;
    private final ComponentName autoLockComponent;

    private static volatile LockStore instance;

    public static LockStore getInstance(Context context) {
        if (instance == null) {
            synchronized (LockStore.class) {
                if (instance == null) {
                    instance = new LockStore(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private LockStore(Context context) {
        preferences = context.getSharedPreferences(LOCK_PREFERENCES, Context.MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(this);

        jobScheduler = context.getSystemService(JobScheduler.class);
        autoLockComponent = new ComponentName(context, AutoLockJobService.class);
    }

    @Override
    protected void finalize() throws Throwable {
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        cancelAutoLock();
        super.finalize();
    }

    @Override
    public void onSharedPreferenceChanged(@NonNull SharedPreferences sharedPreferences,
            @Nullable String key) {
        if (KEY_LOCK.equals(key)) {
            onLockChanged();
        }
    }

    public synchronized boolean isLocked() {
        return preferences.getBoolean(KEY_LOCK, DEFAULT_LOCK_VALUE);
    }

    public synchronized void lock() {
        preferences.edit().putBoolean(KEY_LOCK, true).apply();
        cancelAutoLock();
    }

    public synchronized void unlock() {
        preferences.edit().putBoolean(KEY_LOCK, false).apply();
        if (isAutoLockEnabled()) {
            scheduleAutoLock();
        }
    }

    public synchronized boolean setPassword(String password) {
        return hashString(password).map(hashedPwd -> {
            preferences.edit().putString(KEY_PASSWORD, hashedPwd).apply();
            return hashedPwd;
        }).isPresent();
    }

    public synchronized boolean passwordMatch(String password) {
        return hashString(password)
                .map(hashedPwd -> hashedPwd.equals(preferences.getString(KEY_PASSWORD, null)))
                .orElse(false);
    }

    public synchronized boolean hasPassword() {
        return preferences.getString(KEY_PASSWORD, null) != null;
    }

    public synchronized void removePassword() {
        preferences.edit().remove(KEY_PASSWORD).apply();
    }

    public synchronized boolean isAutoLockEnabled() {
        return preferences.getBoolean(KEY_AUTO_LOCK, DEFAULT_AUTO_LOCK_VALUE);
    }

    public synchronized void setAutoLockEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_LOCK, enabled).apply();

        if (!isLocked()) {
            if (enabled) {
                // If
                // auto-lock
                // is
                // enabled
                // while
                // the
                // storage
                // is
                // unlocked,
                // schedule
                // the job
                scheduleAutoLock();
            } else {
                // If
                // auto-lock
                // is
                // disabled
                // while
                // the
                // storage
                // is
                // unlocked,
                // cancel
                // the job
                cancelAutoLock();
            }
        }
    }

    public void addListener(Consumer<Boolean> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<Boolean> listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private void onLockChanged() {
        final boolean newValue = preferences.getBoolean(KEY_LOCK, DEFAULT_LOCK_VALUE);
        listeners.forEach(listener -> listener.accept(newValue));
    }

    private void scheduleAutoLock() {
        jobScheduler.schedule(new JobInfo.Builder(AUTO_LOCK_JOB_ID, autoLockComponent)
                .setMinimumLatency(AUTO_LOCK_DELAY)
                .build());
    }

    private void cancelAutoLock() {
        jobScheduler.cancel(AUTO_LOCK_JOB_ID);
    }

    private Optional<String> hashString(String string) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(string.getBytes());
            return Optional.of(new String(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Couldn't get hash", e);
            return Optional.empty();
        }
    }
}
