/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.receiver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

public final class ReceiverActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            final Intent serviceIntent = new Intent(this, ReceiverService.class)
                    .setType(intent.getType())
                    .putExtra(Intent.EXTRA_STREAM,
                            (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
            startForegroundService(serviceIntent);
        }
        finish();
    }
}
