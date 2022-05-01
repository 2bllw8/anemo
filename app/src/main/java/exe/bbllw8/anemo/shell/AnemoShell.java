/*
 * Copyright (c) 2022 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.shell;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

public final class AnemoShell {

    private AnemoShell() {
    }

    public static boolean isEnabled(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final int status = packageManager
                .getComponentEnabledSetting(new ComponentName(context, LauncherActivity.class));
        return PackageManager.COMPONENT_ENABLED_STATE_DISABLED > status;
    }

    public static void setEnabled(Context context, boolean enabled) {
        final PackageManager packageManager = context.getPackageManager();
        final int newStatus = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        packageManager.setComponentEnabledSetting(
                new ComponentName(context, LauncherActivity.class), newStatus,
                PackageManager.DONT_KILL_APP);
    }
}
