package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperStripTest {
    @Test
    fun cropScaleCoversTheScreen() {
        assertEquals(1f, wallpaperCropScale(1080, 1920, 1080, 1920))
        assertEquals(2f, wallpaperCropScale(540, 960, 1080, 1920))
    }

    @Test
    fun topStripStartsAtZero() {
        assertEquals(0, wallpaperStripTopPx(WallpaperStripEdge.Top, screenHeightPx = 1920, stripHeightPx = 96))
    }

    @Test
    fun bottomStripSitsOnTheScreenEdge() {
        assertEquals(
            1824,
            wallpaperStripTopPx(WallpaperStripEdge.Bottom, screenHeightPx = 1920, stripHeightPx = 96),
        )
    }
}
