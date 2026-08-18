package com.omadroid.clock

import android.content.Context
import android.content.SharedPreferences

class AlarmStore(private val prefs: SharedPreferences) {
    fun load(): List<SavedAlarm> = AlarmCodec.parse(prefs.getString(KEY, "") ?: "")

    fun save(alarms: List<SavedAlarm>) {
        prefs.edit().putString(KEY, AlarmCodec.serialize(alarms)).apply()
    }

    companion object {
        private const val PREFS = "omadroid_clock"
        private const val KEY = "alarms"

        fun deviceProtected(context: Context): AlarmStore {
            val storage = context.createDeviceProtectedStorageContext()
            return AlarmStore(storage.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
        }
    }
}
