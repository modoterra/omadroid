package com.omadroid.launcher.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class GridChrome(context: Context, style: GridStyle) : FrameLayout(context) {
    init {
        setBackgroundColor(style.colors.lighterBackground)
        setPadding(style.spacePx, style.spacePx, style.spacePx, style.spacePx)
        minimumHeight = style.cellPx
    }
}

class GridRow(context: Context, style: GridStyle) : LinearLayout(context) {
    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(style.colors.lighterBackground)
        setPadding(style.spacePx, style.spacePx, style.spacePx, style.spacePx)
        minimumHeight = style.cellPx
    }
}

class GridText(context: Context, style: GridStyle) : TextView(context) {
    init {
        typeface = IconFonts.ui(context)
        setTextColor(style.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, style.textSizePx.toFloat())
        includeFontPadding = false
        gravity = Gravity.CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        minHeight = style.innerPx
        setPadding(style.spacePx, style.spacePx, style.spacePx, style.spacePx)
    }
}

class GridButton(context: Context, style: GridStyle) : TextView(context) {
    init {
        typeface = IconFonts.ui(context)
        setTextColor(style.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, style.textSizePx.toFloat())
        includeFontPadding = false
        gravity = Gravity.CENTER
        minWidth = style.innerPx
        minHeight = style.innerPx
        setPadding(style.spacePx, style.spacePx, style.spacePx, style.spacePx)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
}

class GridIcon(context: Context, style: GridStyle) : TextView(context) {
    init {
        typeface = IconFonts.ui(context)
        setTextColor(style.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, style.iconPx.toFloat())
        includeFontPadding = false
        gravity = Gravity.CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        minWidth = style.innerPx
        minHeight = style.innerPx
        setPadding(0, 0, 0, 0)
    }
}

class GridIconButton(context: Context, style: GridStyle) : TextView(context) {
    init {
        typeface = IconFonts.ui(context)
        setTextColor(style.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, style.iconPx.toFloat())
        includeFontPadding = false
        gravity = Gravity.CENTER
        setBackgroundColor(0)
        minWidth = style.innerPx
        minHeight = style.innerPx
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setPadding(0, 0, 0, 0)
    }
}

class GridField(context: Context, style: GridStyle) : EditText(context, null, 0) {
    init {
        typeface = IconFonts.ui(context)
        setHintTextColor(style.colors.muted)
        setTextColor(style.colors.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, style.textSizePx.toFloat())
        setBackground(
            GradientDrawable().apply {
                setColor(style.colors.darkBackground)
                cornerRadius = style.cornerPx
            },
        )
        setPadding(style.spacePx, style.spacePx, style.spacePx, style.spacePx)
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        inputType = EditorInfo.TYPE_CLASS_TEXT
        isSingleLine = true
        includeFontPadding = false
        gravity = Gravity.CENTER_VERTICAL
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        minHeight = 0
        minimumHeight = 0
        maxHeight = style.innerPx
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
