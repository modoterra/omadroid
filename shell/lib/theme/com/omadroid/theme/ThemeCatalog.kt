package com.omadroid.theme

import android.content.res.AssetManager
import java.io.IOException
import java.util.Locale

object ThemeCatalog {
    fun displayName(slug: String): String =
        slug.split('-', '_').filter { it.isNotEmpty() }.joinToString(" ") { part ->
            part.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
            }
        }
    fun load(assets: AssetManager, slug: String = ThemeColors.DEFAULT_SLUG): ThemeColors {
        val path = "$slug/colors.toml"
        val text = try {
            assets.open(path).bufferedReader().use { it.readText() }
        } catch (error: IOException) {
            throw ThemeColorsException("theme $slug missing: ${error.message}")
        }
        return ThemeColors.parse(slug, text)
    }

    fun slugs(assets: AssetManager): List<String> {
        val dirs = assets.list("") ?: return emptyList()
        return dirs.filter { dir ->
            try {
                assets.open("$dir/colors.toml").close()
                true
            } catch (_: IOException) {
                false
            }
        }.sorted()
    }

    fun backgroundFiles(names: Array<String>?): List<String> =
        names
            ?.filter { name ->
                val lower = name.lowercase(Locale.US)
                !name.startsWith(".") &&
                    name != "omarchy.png" &&
                    (lower.endsWith(".jpg") ||
                        lower.endsWith(".jpeg") ||
                        lower.endsWith(".png") ||
                        lower.endsWith(".webp"))
            }
            ?.sorted()
            ?: emptyList()

    fun backgroundAsset(slug: String, file: String): String = "$slug/backgrounds/$file"
}
