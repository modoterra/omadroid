package com.omadroid.clock

data class SavedAlarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
)

object AlarmCodec {
    fun serialize(alarms: List<SavedAlarm>): String =
        alarms.joinToString("\n") { alarm ->
            val on = if (alarm.enabled) "1" else "0"
            "${alarm.id},${alarm.hour},${alarm.minute},$on"
        }

    fun parse(raw: String): List<SavedAlarm> {
        if (raw.isBlank()) {
            return emptyList()
        }
        return raw.lineSequence().mapNotNull { line ->
            val parts = line.trim().split(',')
            if (parts.size != 4) {
                return@mapNotNull null
            }
            val id = parts[0].toIntOrNull() ?: return@mapNotNull null
            val hour = parts[1].toIntOrNull() ?: return@mapNotNull null
            val minute = parts[2].toIntOrNull() ?: return@mapNotNull null
            if (id <= 0 || hour !in 0..23 || minute !in 0..59) {
                return@mapNotNull null
            }
            val enabled = parts[3] == "1" || parts[3].equals("true", ignoreCase = true)
            SavedAlarm(id, hour, minute, enabled)
        }.toList()
    }

    fun nextId(alarms: List<SavedAlarm>): Int = (alarms.maxOfOrNull { it.id } ?: 0) + 1
}
