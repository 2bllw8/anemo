/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.task;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public final class TaskExecutor {
    private static final String TAG = "TaskExecutor";

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Future<?>> execFutures = new ArrayList<>(4);

    public synchronized <T> void runTask(@WorkerThread Callable<T> callable,
                                         @MainThread Consumer<T> consumer) {
        final Future<T> future = executor.submit(callable);
        execFutures.add(future);
        try {
            final T result = future.get(1, TimeUnit.MINUTES);
            // It's completed, remove to free memory
            execFutures.remove(future);
            // Post result
            handler.post(() -> consumer.accept(result));
        } catch (InterruptedException e) {
            Log.w(TAG, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException("An error occurred while executing task",
                    e.getCause());
        }
    }

    public void terminate() {
        executor.shutdown();
        if (hasUnfinishedTasks()) {
            try {
                if (!executor.awaitTermination(250, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                    //noinspection ResultOfMethodCallIgnored
                    executor.awaitTermination(100, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted", e);
                // (Re-)Cancel if current thread also interrupted
                executor.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean hasUnfinishedTasks() {
        for (final Future<?> future : execFutures) {
            if (!future.isDone()) {
                return true;
            }
        }
        return false;
    }
}
