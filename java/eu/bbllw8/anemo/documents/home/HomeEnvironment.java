/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class HomeEnvironment {
    public static final String AUTHORITY = "eu.bbllw8.anemo.documents";

    public static final String ROOT = "anemo";
    public static final String DOCUMENTS = "Documents";
    public static final String PICTURES = "Pictures";
    public static final String MOVIES = "Movies";
    public static final String MUSIC = "Music";

    private final File baseDir;
    private final Map<String, File> defaultDirectories;

    private static volatile HomeEnvironment instance;

    @NonNull
    public static HomeEnvironment getInstance(@NonNull Context context) throws IOException {
        if (instance == null) {
            synchronized (HomeEnvironment.class) {
                if (instance == null) {
                    instance = new HomeEnvironment(context);
                }
            }
        }
        return instance;
    }

    private HomeEnvironment(@NonNull Context context) throws IOException {
        baseDir = new File(context.getFilesDir(), ROOT);

        defaultDirectories = new HashMap<>();
        defaultDirectories.put(DOCUMENTS, new File(baseDir, DOCUMENTS));
        defaultDirectories.put(PICTURES, new File(baseDir, PICTURES));
        defaultDirectories.put(MOVIES, new File(baseDir, MOVIES));
        defaultDirectories.put(MUSIC, new File(baseDir, MUSIC));

        prepare();
    }

    @NonNull
    public File getBaseDir() {
        return baseDir;
    }

    @NonNull
    public Optional<File> getDefaultDirectory(@NonNull String name) {
        return defaultDirectories.containsKey(name)
                ? Optional.ofNullable(defaultDirectories.get(name))
                : Optional.empty();
    }

    public boolean isDefaultDirectory(@NonNull File file) {
        if (baseDir.equals(file)) {
            return true;
        }

        for (final File dir : defaultDirectories.values()) {
            if (dir.equals(file)) {
                return true;
            }
        }
        return false;
    }

    public void wipe() throws IOException {
        Files.walkFileTree(baseDir.toPath(),
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(@NonNull Path file,
                                                     @NonNull BasicFileAttributes attrs)
                            throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(@NonNull Path dir,
                                                              @Nullable IOException exc)
                            throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
        prepare();
    }

    private void prepare() throws IOException {
        if (!baseDir.exists() && !baseDir.mkdir()) {
            throw new IOException("Failed to prepare root directory");
        }

        for (final File d : defaultDirectories.values()) {
            if (!d.exists() && !d.mkdir()) {
                throw new IOException("Failed to prepare " + d.getName() + " directory");
            }
        }
    }
}
