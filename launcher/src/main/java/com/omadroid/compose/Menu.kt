package com.omadroid.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.omadroid.launcher.widget.MenuItem
import com.omadroid.launcher.widget.MenuSpec

@Composable
fun Menu(
    spec: MenuSpec,
    modifier: Modifier = Modifier,
    scrollY: Int = 0,
    onScroll: (Int) -> Unit = {},
    onItemClick: (MenuItem) -> Unit = {},
    onItemToggle: (MenuItem, Boolean) -> Unit = { _, _ -> },
) {
    val style = LocalGridStyle.current
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    LaunchedEffect(scrollY) {
        if (scrollY != scroll.value) {
            scroll.scrollTo(scrollY)
        }
    }
    LaunchedEffect(scroll.value) {
        onScroll(scroll.value)
    }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(
                    scroll,
                    overscrollEffect = NoOverscroll,
                    flingBehavior = NoFling,
                ),
    ) {
        spec.sections.forEach { section ->
            if (section.header.isNotEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(with(density) { style.cellPx.toDp() }),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        section.header,
                        color = style.colors.muted,
                        description = section.header,
                        sizePx = style.captionSizePx,
                    )
                }
            }
            section.items.forEachIndexed { index, item ->
                Box(
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            val current = item.toggled
                            if (current != null) {
                                onItemToggle(item, !current)
                            } else {
                                onItemClick(item)
                            }
                        },
                    ),
                ) {
                    MenuRow(item)
                }
                if (index < section.items.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = with(density) { style.spacePx.toDp() })
                            .height(1.dp)
                            .background(Color(style.colors.muted).copy(alpha = 0.25f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuRow(item: MenuItem) {
    val style = LocalGridStyle.current
    val density = LocalDensity.current
    val titleH = with(density) { style.cellPx.toDp() }
    if (item.swatches.isEmpty()) {
        TitleRow(item, titleH)
        return
    }
    val gap = with(density) { style.spacePx.toDp() }
    Column(Modifier.fillMaxWidth()) {
        TitleRow(item, titleH)
        Row(
            Modifier
                .fillMaxWidth()
                .height(titleH)
                .padding(start = gap, end = gap, bottom = gap),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item.swatches.forEach { color ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(color)),
                )
            }
        }
    }
}

@Composable
private fun TitleRow(
    item: MenuItem,
    height: androidx.compose.ui.unit.Dp,
) {
    val style = LocalGridStyle.current
    Stack(Direction.Horizontal, Modifier.height(height)) {
        if (item.icon != null) {
            Node(Slot.square) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Glyph(item.icon)
                }
            }
        }
        Node(Slot.grow()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = item.title },
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(item.title, description = item.title)
            }
        }
        when {
            item.toggled != null -> {
                Node(Slot.square) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Glyph(
                            if (item.toggled == true) {
                                com.omadroid.launcher.widget.IconGlyphs.TOGGLE_ON
                            } else {
                                com.omadroid.launcher.widget.IconGlyphs.TOGGLE_OFF
                            },
                            color = if (item.toggled == true) style.colors.accent else style.colors.muted,
                        )
                    }
                }
            }
            item.selected -> {
                Node(Slot.square) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Check(true)
                    }
                }
            }
        }
    }
}
