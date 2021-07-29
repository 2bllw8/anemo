/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.receiver.importer;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import eu.bbllw8.anemo.documents.home.HomeEnvironment;
import eu.bbllw8.anemo.documents.receiver.R;

public final class AudioImporter extends Importer {
    public static final String TYPE_AUDIO = "audio/";

    public AudioImporter(@NonNull Context context) throws IOException {
        super(context);
    }

    @NonNull
    @Override
    protected String getTypePrefix() {
        return TYPE_AUDIO;
    }

    @NonNull
    @Override
    protected Optional<Path> getDestinationFolder() {
        return homeEnvironment.getDefaultDirectory(HomeEnvironment.MUSIC);
    }

    @NonNull
    @Override
    protected String getDefaultName() {
        return resources.getString(R.string.receiver_audio_default_name,
                dateTimeFormatter.format(LocalDateTime.now()));
    }
}
