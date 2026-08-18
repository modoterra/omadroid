package com.omadroid.launcher

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.omadroid.theme.ThemeCatalog
import java.io.IOException

fun loadThemeBackground(assets: AssetManager, slug: String): ImageBitmap? {
    val names = ThemeCatalog.backgroundFiles(assets.list("$slug/backgrounds"))
    val file = names.firstOrNull() ?: return null
    return try {
        assets.open(ThemeCatalog.backgroundAsset(slug, file)).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    } catch (_: IOException) {
        null
    }
}
