package com.omadroid.launcher.widget

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.Build
import com.omadroid.launcher.R

object IconFonts {
    const val OMARCHY_ASSET = "fonts/omarchy.ttf"

    @Volatile
    private var uiFace: Typeface? = null

    fun ui(context: Context): Typeface {
        uiFace?.let {
            return it
        }
        val loaded =
            if (Build.VERSION.SDK_INT >= 29) {
                val nerdFd = context.resources.openRawResourceFd(R.font.jetbrains_mono_nerd)
                val nerd =
                    nerdFd.use { fd ->
                        FontFamily.Builder(Font.Builder(fd.parcelFileDescriptor).build()).build()
                    }
                val omarchy =
                    FontFamily.Builder(Font.Builder(context.assets, OMARCHY_ASSET).build()).build()
                Typeface.CustomFallbackBuilder(nerd)
                    .addCustomFallback(omarchy)
                    .setSystemFallback("monospace")
                    .build()
            } else {
                context.resources.getFont(R.font.jetbrains_mono_nerd)
            }
        uiFace = loaded
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
