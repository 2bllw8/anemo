/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.receiver;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import eu.bbllw8.anemo.home.HomeEnvironment;

public final class VideoImporter extends Importer {
    public static final String TYPE_VIDEO = "video/";

    public VideoImporter(@NonNull Context context) throws IOException {
        super(context);
    }

    @NonNull
    @Override
    protected String getTypePrefix() {
        return TYPE_VIDEO;
    }

    @NonNull
    @Override
    protected Optional<File> getDestinationFolder() {
        return Optional.ofNullable(homeEnvironment.getDefaultDirectory(HomeEnvironment.MOVIES));
    }

    @NonNull
    @Override
    protected String getDefaultName() {
        return resources.getString(R.string.receiver_video_default_name,
                dateTimeFormatter.format(LocalDateTime.now()));
    }
}
