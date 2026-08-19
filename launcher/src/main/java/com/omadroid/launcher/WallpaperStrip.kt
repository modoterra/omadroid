package com.omadroid.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.max

enum class WallpaperStripEdge {
    Top,
    Bottom,
}

internal fun wallpaperCropScale(
    sourceWidth: Int,
    sourceHeight: Int,
    destWidth: Int,
    destHeight: Int,
): Float {
    val srcW = sourceWidth.coerceAtLeast(1)
    val srcH = sourceHeight.coerceAtLeast(1)
    val dstW = destWidth.coerceAtLeast(1)
    val dstH = destHeight.coerceAtLeast(1)
    return max(dstW / srcW.toFloat(), dstH / srcH.toFloat())
}

internal fun wallpaperStripTopPx(
    edge: WallpaperStripEdge,
    screenHeightPx: Int,
    stripHeightPx: Int,
): Int =
    when (edge) {
        WallpaperStripEdge.Top -> 0
        WallpaperStripEdge.Bottom -> (screenHeightPx - stripHeightPx).coerceAtLeast(0)
    }

internal fun displayedWallpaperStrip(
    source: ImageBitmap,
    screenWidthPx: Int,
    screenHeightPx: Int,
    stripTopPx: Int,
    stripHeightPx: Int,
): ImageBitmap {
    val width = screenWidthPx.coerceAtLeast(1)
    val height = stripHeightPx.coerceAtLeast(1)
    val src = source.asAndroidBitmap()
    val scale = wallpaperCropScale(src.width, src.height, width, screenHeightPx.coerceAtLeast(1))
    val offsetX = (width - src.width * scale) / 2f
    val offsetY = (screenHeightPx.coerceAtLeast(1) - src.height * scale) / 2f
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val matrix = Matrix()
    matrix.setScale(scale, scale)
    matrix.postTranslate(offsetX, offsetY - stripTopPx.coerceAtLeast(0))
    canvas.drawBitmap(src, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
    return out.asImageBitmap()
}
