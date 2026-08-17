package com.omadroid.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.omadroid.theme.ThemeCatalog
import com.omadroid.theme.ThemeColors

class HomeActivity : Activity() {
    private lateinit var launcherApps: LauncherApps
    private lateinit var grid: GridView
    private lateinit var theme: ThemeColors
    private val user: UserHandle = Process.myUserHandle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcherApps = getSystemService(LauncherApps::class.java)
        theme = ThemeCatalog.load(assets)

        grid = GridView(this).apply {
            setBackgroundColor(theme.background)
            numColumns = 4
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            verticalSpacing = dp(20)
            horizontalSpacing = dp(8)
            setPadding(dp(16), dp(48), dp(16), dp(24))
            clipToPadding = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = getString(R.string.apps_grid)
        }
        window.decorView.setBackgroundColor(theme.background)
        setContentView(grid)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback { },
            )
        }
        bindApps()
    }

    override fun onResume() {
        super.onResume()
        bindApps()
    }

    private fun bindApps() {
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
        grid.adapter = AppAdapter(apps, theme)
    }

    private inner class AppAdapter(
        private val apps: List<LaunchableApp>,
        private val theme: ThemeColors,
    ) : BaseAdapter() {
        override fun getCount(): Int = apps.size

        override fun getItem(position: Int): LaunchableApp = apps[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val app = apps[position]
            val column = (parent as GridView).let { it.width / it.numColumns.coerceAtLeast(1) }
            val cell =
                (convertView as? LinearLayout) ?: LinearLayout(this@HomeActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(4), dp(8), dp(4), dp(8))
                    val icon =
                        ImageView(this@HomeActivity).apply {
                            id = ICON_ID
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        }
                    val size = dp(56)
                    addView(icon, LinearLayout.LayoutParams(size, size))
                    val label =
                        TextView(this@HomeActivity).apply {
                            id = LABEL_ID
                            gravity = Gravity.CENTER
                            textSize = 12f
                            setTextColor(theme.foreground)
                            maxLines = 2
                        }
                    addView(
                        label,
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply { topMargin = dp(8) },
                    )
                }
            if (column > 0) {
                cell.layoutParams = ViewGroup.LayoutParams(column, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            val component = ComponentName(app.packageName, app.activityName)
            val info = launcherApps.getActivityList(app.packageName, user)
                .firstOrNull { it.componentName == component }
            val iconView = cell.findViewById<ImageView>(ICON_ID)
            val labelView = cell.findViewById<TextView>(LABEL_ID)
            iconView.setImageDrawable(info?.getIcon(0))
            labelView.text = app.label
            cell.contentDescription = app.label
            cell.setOnClickListener {
                launcherApps.startMainActivity(component, user, null, null)
            }
            return cell
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ICON_ID = 0x7f0a0001
        private const val LABEL_ID = 0x7f0a0002
    }
}
