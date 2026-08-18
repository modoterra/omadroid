package com.omadroid.theme

import android.content.ContentValues
import android.content.Context
import android.content.res.AssetManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper

object OmadroidTheme {
    const val AUTHORITY = "com.omadroid.theme"
    const val PATH_CURRENT = "current"
    const val COLUMN_SLUG = "slug"
    const val PREFS = "omadroid"
    const val PREF_SLUG = "theme_slug"

    fun uri(): Uri = Uri.parse("content://$AUTHORITY/$PATH_CURRENT")

    fun normalizeSlug(slug: String?): String {
        val value = slug?.trim().orEmpty()
        return if (value.isEmpty()) ThemeColors.DEFAULT_SLUG else value
    }

    fun slug(context: Context): String {
        return try {
            context.contentResolver.query(uri(), arrayOf(COLUMN_SLUG), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }.let { normalizeSlug(it) }
    }

    fun load(context: Context, assets: AssetManager = context.assets): ThemeColors {
        val requested = slug(context)
        return try {
            ThemeCatalog.load(assets, requested)
        } catch (_: ThemeColorsException) {
            ThemeCatalog.load(assets, ThemeColors.DEFAULT_SLUG)
        }
    }

    fun save(context: Context, slug: String) {
        val values = ContentValues().apply { put(COLUMN_SLUG, normalizeSlug(slug)) }
        context.contentResolver.update(uri(), values, null, null)
    }

    fun observe(context: Context, onChange: () -> Unit): ContentObserver {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    onChange()
                }
            }
        context.contentResolver.registerContentObserver(uri(), false, observer)
        return observer
    }
}
