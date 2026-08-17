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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class GridChrome(context: Context, style: GridStyle) : FrameLayout(context) {
    init {
        setBackgroundColor(style.colors.lighterBackground)
        setPadding(style.insetPx, 0, style.insetPx, 0)
        minimumHeight = style.cellPx
    }
}

class GridRow(context: Context, style: GridStyle) : LinearLayout(context) {
    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(style.colors.lighterBackground)
        setPadding(style.insetPx, 0, style.insetPx, 0)
        minimumHeight = style.cellPx
    }
}

class GridText(context: Context, style: GridStyle) : TextView(context) {
    init {
        setTextColor(style.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, style.textSizePx.toFloat())
        includeFontPadding = false
        gravity = Gravity.CENTER_VERTICAL
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        minHeight = style.cellPx
    }
}

class GridButton(context: Context, style: GridStyle, compact: Boolean = false) : TextView(context) {
    init {
        setTextColor(style.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, style.textSizePx.toFloat())
        includeFontPadding = false
        gravity = Gravity.CENTER
        minHeight = style.cellPx
        if (compact) {
            minWidth = 0
            setPadding(style.insetPx, 0, style.insetPx, 0)
        } else {
            minWidth = style.cellPx
        }
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
}

class GridIcon(context: Context, style: GridStyle) : ImageView(context) {
    init {
        imageTintList = ColorStateList.valueOf(style.colors.foreground)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        minimumWidth = style.iconPx
        minimumHeight = style.iconPx
    }
}

class GridIconButton(context: Context, style: GridStyle) : ImageButton(context) {
    init {
        imageTintList = ColorStateList.valueOf(style.colors.foreground)
        setBackgroundColor(0)
        minimumWidth = style.cellPx
        minimumHeight = style.cellPx
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        scaleType = ScaleType.CENTER_INSIDE
    }
}

class GridField(context: Context, style: GridStyle) : EditText(context, null, 0) {
    init {
        setHintTextColor(style.colors.muted)
        setTextColor(style.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, style.textSizePx.toFloat())
        setBackground(
            GradientDrawable().apply {
                setColor(style.colors.darkBackground)
                cornerRadius = style.cornerPx
            },
        )
        setPadding(style.insetPx, 0, style.insetPx, 0)
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        inputType = EditorInfo.TYPE_CLASS_TEXT
        isSingleLine = true
        includeFontPadding = false
        gravity = Gravity.CENTER_VERTICAL
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        minHeight = 0
        minimumHeight = 0
        maxHeight = style.fieldHeightPx
    }
}

fun gridCellParams(style: GridStyle, marginStart: Int = 0): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(style.cellPx, style.cellPx).apply {
        this.marginStart = marginStart
    }

fun gridIconParams(style: GridStyle, marginEnd: Int = 0): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(style.iconPx, style.iconPx).apply {
        this.marginEnd = marginEnd
    }

fun gridStretchParams(style: GridStyle, marginEnd: Int = 0): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(0, style.fieldHeightPx, 1f).apply {
        this.marginEnd = marginEnd
    }

fun View.tint(color: Int) {
    when (this) {
        is ImageView -> imageTintList = ColorStateList.valueOf(color)
        is TextView -> setTextColor(color)
    }
}
