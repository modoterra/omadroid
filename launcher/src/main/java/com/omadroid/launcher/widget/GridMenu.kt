package com.omadroid.launcher.widget

import android.content.Context
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView

class GridMenu(
    context: Context,
    private var style: GridStyle,
) : ScrollView(context), Node {
    var onItemClick: (MenuItem) -> Unit = {}
    var onItemToggle: (MenuItem, Boolean) -> Unit = { _, _ -> }

    private val host = NodeHost()
    private val column =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

    override val nodes: List<Node>
        get() = host.nodes

    init {
        isFillViewport = true
        overScrollMode = OVER_SCROLL_NEVER
        isNestedScrollingEnabled = false
        isFocusable = true
        isFocusableInTouchMode = true
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        isSmoothScrollingEnabled = false
        addView(
            column,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    override fun style(next: GridStyle) {
        style = next
        host.style(next)
    }

    override fun requestChildFocus(child: View?, focused: View?) {
        // Mouse hover/wheel must not scroll a row into view.
    }

    override fun fling(velocityY: Int) {
        // Wheel and drag must stop when the finger/wheel does.
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_SCROLL) {
            return super.onGenericMotionEvent(event)
        }
        if (!event.isFromSource(InputDevice.SOURCE_CLASS_POINTER)) {
            return super.onGenericMotionEvent(event)
        }
        val notches = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
        if (notches == 0f) {
            return super.onGenericMotionEvent(event)
        }
        val step = ViewConfiguration.get(context).scaledVerticalScrollFactor
        val next = (scrollY - notches * step).toInt()
        val max = (column.height - height).coerceAtLeast(0)
        scrollTo(0, next.coerceIn(0, max))
        return true
    }

    fun bind(spec: MenuSpec) {
        val y = scrollY
        host.clear()
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
                host.add(header)
                column.addView(
                    header,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        style.cellPx,
                    ),
                )
            }
            section.items.forEachIndexed { index, item ->
                val row =
                    GridMenuRow(context, style, item).apply {
                        setOnClickListener { handleClick(item) }
                    }
                host.add(row)
                column.addView(
                    row,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        style.cellPx,
                    ),
                )
                if (index < section.items.lastIndex) {
                    val rule =
                        View(context).apply {
                            setBackgroundColor(style.colors.muted)
                            alpha = 0.25f
                        }
                    column.addView(
                        rule,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            1,
                        ).apply {
                            marginStart = style.spacePx
                            marginEnd = style.spacePx
                        },
                    )
                }
            }
        }
        post { scrollTo(0, y) }
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
) : LinearLayout(context), Node {
    private val host = NodeHost()

    override val nodes: List<Node>
        get() = host.nodes

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(style.spacePx, 0, style.spacePx, 0)
        isClickable = true
        isFocusable = false
        isFocusableInTouchMode = false
        contentDescription = item.title
        minimumHeight = style.cellPx
        if (item.icon != null) {
            val icon =
                GridIcon(context, style).apply {
                    text = item.icon
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            host.add(icon)
            addView(icon, LinearLayout.LayoutParams(style.innerPx, style.innerPx))
        }
        val label =
            GridText(context, style).apply {
                text = item.title
                gravity = Gravity.CENTER_VERTICAL
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        host.add(label)
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

    override fun style(next: GridStyle) {
        setPadding(next.spacePx, 0, next.spacePx, 0)
        minimumHeight = next.cellPx
        host.style(next)
    }
}
