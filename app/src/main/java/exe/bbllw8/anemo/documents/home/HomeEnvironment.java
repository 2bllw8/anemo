/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.home;

import android.content.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HomeEnvironment {
    public static final String AUTHORITY = "exe.bbllw8.anemo.documents";

    public static final String ROOT = "anemo";
    public static final String ROOT_DOC_ID = "root";

    private final Path baseDir;

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
        if (!Files.exists(baseDir)) {
            Files.createDirectory(baseDir);
        } else if (!Files.isDirectory(baseDir)) {
            throw new IOException(baseDir + " is not a directory");
        }
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public boolean isRoot(Path path) {
        return baseDir.equals(path);
    }
}
