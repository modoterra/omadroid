package com.omadroid.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.omadroid.launcher.widget.GridStyle

/**
 * Omadroid Compose: a small Stack/Node API on Jetpack Compose Foundation.
 * Call [Compose] once at the Activity root. Do not import Material.
 */
val LocalGridStyle =
    staticCompositionLocalOf<GridStyle> {
        error("Compose { } must wrap the tree")
    }

@Composable
fun Compose(
    style: GridStyle,
    modifier: Modifier = Modifier,
    wallpaper: ImageBitmap? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalGridStyle provides style,
        LocalOverscrollFactory provides NoOverscrollFactory,
    ) {
        Box(modifier.fillMaxSize().background(Color(style.colors.background))) {
            if (wallpaper != null) {
                Image(
                    bitmap = wallpaper,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            content()
        }
    }
}
