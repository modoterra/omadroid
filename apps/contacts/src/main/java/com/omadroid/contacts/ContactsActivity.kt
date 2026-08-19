package com.omadroid.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
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
import com.omadroid.launcher.unitLengthPx
import com.omadroid.launcher.widget.GridStyle
import com.omadroid.theme.OmadroidTheme
import com.omadroid.theme.ThemeColors

class ContactsActivity : ComponentActivity() {
    private var theme by mutableStateOf(placeholderTheme())
    private var style by mutableStateOf(GridStyle(1, placeholderTheme()))
    private var screen by mutableStateOf<ContactsScreen>(ContactsScreen.List)
    private var query by mutableStateOf("")
    private var contacts by mutableStateOf<List<ContactSummary>>(emptyList())
    private var detail by mutableStateOf<ContactDetail?>(null)
    private var permitted by mutableStateOf(false)
    private lateinit var store: ContactsStore
    private var themeWatch: ContentObserver? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshPermissions()
        }

    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reload()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        window.decorView.overScrollMode = android.view.View.OVER_SCROLL_NEVER
        store = ContactsContractStore(contentResolver)
        applySelectedTheme()
        hideSystemBars()
        refreshPermissions()
        setContent {
            ContactsApp(
                style = style,
                screen = screen,
                query = query,
                contacts = contacts,
                detail = detail,
                permitted = permitted,
                onQuery = { query = it },
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.WRITE_CONTACTS,
                        ),
                    )
                },
                onOpen = { id ->
                    screen = ContactsScreen.Detail(id)
                    detail = if (permitted) store.detail(id) else null
                },
                onAdd = { screen = ContactsScreen.Edit(null) },
                onEdit = { id ->
                    screen = ContactsScreen.Edit(id)
                    if (permitted) {
                        detail = store.detail(id)
                    }
                },
                onBack = { onScreenBack() },
                onSave = { id, name, phone -> save(id, name, phone) },
                onDelete = { id -> delete(id) },
            )
        }
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback { onScreenBack() },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
        if (themeWatch == null) {
            themeWatch = OmadroidTheme.observe(this) { applySelectedTheme() }
        }
        applySelectedTheme()
        refreshPermissions()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(observer)
        themeWatch?.let { contentResolver.unregisterContentObserver(it) }
        themeWatch = null
        super.onStop()
    }

    private fun applySelectedTheme() {
        theme =
            try {
                OmadroidTheme.load(this)
            } catch (_: com.omadroid.theme.ThemeColorsException) {
                placeholderTheme()
            }
        style = GridStyle(unitLengthPx(resources.displayMetrics.density), theme)
        window.decorView.setBackgroundColor(theme.background)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (onScreenBack()) {
            return
        }
        super.onBackPressed()
    }

    private fun onScreenBack(): Boolean {
        return when (val current = screen) {
            is ContactsScreen.Edit -> {
                val id = current.contactId
                screen = if (id != null) ContactsScreen.Detail(id) else ContactsScreen.List
                if (id != null && permitted) {
                    detail = store.detail(id)
                }
                true
            }
            is ContactsScreen.Detail -> {
                screen = ContactsScreen.List
                detail = null
                true
            }
            ContactsScreen.List -> false
        }
    }

    private fun refreshPermissions() {
        permitted =
            checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED
        reload()
    }

    private fun reload() {
        if (!permitted) {
            contacts = emptyList()
            detail = null
            return
        }
        try {
            contacts = store.list()
            val current = screen
            if (current is ContactsScreen.Detail) {
                detail = store.detail(current.contactId)
            }
        } catch (_: SecurityException) {
            permitted = false
            contacts = emptyList()
            detail = null
        }
    }

    private fun save(id: Long?, name: String, phone: String): Boolean {
        val draft = normalizeDraft(name, phone) ?: return false
        if (!permitted) {
            return false
        }
        val savedId =
            try {
                if (id == null) {
                    store.insert(draft)
                } else if (store.update(id, draft)) {
                    id
                } else {
                    null
                }
            } catch (_: SecurityException) {
                permitted = false
                null
            } ?: return false
        reload()
        screen = ContactsScreen.Detail(savedId)
        detail = try {
            store.detail(savedId)
        } catch (_: SecurityException) {
            null
        }
        return true
    }

    private fun delete(id: Long) {
        if (permitted) {
            try {
                store.delete(id)
            } catch (_: SecurityException) {
                permitted = false
            }
        }
        screen = ContactsScreen.List
        detail = null
        reload()
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
        activeBorder = 0xFF7AA2F7.toInt(),
        inactiveBorder = 0xFF414868.toInt(),
    )
