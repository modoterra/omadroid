package com.omadroid.launcher.widget

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import com.omadroid.launcher.R

object IconFonts {
    const val OMARCHY_ASSET = "fonts/omarchy.ttf"

    @Volatile
    private var uiFace: Typeface? = null

    @Volatile
    private var omarchyFace: Typeface? = null

    fun ui(context: Context): Typeface {
        uiFace?.let {
            return it
        }
        val loaded = context.resources.getFont(R.font.jetbrains_mono_nerd)
        uiFace = loaded
        return loaded
    }

    fun omarchy(context: Context): Typeface {
        omarchyFace?.let {
            return it
        }
        val family = FontFamily.Builder(Font.Builder(context.assets, OMARCHY_ASSET).build()).build()
        val loaded = Typeface.CustomFallbackBuilder(family).build()
        omarchyFace = loaded
        return loaded
    }
}

object IconGlyphs {
    const val WIFI = "\uF1EB"
    const val BATTERY_FULL = "\uF240"
    const val BATTERY_THREE_QUARTERS = "\uF241"
    const val BATTERY_HALF = "\uF242"
    const val BATTERY_QUARTER = "\uF243"
    const val BATTERY_EMPTY = "\uF244"
    const val PLUG = "\uF1E6"
    const val LAYOUT = "\uF009"
    const val MENU = "\uF0C9"
    const val BACK = "\uF053"
    const val APP = "\uF1B2"
    const val TOGGLE_ON = "\uF205"
    const val TOGGLE_OFF = "\uF204"
    const val OMARCHY = "\uE900"

    fun battery(percent: Int, charging: Boolean): String {
        if (charging) {
            return PLUG
        }
        return when {
            percent > 75 -> BATTERY_FULL
            percent > 50 -> BATTERY_THREE_QUARTERS
            percent > 25 -> BATTERY_HALF
            percent > 10 -> BATTERY_QUARTER
            else -> BATTERY_EMPTY
        }
    }
}
