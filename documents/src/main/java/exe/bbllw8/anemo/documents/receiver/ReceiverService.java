/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package exe.bbllw8.anemo.documents.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.IOException;
import java.nio.file.Path;

import exe.bbllw8.anemo.documents.R;
import exe.bbllw8.anemo.documents.home.HomeEnvironment;
import exe.bbllw8.anemo.task.TaskExecutor;

public final class ReceiverService extends Service {
    private static final String CHANNEL_ID = "notifications_receiver";
    private static final String TAG = "ReceiverService";
    private static final int NOTIFICATION_ID = 1;

    @NonNull
    private final TaskExecutor taskExecutor = new TaskExecutor();
    private NotificationManager notificationManager;
    private Importer[] importers;

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = getSystemService(NotificationManager.class);
        try {
            importers = getImporters();
        } catch (IOException e) {
            Log.e(TAG, "Failed to load importers", e);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        taskExecutor.terminate();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(@Nullable Intent intent) {
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

    @NonNull
    private Importer[] getImporters() throws IOException {
        final HomeEnvironment homeEnvironment = HomeEnvironment.getInstance(this);
        final Path fallbackDir = homeEnvironment.getBaseDir();
        return new Importer[]{
                // Audio
                new Importer(this,
                        taskExecutor,
                        homeEnvironment.getDefaultDirectory(HomeEnvironment.MUSIC)
                                .orElse(fallbackDir),
                        "audio/",
                        R.string.receiver_audio_default_name),
                // Images
                new Importer(this,
                        taskExecutor,
                        homeEnvironment.getDefaultDirectory(HomeEnvironment.PICTURES)
                                .orElse(fallbackDir),
                        "image/",
                        R.string.receiver_image_default_name),
                // PDF
                new Importer(this,
                        taskExecutor,
                        homeEnvironment.getDefaultDirectory(HomeEnvironment.DOCUMENTS)
                                .orElse(fallbackDir),
                        "application/pdf",
                        R.string.receiver_pdf_default_name),
                // Video
                new Importer(this,
                        taskExecutor,
                        homeEnvironment.getDefaultDirectory(HomeEnvironment.MOVIES)
                                .orElse(fallbackDir),
                        "video/",
                        R.string.receiver_video_default_name),
        };
    }

    private void runImporter(@NonNull Importer importer,
                             @NonNull Intent intent) {
        startForeground(NOTIFICATION_ID,
                buildNotification(true, R.string.receiver_importing_prepare));
        importer.execute(intent.getParcelableExtra(Intent.EXTRA_STREAM),
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
                .setColor(getColor(exe.bbllw8.anemo.shell.R.color.anemoColor));
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
