package com.omadroid.launcher.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class GridChrome(context: Context, private var style: GridStyle) : FrameLayout(context), Node {
    private val host = NodeHost()

    override val nodes: List<Node>
        get() = host.nodes

    init {
        style(style)
    }

    fun add(child: View, params: LayoutParams) {
        if (child is Node) {
            host.add(child)
        }
        addView(child, params)
    }

    override fun style(next: GridStyle) {
        style = next
        setBackgroundColor(next.colors.lighterBackground)
        setPadding(next.spacePx, next.spacePx, next.spacePx, next.spacePx)
        minimumHeight = next.cellPx
        host.style(next)
    }
}

class GridRow(context: Context, private var style: GridStyle) : LinearLayout(context), Node {
    private val host = NodeHost()

    override val nodes: List<Node>
        get() = host.nodes

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        style(style)
    }

    fun add(child: View, params: ViewGroup.LayoutParams) {
        if (child is Node) {
            host.add(child)
        }
        addView(child, params)
    }

    override fun style(next: GridStyle) {
        style = next
        setBackgroundColor(next.colors.lighterBackground)
        setPadding(next.spacePx, next.spacePx, next.spacePx, next.spacePx)
        minimumHeight = next.cellPx
        host.style(next)
    }
}

class GridText(context: Context, private var style: GridStyle) : TextView(context), Node {
    override val nodes: List<Node>
        get() = emptyList()

    init {
        typeface = IconFonts.ui(context)
        includeFontPadding = false
        gravity = Gravity.CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        style(style)
    }

    override fun style(next: GridStyle) {
        style = next
        setTextColor(next.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, next.textSizePx.toFloat())
        minHeight = next.innerPx
        setPadding(next.spacePx, next.spacePx, next.spacePx, next.spacePx)
    }
}

class GridButton(context: Context, private var style: GridStyle) : TextView(context), Node {
    override val nodes: List<Node>
        get() = emptyList()

    init {
        typeface = IconFonts.ui(context)
        includeFontPadding = false
        gravity = Gravity.CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        style(style)
    }

    override fun style(next: GridStyle) {
        style = next
        setTextColor(next.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, next.textSizePx.toFloat())
        minWidth = next.innerPx
        minHeight = next.innerPx
        setPadding(next.spacePx, next.spacePx, next.spacePx, next.spacePx)
    }
}

class GridIcon(context: Context, private var style: GridStyle) : TextView(context), Node {
    override val nodes: List<Node>
        get() = emptyList()

    init {
        typeface = IconFonts.ui(context)
        includeFontPadding = false
        gravity = Gravity.CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setPadding(0, 0, 0, 0)
        style(style)
    }

    override fun style(next: GridStyle) {
        style = next
        setTextColor(next.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, next.iconPx.toFloat())
        minWidth = next.innerPx
        minHeight = next.innerPx
    }
}

class GridIconButton(context: Context, private var style: GridStyle) : TextView(context), Node {
    override val nodes: List<Node>
        get() = emptyList()

    init {
        typeface = IconFonts.ui(context)
        includeFontPadding = false
        gravity = Gravity.CENTER
        setBackgroundColor(0)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setPadding(0, 0, 0, 0)
        style(style)
    }

    override fun style(next: GridStyle) {
        style = next
        setTextColor(next.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, next.iconPx.toFloat())
        minWidth = next.innerPx
        minHeight = next.innerPx
    }
}

class GridField(context: Context, private var style: GridStyle) : EditText(context, null, 0), Node {
    override val nodes: List<Node>
        get() = emptyList()

    init {
        typeface = IconFonts.ui(context)
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        inputType = EditorInfo.TYPE_CLASS_TEXT
        isSingleLine = true
        includeFontPadding = false
        gravity = Gravity.CENTER_VERTICAL
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        minHeight = 0
        minimumHeight = 0
        style(style)
    }

    override fun style(next: GridStyle) {
        style = next
        setHintTextColor(next.colors.muted)
        setTextColor(next.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, next.textSizePx.toFloat())
        setBackground(
            GradientDrawable().apply {
                setColor(next.colors.darkBackground)
                cornerRadius = next.cornerPx
            },
        )
        setPadding(next.spacePx, next.spacePx, next.spacePx, next.spacePx)
        maxHeight = next.innerPx
    }
}

fun gridCellParams(style: GridStyle): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(style.innerPx, style.innerPx).apply {
        marginStart = style.spacePx
    }

fun gridIconParams(style: GridStyle): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(style.innerPx, style.innerPx).apply {
        marginEnd = style.spacePx
    }

fun gridStretchParams(style: GridStyle): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(0, style.innerPx, 1f).apply {
        marginEnd = style.spacePx
    }

fun View.tint(color: Int) {
    when (this) {
        is ImageView -> imageTintList = ColorStateList.valueOf(color)
        is TextView -> setTextColor(color)
    }
}
