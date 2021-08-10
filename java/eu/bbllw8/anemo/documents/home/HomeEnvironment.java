/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class HomeEnvironment {
    public static final String AUTHORITY = "eu.bbllw8.anemo.documents";

    public static final String ROOT = "anemo";
    public static final String DOCUMENTS = "Documents";
    public static final String PICTURES = "Pictures";
    public static final String MOVIES = "Movies";
    public static final String MUSIC = "Music";

    public static final Set<PosixFilePermission> ATTR_DEFAULT_POSIX
            = PosixFilePermissions.fromString("rwxr--r--");

    private final Path baseDir;
    private final Map<String, Path> defaultDirectories;

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
        baseDir = context.getFilesDir()
                .toPath()
                .resolve(ROOT);

        defaultDirectories = new HashMap<>();
        defaultDirectories.put(DOCUMENTS, baseDir.resolve(DOCUMENTS));
        defaultDirectories.put(PICTURES, baseDir.resolve(PICTURES));
        defaultDirectories.put(MOVIES, baseDir.resolve(MOVIES));
        defaultDirectories.put(MUSIC, baseDir.resolve(MUSIC));

        prepare();
    }

    @NonNull
    public Path getBaseDir() {
        return baseDir;
    }

    @NonNull
    public Optional<Path> getDefaultDirectory(@NonNull String name) {
        return defaultDirectories.containsKey(name)
                ? Optional.ofNullable(defaultDirectories.get(name))
                : Optional.empty();
    }

    public boolean isDefaultDirectory(@NonNull Path path) {
        if (baseDir.equals(path)) {
            return true;
        }

        for (final Path dir : defaultDirectories.values()) {
            if (dir.equals(path)) {
                return true;
            }
        }
        return false;
    }

    public void wipe() throws IOException {
        Files.walkFileTree(baseDir,
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
        if (!Files.exists(baseDir)) {
            Files.createDirectory(baseDir);
        } else if (!Files.isDirectory(baseDir)) {
            throw new IOException(baseDir + " is not a directory");
        }

        for (final Path path : defaultDirectories.values()) {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            } else if (!Files.isDirectory(path)) {
                throw new IOException(path + " is not a directory");
            }
        }
    }
}
