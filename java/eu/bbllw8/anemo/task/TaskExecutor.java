/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.task;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public final class TaskExecutor {
    private static final String TAG = "TaskExecutor";

    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    private TaskExecutor() {
    }

    public static <T> void runTask(@NonNull @WorkerThread Callable<T> callable,
                                   @NonNull Consumer<T> consumer) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final FutureTask<T> future = new FutureTask<T>(callable) {
            @Override
            protected void done() {
                try {
                    final T result = get(1, TimeUnit.MINUTES);
                    handler.post(() -> consumer.accept(result));
                } catch (InterruptedException e) {
                    Log.w(TAG, e);
                } catch (ExecutionException | TimeoutException e) {
                    throw new RuntimeException("An error occurred while executing task",
                            e.getCause());
                }
            }
        };
        executor.execute(future);
    }
}
