/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.home;

import android.content.Context;

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
    public static final String AUTHORITY = "exe.bbllw8.anemo.documents";

    public static final String ROOT = "anemo";
    public static final String ROOT_DOC_ID = "root";
    public static final String DOCUMENTS = "Documents";
    public static final String PICTURES = "Pictures";
    public static final String MOVIES = "Movies";
    public static final String MUSIC = "Music";

    private final Path baseDir;
    private final Map<String, Path> defaultDirectories;

    private static volatile HomeEnvironment instance;

    public static HomeEnvironment getInstance(Context context) throws IOException {
        if (instance == null) {
            synchronized (HomeEnvironment.class) {
                if (instance == null) {
                    instance = new HomeEnvironment(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private HomeEnvironment(Context context) throws IOException {
        baseDir = context.getFilesDir().toPath().resolve(ROOT);

        defaultDirectories = new HashMap<>(4);
        defaultDirectories.put(DOCUMENTS, baseDir.resolve(DOCUMENTS));
        defaultDirectories.put(PICTURES, baseDir.resolve(PICTURES));
        defaultDirectories.put(MOVIES, baseDir.resolve(MOVIES));
        defaultDirectories.put(MUSIC, baseDir.resolve(MUSIC));

        prepare();
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public Optional<Path> getDefaultDirectory(String name) {
        return defaultDirectories.containsKey(name)
                ? Optional.ofNullable(defaultDirectories.get(name))
                : Optional.empty();
    }

    public boolean isDefaultDirectory(Path path) {
        return baseDir.equals(path) || defaultDirectories.containsValue(path);
    }

    public void wipe() throws IOException {
        Files.walkFileTree(baseDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        prepare();
    }

    private void prepare() throws IOException {
        ensureExists(baseDir);
        for (final Path dir : defaultDirectories.values()) {
            ensureExists(dir);
        }
    }

    private void ensureExists(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectory(dir);
        } else if (!Files.isDirectory(dir)) {
            throw new IOException(dir + " is not a directory");
        }
    }
}
