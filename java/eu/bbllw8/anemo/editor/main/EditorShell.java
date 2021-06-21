/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.editor.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

public final class EditorShell {

    private EditorShell() {
    }

    public static boolean isEnabled(@NonNull Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final int status = packageManager.getComponentEnabledSetting(
                new ComponentName(context, EditorShell.class));
        return PackageManager.COMPONENT_ENABLED_STATE_ENABLED == status;
    }

    public static void setEnabled(@NonNull Context context, boolean enabled) {
        final PackageManager packageManager = context.getPackageManager();
        final int newStatus = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        packageManager.setComponentEnabledSetting(
                new ComponentName(context, EditorShell.class),
                newStatus,
                PackageManager.DONT_KILL_APP);
    }
}
