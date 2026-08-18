package com.omadroid.gallery

import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.omadroid.launcher.widget.GridStyle
import com.omadroid.theme.OmadroidTheme
import com.omadroid.theme.ThemeColors

class GalleryActivity : ComponentActivity() {
    private var theme by mutableStateOf(placeholderTheme())
    private var style by mutableStateOf(GridStyle(1, placeholderTheme()))
    private var permitted by mutableStateOf(false)
    private var images by mutableStateOf<List<MediaImage>>(emptyList())
    private var nav by mutableStateOf(GalleryNav())
    private var themeWatch: ContentObserver? = null

    private val askPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permitted = granted
            if (granted) {
                images = loadImages()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        window.decorView.overScrollMode = android.view.View.OVER_SCROLL_NEVER
        applySelectedTheme()
        hideSystemBars()
        setContent {
            com.omadroid.compose.Compose(style) {
            GalleryApp(
                permitted = permitted,
                images = images,
                nav = nav,
                onRequestPermission = { askPermission.launch(GalleryPermissions.required()) },
                onOpenAlbum = { bucketId -> nav = nav.push(GalleryRoute.Album(bucketId)) },
                onOpenImage = { imageId -> nav = nav.push(GalleryRoute.Viewer(imageId)) },
                onBack = { onGalleryBack() },
            )
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback { onGalleryBack() },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (themeWatch == null) {
            themeWatch = OmadroidTheme.observe(this) { applySelectedTheme() }
        }
        applySelectedTheme()
        hideSystemBars()
        reload()
    }

    override fun onStop() {
        themeWatch?.let { contentResolver.unregisterContentObserver(it) }
        themeWatch = null
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (onGalleryBack()) {
            return
        }
        super.onBackPressed()
    }

    private fun onGalleryBack(): Boolean {
        if (!nav.canPop) {
            return false
        }
        nav = nav.pop()
        return true
    }

    private fun reload() {
        permitted = GalleryPermissions.granted(this)
        images = if (permitted) loadImages() else emptyList()
    }

    private fun loadImages(): List<MediaImage> =
        try {
            MediaStoreImages.query(contentResolver)
        } catch (_: SecurityException) {
            emptyList()
        }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT < 30) {
            return
        }
        window.setDecorFitsSystemWindows(false)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
    }

    private fun applySelectedTheme() {
        theme =
            try {
                OmadroidTheme.load(this)
            } catch (_: com.omadroid.theme.ThemeColorsException) {
                placeholderTheme()
            }
        style = GridStyle((32 * resources.displayMetrics.density).toInt().coerceAtLeast(1), theme)
        window.decorView.setBackgroundColor(theme.background)
    }
}

private fun placeholderTheme(): ThemeColors =
    ThemeColors(
        slug = ThemeColors.DEFAULT_SLUG,
        mode = "dark",
        accent = 0xFF7AA2F7.toInt(),
        selection = 0xFF292E42.toInt(),
        muted = 0xFF414868.toInt(),
        background = 0xFF1A1B26.toInt(),
        darkBackground = 0xFF13141C.toInt(),
        darkerBackground = 0xFF0E0E14.toInt(),
        lighterBackground = 0xFF24283B.toInt(),
        foreground = 0xFFA9B1D6.toInt(),
        darkForeground = 0xFF565F89.toInt(),
        lightForeground = 0xFFB4BEE6.toInt(),
        brightForeground = 0xFFC0CAF5.toInt(),
        red = 0xFFF7768E.toInt(),
        yellow = 0xFFE0AF68.toInt(),
        orange = 0xFFEB927B.toInt(),
        green = 0xFF9ECE6A.toInt(),
        cyan = 0xFF449DAB.toInt(),
        blue = 0xFF7AA2F7.toInt(),
        magenta = 0xFFAD8EE6.toInt(),
        brown = 0xFF75493D.toInt(),
    )
