/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.IOException;

import eu.bbllw8.anemo.documents.receiver.importer.AudioImporter;
import eu.bbllw8.anemo.documents.receiver.importer.ImageImporter;
import eu.bbllw8.anemo.documents.receiver.importer.Importer;
import eu.bbllw8.anemo.documents.receiver.importer.PdfImporter;
import eu.bbllw8.anemo.documents.receiver.importer.VideoImporter;

public final class ReceiverService extends Service {
    private static final String CHANNEL_ID = "notifications_receiver";
    private static final String TAG = "ReceiverService";
    private static final int NOTIFICATION_ID = 1;

    private NotificationManager notificationManager;
    private Importer[] importers;

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = getSystemService(NotificationManager.class);
        try {
            importers = new Importer[]{
                    new AudioImporter(this),
                    new ImageImporter(this),
                    new PdfImporter(this),
                    new VideoImporter(this),
            };
        } catch (IOException e) {
            Log.e(TAG, "Failed to load home", e);
            stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(buildNotificationChannel());
        }
        if (intent != null) {
            final String type = intent.getType();
            if (type != null) {
                for (final Importer importer : importers) {
                    if (importer.typeMatch(type)) {
                        runImporter(importer, intent);
                        break;
                    }
                }
            }
        }
        return START_NOT_STICKY;
    }

    private void runImporter(@NonNull Importer importer,
                             @NonNull Intent intent) {
        startForeground(NOTIFICATION_ID,
                buildNotification(true, R.string.receiver_importing_prepare));
        final Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        importer.execute(uri,
                fileName -> notificationManager.notify(NOTIFICATION_ID,
                        buildNotification(true, R.string.receiver_importing_message, fileName)),
                (destination, fileName) -> {
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(false,
                            R.string.receiver_importing_done_ok,
                            destination,
                            fileName));
                    stopSelf();
                },
                fileName -> {
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(false,
                            R.string.receiver_importing_done_fail,
                            fileName));
                    stopSelf();
                }
        );
    }

    @NonNull
    private Notification buildNotification(boolean inProgress,
                                           @StringRes int message,
                                           @NonNull Object... args) {
        final Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.receiver_label))
                .setContentText(getString(message, args))
                .setSmallIcon(R.drawable.ic_importer_notification)
                .setColor(getColor(R.color.anemoColor));
        if (inProgress) {
            builder.setProgress(100, 50, true);
        }
        return builder.build();
    }

    @NonNull
    private NotificationChannel buildNotificationChannel() {
        final NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.receiver_notification_channel_title),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.receiver_notification_channel_description));
        return channel;
    }
}
