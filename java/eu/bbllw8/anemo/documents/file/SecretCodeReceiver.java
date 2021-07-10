/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.file;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

public final class SecretCodeReceiver extends BroadcastReceiver {
    private static final String ACTION = "android.provider.Telephony.SECRET_CODE";
    private static final String ENTRY_POINT_CODE = "26366";

    @Override
    public void onReceive(@Nullable Context context, @Nullable Intent intent) {
        if (intent != null && ACTION.equals(intent.getAction())) {
            final String secretCodeStr = intent.getDataString()
                    .replace("android_secret_code://", "")
                    .replace("tel:", "");
            if (context != null && ENTRY_POINT_CODE.equals(secretCodeStr)) {
                final Intent activityIntent = new Intent(context, FileActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(activityIntent);
            }
        }
    }
}
