package com.omadroid.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.omadroid.launcher.widget.IconGlyphs

@Composable
fun StackScope.Input(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    slot: Slot = Slot.grow(),
    icon: String? = null,
    accent: Boolean = true,
    readOnly: Boolean = false,
    autoFocus: Boolean = false,
    onClick: (() -> Unit)? = null,
    onFocus: (() -> Unit)? = null,
    onSubmit: (() -> Unit)? = null,
) {
    val style = LocalGridStyle.current
    val density = LocalDensity.current
    val inset = with(density) { style.spacePx.toDp() }
    val iconBox = with(density) { style.innerPx.toDp() }
    val font = omadroidFont()
    val textSize = with(density) { style.textSizePx.toSp() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused && !readOnly
    val railAlpha by animateFloatAsState(
        targetValue = if (accent && active) 1f else 0f,
        animationSpec = tween(140),
        label = "input-rail",
    )
    val requester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            requester.requestFocus()
        }
    }
    val selection =
        TextSelectionColors(
            handleColor = Color(style.colors.accent),
            backgroundColor = Color(style.colors.selection).copy(alpha = 0.45f),
        )
    val textStyle =
        TextStyle(
            color = Color(style.colors.foreground),
            fontSize = textSize,
            fontFamily = font,
            textAlign = TextAlign.Start,
        )
    val hintStyle = textStyle.copy(color = Color(style.colors.muted))
    Node(slot) {
        CompositionLocalProvider(LocalTextSelectionColors provides selection) {
            Box(
                Modifier
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
                    .semantics { contentDescription = hint },
            ) {
                if (accent) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(inset)
                            .background(Color(style.colors.accent).copy(alpha = railAlpha)),
                    )
                }
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(inset),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(inset),
                ) {
                    if (icon != null) {
                        Box(
                            Modifier.size(iconBox),
                            contentAlignment = Alignment.Center,
                        ) {
                            Glyph(
                                icon,
                                color = if (active) style.colors.accent else style.colors.muted,
                            )
                        }
                    }
                    Box(
                        Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (readOnly) {
                            BasicText(
                                text = value.ifEmpty { hint },
                                style = if (value.isEmpty()) hintStyle else textStyle,
                            )
                        } else {
                            BasicTextField(
                                value = value,
                                onValueChange = onValueChange,
                                modifier =
                                    Modifier
                                        .focusRequester(requester)
                                        .onFocusChanged { focus ->
                                            if (focus.isFocused) {
                                                onFocus?.invoke()
                                            }
                                        },
                                textStyle = textStyle,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions =
                                    KeyboardActions(onSearch = { onSubmit?.invoke() }),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(style.colors.accent)),
                                interactionSource = interaction,
                                decorationBox = { inner ->
                                    if (value.isEmpty()) {
                                        BasicText(text = hint, style = hintStyle)
                                    }
                                    inner()
                                },
                            )
                        }
                    }
                    if (value.isNotEmpty() && !readOnly) {
                        Box(
                            Modifier
                                .size(iconBox)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onValueChange("") },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Glyph(IconGlyphs.CLOSE, color = style.colors.muted, description = "Clear")
                        }
                    }
                }
            }
        }
    }
}
