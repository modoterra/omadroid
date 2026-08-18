package com.omadroid.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = AlarmStore.deviceProtected(context)
        val scheduler = AlarmScheduler(context)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            -> {
                scheduler.applyAll(store.load())
            }
            AlarmScheduler.ACTION_DISMISS -> {
                dismiss(context)
            }
            AlarmScheduler.ACTION_FIRE -> {
                fire(context, store, scheduler, intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1))
            }
            else -> {
                if (intent.hasExtra(AlarmScheduler.EXTRA_ALARM_ID)) {
                    fire(context, store, scheduler, intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1))
                }
            }
        }
    }

    private fun fire(
        context: Context,
        store: AlarmStore,
        scheduler: AlarmScheduler,
        id: Int,
    ) {
        val alarm = store.load().find { it.id == id && it.enabled } ?: return
        AlarmTone.start(context.applicationContext)
        AlarmNotifier.show(context, alarm)
        try {
            context.startActivity(AlarmNotifier.fireIntent(context, alarm.id))
        } catch (_: Exception) {
            // Full-screen notification still fires.
        }
        if (alarm.enabled) {
            scheduler.schedule(alarm)
        }
    }

    companion object {
        fun dismiss(context: Context) {
            AlarmTone.stop()
            AlarmTone.stopVibrate(context)
            AlarmNotifier.cancel(context)
        }
    }
}
