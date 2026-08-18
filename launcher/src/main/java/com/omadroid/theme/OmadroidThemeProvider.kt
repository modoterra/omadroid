package com.omadroid.theme

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class OmadroidThemeProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(arrayOf(OmadroidTheme.COLUMN_SLUG))
        cursor.addRow(arrayOf(storedSlug()))
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/omadroid-theme"

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        update(uri, values, null, null)
        return OmadroidTheme.uri()
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        val slug = OmadroidTheme.normalizeSlug(values?.getAsString(OmadroidTheme.COLUMN_SLUG))
        prefs().edit().putString(OmadroidTheme.PREF_SLUG, slug).apply()
        context?.contentResolver?.notifyChange(OmadroidTheme.uri(), null)
        return 1
    }

    private fun storedSlug(): String =
        OmadroidTheme.normalizeSlug(prefs().getString(OmadroidTheme.PREF_SLUG, ThemeColors.DEFAULT_SLUG))

    private fun prefs() =
        requireNotNull(context)
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(OmadroidTheme.PREFS, 0)
}
