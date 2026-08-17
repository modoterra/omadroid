package com.omadroid.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.pm.LauncherApps
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import com.omadroid.theme.ThemeCatalog
import com.omadroid.theme.ThemeColors

class HomeActivity : Activity() {
    private lateinit var launcherApps: LauncherApps
    private lateinit var theme: ThemeColors
    private lateinit var layoutButton: ImageButton
    private var layout: LauncherLayout = LauncherLayout.Desktop
    private val user: UserHandle = Process.myUserHandle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcherApps = getSystemService(LauncherApps::class.java)
        theme = ThemeCatalog.load(assets)
        if (savedInstanceState != null) {
            layout = LauncherLayout.valueOf(
                savedInstanceState.getString(STATE_LAYOUT, LauncherLayout.Desktop.name),
            )
        }
        window.decorView.setBackgroundColor(theme.background)
        setContentView(buildChrome())
        bindLayoutButton()
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback { },
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_LAYOUT, layout.name)
    }

    private fun buildChrome(): View {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(theme.background)
            }
        val desktop =
            FrameLayout(this).apply {
                setBackgroundColor(theme.background)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        root.addView(
            desktop,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
        root.addView(
            buildLauncherBar(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        return root
    }

    private fun buildLauncherBar(): View {
        val bar =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(theme.lighterBackground)
                val pad = dp(12)
                setPadding(pad, pad, pad, pad + dp(8))
                contentDescription = getString(R.string.app_name)
            }
        val field = EditText(this).apply {
            hint = getString(R.string.launcher_search)
            setHintTextColor(theme.muted)
            setTextColor(theme.foreground)
            setBackground(inputBackground())
            setPadding(dp(14), dp(10), dp(14), dp(10))
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = EditorInfo.TYPE_CLASS_TEXT
            isSingleLine = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        bar.addView(
            field,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            },
        )
        layoutButton = iconButton(R.drawable.ic_layout, getString(R.string.launcher_layout)) {
            layout = layout.next()
            bindLayoutButton()
        }
        bar.addView(layoutButton, buttonParams())
        val menuButton =
            iconButton(R.drawable.ic_menu, getString(R.string.launcher_menu)) { button ->
                showAppMenu(button)
            }
        bar.addView(menuButton, buttonParams())
        return bar
    }

    private fun bindLayoutButton() {
        val description =
            when (layout) {
                LauncherLayout.Desktop -> getString(R.string.launcher_layout_desktop)
                LauncherLayout.Focus -> getString(R.string.launcher_layout_focus)
            }
        layoutButton.contentDescription = description
        val tint = if (layout == LauncherLayout.Focus) theme.accent else theme.foreground
        layoutButton.imageTintList = ColorStateList.valueOf(tint)
    }

    private fun showAppMenu(anchor: View) {
        val infos = launcherApps.getActivityList(null, user)
        val apps =
            visibleLaunchableApps(
                infos.map { info ->
                    LaunchableApp(
                        packageName = info.componentName.packageName,
                        activityName = info.componentName.className,
                        label = info.label.toString(),
                    )
                },
                packageName,
            )
        val menu = PopupMenu(this, anchor)
        apps.forEachIndexed { index, app ->
            menu.menu.add(0, index, index, app.label)
        }
        menu.setOnMenuItemClickListener { item ->
            val app = apps[item.itemId]
            launcherApps.startMainActivity(
                ComponentName(app.packageName, app.activityName),
                user,
                null,
                null,
            )
            true
        }
        menu.show()
    }

    private fun iconButton(drawable: Int, description: String, onClick: (View) -> Unit): ImageButton {
        return ImageButton(this).apply {
            setImageResource(drawable)
            imageTintList = ColorStateList.valueOf(theme.foreground)
            setBackgroundColor(0)
            contentDescription = description
            minimumWidth = dp(48)
            minimumHeight = dp(48)
            setOnClickListener(onClick)
        }
    }

    private fun buttonParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(4) }

    private fun inputBackground(): GradientDrawable =
        GradientDrawable().apply {
            setColor(theme.darkBackground)
            cornerRadius = dp(8).toFloat()
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val STATE_LAYOUT = "launcher_layout"
    }
}
