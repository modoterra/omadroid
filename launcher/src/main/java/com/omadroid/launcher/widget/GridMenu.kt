package com.omadroid.launcher.widget

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView

class GridMenu(
    context: Context,
    private val style: GridStyle,
) : ScrollView(context) {
    var onItemClick: (MenuItem) -> Unit = {}
    var onItemToggle: (MenuItem, Boolean) -> Unit = { _, _ -> }

    private val column =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

    init {
        isFillViewport = true
        addView(
            column,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    fun bind(spec: MenuSpec) {
        column.removeAllViews()
        spec.sections.forEach { section ->
            if (section.header.isNotEmpty()) {
                val header =
                    GridText(context, style).apply {
                        text = section.header
                        setTextColor(style.colors.muted)
                        gravity = Gravity.CENTER_VERTICAL
                        contentDescription = section.header
                    }
                column.addView(
                    header,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        style.cellPx,
                    ),
                )
            }
            section.items.forEach { item ->
                column.addView(
                    GridMenuRow(context, style, item).apply {
                        setOnClickListener { handleClick(item) }
                    },
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        style.cellPx,
                    ),
                )
            }
        }
    }

    private fun handleClick(item: MenuItem) {
        val current = item.toggled
        if (current != null) {
            onItemToggle(item, !current)
        } else {
            onItemClick(item)
        }
    }
}

class GridMenuRow(
    context: Context,
    style: GridStyle,
    item: MenuItem,
) : LinearLayout(context) {
    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(style.spacePx, 0, style.spacePx, 0)
        isClickable = true
        isFocusable = true
        contentDescription = item.title
        minimumHeight = style.cellPx
        val icon =
            GridIcon(context, style).apply {
                text = item.icon.orEmpty()
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        addView(icon, LinearLayout.LayoutParams(style.innerPx, style.innerPx))
        val label =
            GridText(context, style).apply {
                text = item.title
                gravity = Gravity.CENTER_VERTICAL
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        addView(
            label,
            LinearLayout.LayoutParams(0, style.innerPx, 1f).apply {
                marginStart = style.spacePx
            },
        )
        when {
            item.toggled != null -> {
                val on = item.toggled == true
                addView(
                    GridIcon(context, style).apply {
                        text = if (on) IconGlyphs.TOGGLE_ON else IconGlyphs.TOGGLE_OFF
                        tint(if (on) style.colors.accent else style.colors.muted)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    },
                    LinearLayout.LayoutParams(style.innerPx, style.innerPx).apply {
                        marginStart = style.spacePx
                    },
                )
            }
            item.selected -> {
                addView(
                    GridIcon(context, style).apply {
                        text = IconGlyphs.CHECK
                        tint(style.colors.accent)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    },
                    LinearLayout.LayoutParams(style.innerPx, style.innerPx).apply {
                        marginStart = style.spacePx
                    },
                )
            }
        }
    }
}
