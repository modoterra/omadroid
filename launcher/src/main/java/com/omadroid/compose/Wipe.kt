package com.omadroid.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath

private const val WIPE_MS = 360
private const val SLANT = 0.18f

@Composable
fun ThemeWipe(
    frame: ImageBitmap,
    onDone: () -> Unit,
) {
    val progress = remember(frame) { Animatable(0f) }
    LaunchedEffect(frame) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(WIPE_MS, easing = FastOutSlowInEasing))
        onDone()
    }
    Canvas(Modifier.fillMaxSize()) {
        val slant = size.width * SLANT
        val travel = size.width + slant
        val top = travel * progress.value
        val bottom = top - slant
        val clip =
            Path().apply {
                moveTo(top, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(bottom, size.height)
                close()
            }
        clipPath(clip) {
            drawImage(frame)
        }
    }
}
