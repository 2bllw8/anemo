/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.provider;

import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public final class PathUtils {
    private static final String MIME_TYPE_DEFAULT = "application/octet-stream";

    private PathUtils() {
    }

    /**
     * Mutate the given filename to make it valid for a FAT filesystem, replacing any invalid
     * characters with "_".
     * <p>
     * Based on {@code android.os.FileUtils#buildUniqueFile#buildValidFilename}
     */
    public static String buildValidFileName(String name) {
        if (TextUtils.isEmpty(name) || ".".equals(name)) {
            return "(invalid)";
        }

        final StringBuilder res = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (isValidFatFilenameChar(c)) {
                res.append(c);
            } else {
                res.append('_');
            }
        }
        return res.toString();
    }

    /**
     * Generates a unique file name under the given parent directory, keeping any extension intact.
     * <p>
     * Based on {@code android.os.FileUtils#buildUniqueFile}
     */
    public static Path buildUniquePath(Path parent, String displayName)
            throws FileNotFoundException {
        final String name;
        final String ext;

        // Extract requested extension from display name
        final int lastDot = displayName.lastIndexOf('.');
        if (lastDot >= 0) {
            name = displayName.substring(0, lastDot);
            ext = displayName.substring(lastDot + 1);
        } else {
            name = displayName;
            ext = null;
        }

        return buildUniquePathWithExtension(parent, name, ext);
    }

    /**
     * Generates a unique file name under the given parent directory. If the display name doesn't
     * have an extension that matches the requested MIME type, the default extension for that MIME
     * type is appended. If a file already exists, the name is appended with a numerical value to
     * make it unique.
     * <p>
     * For example, the display name 'example' with 'text/plain' MIME might produce 'example.txt' or
     * 'example (1).txt', etc.
     * <p>
     * Based on {@code android.os.FileUtils#buildUniqueFile#buildUniqueFile}
     */
    public static Path buildUniquePath(Path parent, String mimeType, String displayName)
            throws FileNotFoundException {
        final String[] parts = splitFileName(mimeType, displayName);
        return buildUniquePathWithExtension(parent, parts[0], parts[1]);
    }

    /**
     * Splits file name into base name and extension. If the display name doesn't have an extension
     * that matches the requested MIME type, the extension is regarded as a part of filename and
     * default extension for that MIME type is appended.
     * <p>
     * Based on {@code android.os.FileUtils#buildUniqueFile#splitFileName}
     */
    public static String[] splitFileName(String mimeType, String displayName) {
        String name;
        String ext;

        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
            name = displayName;
            ext = null;
        } else {
            String mimeTypeFromExt;

            // Extract requested extension from display name
            final int lastDot = displayName.lastIndexOf('.');
            if (lastDot >= 0) {
                name = displayName.substring(0, lastDot);
                ext = displayName.substring(lastDot + 1);
                mimeTypeFromExt = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(ext.toLowerCase());
            } else {
                name = displayName;
                ext = null;
                mimeTypeFromExt = null;
            }

            if (mimeTypeFromExt == null) {
                mimeTypeFromExt = MIME_TYPE_DEFAULT;
            }

            final String extFromMimeType;
            if (MIME_TYPE_DEFAULT.equals(mimeType)) {
                extFromMimeType = null;
            } else {
                extFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            }

            if (!(Objects.equals(mimeType, mimeTypeFromExt)
                    || Objects.equals(ext, extFromMimeType))) {
                // No match; insist that create file matches requested MIME
                name = displayName;
                ext = extFromMimeType;
            }
        }

        if (ext == null) {
            ext = "";
        }

        return new String[]{name, ext};
    }

    /**
     * Recursively delete a directory.
     */
    public static void deleteContents(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Get the mime-type of a given path document.
     */
    public static String getDocumentType(String documentId, Path path) {
        if (Files.isDirectory(path)) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        } else {
            final int lastDot = documentId.lastIndexOf('.');
            if (lastDot >= 0) {
                final String extension = documentId.substring(lastDot + 1).toLowerCase();
                final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (mime != null) {
                    return mime;
                }
            }
            return MIME_TYPE_DEFAULT;
        }
    }

    private static boolean isValidFatFilenameChar(int c) {
        if (0x00 <= c && c <= 0x1f) {
            return false;
        }
        return switch (c) {
            case '"',
                 '*',
                 '/',
                 ':',
                 '<',
                 '>',
                 '?',
                 '\\',
                 '|',
                 0x7F -> false;
            default -> true;
        };
    }

    private static Path buildUniquePathWithExtension(Path parent, String name, String ext)
            throws FileNotFoundException {
        Path path = buildPath(parent, name, ext);

        // If conflicting path, try adding counter suffix
        int n = 0;
        while (Files.exists(path)) {
            if (n++ >= 32) {
                throw new FileNotFoundException("Failed to create unique file");
            }
            path = buildPath(parent, name + " (" + n + ")", ext);
        }

        return path;
    }

    private static Path buildPath(Path parent, String name, String ext) {
        if (TextUtils.isEmpty(ext)) {
            return parent.resolve(name);
        } else {
            return parent.resolve(name + '.' + ext);
        }
    }
}
