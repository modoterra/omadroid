package com.omadroid.theme

class ThemeColorsException(message: String) : Exception(message)

data class ThemeColors(
    val slug: String,
    val mode: String,
    val accent: Int,
    val selection: Int,
    val muted: Int,
    val background: Int,
    val darkBackground: Int,
    val darkerBackground: Int,
    val lighterBackground: Int,
    val foreground: Int,
    val darkForeground: Int,
    val lightForeground: Int,
    val brightForeground: Int,
    val red: Int,
    val yellow: Int,
    val orange: Int,
    val green: Int,
    val cyan: Int,
    val blue: Int,
    val magenta: Int,
    val brown: Int,
) {
    companion object {
        const val DEFAULT_SLUG = "tokyo-night"

        fun parse(slug: String, toml: String): ThemeColors {
            val values = parseTomlScalars(toml)
            fun required(key: String): Int = parseHex(key, values[key] ?: throw ThemeColorsException("missing $key"))
            fun optional(key: String, fallback: Int): Int {
                val raw = values[key] ?: return fallback
                return parseHex(key, raw)
            }
            val background = required("background")
            val foreground = required("foreground")
            return ThemeColors(
                slug = slug,
                mode = values["mode"] ?: "dark",
                accent = required("accent"),
                selection = optional("selection", required("lighter_background")),
                muted = required("muted"),
                background = background,
                darkBackground = optional("dark_background", background),
                darkerBackground = optional("darker_background", background),
                lighterBackground = required("lighter_background"),
                foreground = foreground,
                darkForeground = optional("dark_foreground", required("muted")),
                lightForeground = optional("light_foreground", foreground),
                brightForeground = optional("bright_foreground", foreground),
                red = required("red"),
                yellow = required("yellow"),
                orange = optional("orange", required("yellow")),
                green = required("green"),
                cyan = required("cyan"),
                blue = required("blue"),
                magenta = required("magenta"),
                brown = optional("brown", required("red")),
            )
        }

        fun parseHex(key: String, raw: String): Int {
            val hex = raw.removePrefix("#")
            val rgb = when (hex.length) {
                6 -> "FF$hex"
                8 -> hex
                else -> throw ThemeColorsException("$key is not a hex color: $raw")
            }
            return try {
                rgb.toLong(16).toInt()
            } catch (_: NumberFormatException) {
                throw ThemeColorsException("$key is not a hex color: $raw")
            }
        }

        private fun parseTomlScalars(toml: String): Map<String, String> {
            val values = mutableMapOf<String, String>()
            toml.lineSequence().forEach { raw ->
                val trimmed = raw.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("[")) {
                    return@forEach
                }
                val line = trimmed
                val eq = line.indexOf('=')
                if (eq <= 0) {
                    return@forEach
                }
                val key = line.substring(0, eq).trim()
                var value = line.substring(eq + 1).trim()
                if (value.startsWith('"') && value.endsWith('"') && value.length >= 2) {
                    value = value.substring(1, value.length - 1)
                }
                values[key] = value
            }
            return values
        }
    }
}
