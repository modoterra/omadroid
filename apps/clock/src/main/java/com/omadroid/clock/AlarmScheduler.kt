package com.omadroid.clock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmScheduler(private val context: Context) {
    private val alarms: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun canScheduleExact(): Boolean {
        val manager = alarms ?: return false
        return if (Build.VERSION.SDK_INT >= 31) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(alarm: SavedAlarm) {
        if (!alarm.enabled) {
            cancel(alarm.id)
            return
        }
        val manager = alarms ?: return
        val trigger =
            ClockMath.nextAlarmMillis(alarm.hour, alarm.minute, System.currentTimeMillis())
        val pending = pending(alarm.id)
        if (canScheduleExact()) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                return
            } catch (_: SecurityException) {
                // Fall through to inexact.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }

    fun cancel(id: Int) {
        alarms?.cancel(pending(id))
    }

    fun applyAll(list: List<SavedAlarm>) {
        list.forEach { alarm ->
            if (alarm.enabled) {
                schedule(alarm)
            } else {
                cancel(alarm.id)
            }
        }
    }

    private fun pending(id: Int): PendingIntent {
        val intent =
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_FIRE
                putExtra(EXTRA_ALARM_ID, id)
            }
        return PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_FIRE = "com.omadroid.clock.ALARM_FIRE"
        const val ACTION_DISMISS = "com.omadroid.clock.ALARM_DISMISS"
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}
