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
    val activeBorder: Int,
    val inactiveBorder: Int,
) {
    fun previewSwatches(): List<Int> =
        listOf(background, foreground, accent, red, yellow, green, cyan, blue, magenta)
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
            val accent = required("accent")
            val muted = required("muted")
            return ThemeColors(
                slug = slug,
                mode = values["mode"] ?: "dark",
                accent = accent,
                selection = optional("selection", required("lighter_background")),
                muted = muted,
                background = background,
                darkBackground = optional("dark_background", background),
                darkerBackground = optional("darker_background", background),
                lighterBackground = required("lighter_background"),
                foreground = foreground,
                darkForeground = optional("dark_foreground", muted),
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
                activeBorder =
                    optionalBorder(values, "active_border_color")
                        ?: optionalBorder(values, "hyprland_active_border")
                        ?: accent,
                inactiveBorder = optionalBorder(values, "hyprland_inactive_border") ?: muted,
            )
        }

        fun parseHex(key: String, raw: String): Int {
            val hex = raw.removePrefix("#")
            val rgb =
                when (hex.length) {
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

        fun parseColorToken(key: String, raw: String): Int {
            val token = raw.trim().split(Regex("\\s+")).firstOrNull { it.isNotEmpty() } ?: raw
            return when {
                token.startsWith("rgba(", ignoreCase = true) && token.endsWith(")") -> {
                    val inner = token.substring(5, token.length - 1)
                    if (inner.length != 8) {
                        throw ThemeColorsException("$key is not a hex color: $raw")
                    }
                    parseHex(key, inner.substring(6, 8) + inner.substring(0, 6))
                }
                token.startsWith("rgb(", ignoreCase = true) && token.endsWith(")") ->
                    parseHex(key, token.substring(4, token.length - 1))
                else -> parseHex(key, token)
            }
        }

        private fun optionalBorder(values: Map<String, String>, key: String): Int? {
            val raw = values[key] ?: return null
            return try {
                parseColorToken(key, raw)
            } catch (_: ThemeColorsException) {
                null
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
