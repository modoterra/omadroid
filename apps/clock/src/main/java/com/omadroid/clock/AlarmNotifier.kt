package com.omadroid.clock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmNotifier {
    const val CHANNEL_ID = "omadroid.clock.alarms"
    const val NOTIFICATION_ID = 1

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) {
            return
        }
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.alarm_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setBypassDnd(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    fun show(context: Context, alarm: SavedAlarm) {
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val fire =
            PendingIntent.getActivity(
                context,
                alarm.id,
                fireIntent(context, alarm.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val dismiss =
            PendingIntent.getBroadcast(
                context,
                alarm.id + 10_000,
                Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmScheduler.ACTION_DISMISS
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val label = ClockMath.formatHm(alarm.hour, alarm.minute)
        val notification =
            if (Build.VERSION.SDK_INT >= 26) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(context.getString(R.string.alarm_ringing))
                .setContentText(label)
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(fire)
                .setFullScreenIntent(fire, true)
                .setDeleteIntent(dismiss)
                .addAction(0, context.getString(R.string.alarm_dismiss), dismiss)
                .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    fun fireIntent(context: Context, alarmId: Int): Intent =
        Intent(context, AlarmFireActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
}
