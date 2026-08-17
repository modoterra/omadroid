package com.omadroid.launcher

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class WifiState {
    Off,
    Disconnected,
    Connected,
}

data class BarStatus(
    val dateLabel: String,
    val wifi: WifiState,
    val batteryPercent: Int,
    val charging: Boolean,
) {
    val batteryLabel: String = formatBattery(batteryPercent, charging)
}

fun formatBarDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))

fun formatBattery(percent: Int, charging: Boolean): String {
    val clamped = percent.coerceIn(0, 100)
    return if (charging) "$clamped%+" else "$clamped%"
}

fun batteryContentDescription(percent: Int, charging: Boolean, locale: Locale): String {
    val clamped = percent.coerceIn(0, 100)
    return if (charging) {
        String.format(locale, "Battery %d percent, charging", clamped)
    } else {
        String.format(locale, "Battery %d percent", clamped)
    }
}

fun wifiContentDescription(state: WifiState): String =
    when (state) {
        WifiState.Off -> "Wi-Fi off"
        WifiState.Disconnected -> "Wi-Fi disconnected"
        WifiState.Connected -> "Wi-Fi connected"
    }

fun readBatteryPercent(level: Int, scale: Int): Int {
    if (level < 0 || scale <= 0) {
        return 0
    }
    return ((level * 100) / scale).coerceIn(0, 100)
}
