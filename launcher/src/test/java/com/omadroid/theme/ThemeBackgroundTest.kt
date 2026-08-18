package com.omadroid.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class ThemeBackgroundTest {
    @Test
    fun picksSortedWallpaperAndSkipsLogo() {
        val files =
            ThemeCatalog.backgroundFiles(
                arrayOf("omarchy.png", "2-waves.png", "1-totoro.png", ".hidden"),
            )
        assertEquals(listOf("1-totoro.png", "2-waves.png"), files)
    }

    @Test
    fun tokyoNightShipsABackground() {
        val dir = File("../shell/themes/tokyo-night/backgrounds")
        if (!dir.isDirectory) {
            return
        }
        val names = ThemeCatalog.backgroundFiles(dir.list())
        assertTrue(names.isNotEmpty())
    }

    @Test
    fun shippedBackgroundsMatchTheGuestDisplay() {
        val root = File("../shell/themes")
        if (!root.isDirectory) {
            return
        }
        val images =
            root.walkTopDown()
                .filter { it.isFile && it.parentFile?.name == "backgrounds" }
                .filter { name ->
                    val lower = name.extension.lowercase()
                    lower == "jpg" || lower == "jpeg" || lower == "png" || lower == "webp"
                }
                .toList()
        assertTrue(images.isNotEmpty())
        images.forEach { file ->
            val image = ImageIO.read(file)
            assertNotNull(file.path, image)
            assertEquals("${file.path} width", 1080, image.width)
            assertEquals("${file.path} height", 1920, image.height)
        }
    }
}