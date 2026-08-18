package com.omadroid.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import com.omadroid.gallery.widget.IconFonts
import com.omadroid.gallery.widget.IconGlyphs

@Composable
fun omadroidFont(): FontFamily {
    val context = LocalContext.current
    return remember { FontFamily(IconFonts.ui(context)) }
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Int? = null,
    description: String = text,
    align: TextAlign = TextAlign.Center,
) {
    val style = LocalGridStyle.current
    val size = with(LocalDensity.current) { style.textSizePx.toSp() }
    BasicText(
        text = text,
        modifier =
            modifier
                .padding(with(LocalDensity.current) { style.spacePx.toDp() })
                .semantics { contentDescription = description },
        style =
            androidx.compose.ui.text.TextStyle(
                color = Color(color ?: style.colors.foreground),
                fontSize = size,
                fontFamily = omadroidFont(),
                textAlign = align,
            ),
    )
}

@Composable
fun Glyph(
    glyph: String,
    modifier: Modifier = Modifier,
    color: Int? = null,
    description: String = "",
    alpha: Float = 1f,
) {
    val style = LocalGridStyle.current
    val size = with(LocalDensity.current) { style.iconPx.toSp() }
    BasicText(
        text = glyph,
        modifier =
            modifier.semantics {
                if (description.isNotEmpty()) {
                    contentDescription = description
                }
            },
        style =
            androidx.compose.ui.text.TextStyle(
                color = Color(color ?: style.colors.foreground).copy(alpha = alpha),
                fontSize = size,
                fontFamily = omadroidFont(),
                textAlign = TextAlign.Center,
            ),
    )
}

@Composable
fun IconButton(
    glyph: String,
    description: String,
    modifier: Modifier = Modifier,
    color: Int? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Glyph(glyph, color = color)
    }
}

@Composable
fun Field(
    hint: String,
    modifier: Modifier = Modifier,
    value: String = "",
    onClick: (() -> Unit)? = null,
) {
    val style = LocalGridStyle.current
    val pad = with(LocalDensity.current) { style.spacePx.toDp() }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(style.colors.darkBackground))
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(pad),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = value.ifEmpty { hint },
            color = if (value.isEmpty()) style.colors.muted else style.colors.foreground,
            description = hint,
            align = TextAlign.Start,
        )
    }
}

@Composable
fun Check(selected: Boolean) {
    val style = LocalGridStyle.current
    if (selected) {
        Glyph(IconGlyphs.CHECK, color = style.colors.accent)
    }
}
