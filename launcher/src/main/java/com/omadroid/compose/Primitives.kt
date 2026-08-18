package com.omadroid.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omadroid.launcher.widget.GridStyle

@Composable
fun GridStyle.spaceDp(): Dp = with(LocalDensity.current) { spacePx.toDp() }

@Composable
fun GridStyle.cellDp(): Dp = with(LocalDensity.current) { cellPx.toDp() }

/** Fills a [Node] slot. Use [Block] for a row on a [Page]. */
@Composable
fun Cell(
    modifier: Modifier = Modifier,
    align: Alignment = Alignment.CenterStart,
    onClick: (() -> Unit)? = null,
    description: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .then(clickable(onClick, description)),
        contentAlignment = align,
        content = content,
    )
}

/** A page row that wraps its content. Pad and gap come from [Line], [Rule], and [Stack]. */
@Composable
fun Block(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().then(clickable(onClick, description)),
        content = content,
    )
}

@Composable
fun Line(
    text: String,
    modifier: Modifier = Modifier,
    color: Int? = null,
    caption: Boolean = false,
    description: String = text,
    onClick: (() -> Unit)? = null,
) {
    val style = LocalGridStyle.current
    Block(modifier, onClick = onClick, description = description) {
        Box(
            Modifier.fillMaxWidth().height(style.cellDp()),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text,
                color = color ?: style.colors.muted,
                description = description,
                sizePx = if (caption) style.captionSizePx else null,
                align = TextAlign.Start,
            )
        }
    }
}

@Composable
fun Rule() {
    val style = LocalGridStyle.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = style.spaceDp())
            .height(1.dp)
            .background(Color(style.colors.muted).copy(alpha = 0.25f)),
    )
}

@Composable
fun Page(
    modifier: Modifier = Modifier,
    scrollY: Int = 0,
    onScroll: (Int) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
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
                .verticalScroll(scroll, overscrollEffect = NoOverscroll, flingBehavior = NoFling),
        content = content,
    )
}

@Composable
fun StackScope.Chrome(
    title: String,
    leading: String? = null,
    leadingDescription: String = "",
    onLeading: (() -> Unit)? = null,
    trailing: String? = null,
    trailingDescription: String = "",
    onTrailing: (() -> Unit)? = null,
) {
    val style = LocalGridStyle.current
    Node(Slot.units(1)) {
        Box(Modifier.fillMaxSize().background(Color(style.colors.lighterBackground))) {
            Stack(Direction.Horizontal, gap = false, pad = false) {
                if (leading != null && onLeading != null) {
                    Node(Slot.square) {
                        IconButton(leading, leadingDescription, onClick = onLeading)
                    }
                }
                Node(Slot.grow()) {
                    Cell(description = title) {
                        Text(title, description = title, align = TextAlign.Start)
                    }
                }
                if (trailing != null && onTrailing != null) {
                    Node(Slot.square) {
                        IconButton(
                            trailing,
                            trailingDescription,
                            color = style.colors.accent,
                            onClick = onTrailing,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Empty(
    message: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val style = LocalGridStyle.current
    Stack(Direction.Vertical) {
        Node(Slot.grow()) {
            Cell(align = Alignment.Center) {
                Text(message, color = style.colors.muted, description = message)
            }
        }
        if (action != null && onAction != null) {
            Node(Slot.units(1)) {
                Cell(
                    modifier =
                        Modifier
                            .padding(style.spaceDp())
                            .background(Color(style.colors.lighterBackground)),
                    onClick = onAction,
                    align = Alignment.Center,
                    description = action,
                ) {
                    Text(action, color = style.colors.accent, description = action)
                }
            }
        }
    }
}

@Composable
fun Tiles(
    modifier: Modifier = Modifier,
    minSizePx: Int? = null,
    content: LazyGridScope.() -> Unit,
) {
    val style = LocalGridStyle.current
    val space = style.spaceDp()
    val min = with(LocalDensity.current) { (minSizePx ?: style.unitPx * 3).toDp() }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(min),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(space),
        horizontalArrangement = Arrangement.spacedBy(space),
        verticalArrangement = Arrangement.spacedBy(space),
        flingBehavior = NoFling,
        overscrollEffect = NoOverscroll,
        content = content,
    )
}

@Composable
fun Swatches(colors: List<Int>, modifier: Modifier = Modifier) {
    val style = LocalGridStyle.current
    val gap = style.spaceDp()
    val swatch = with(LocalDensity.current) { (style.cellPx / 2).coerceAtLeast(1).toDp() }
    Row(
        modifier = modifier.fillMaxWidth().padding(start = gap, end = gap, bottom = gap),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        colors.forEach { color ->
            Box(
                Modifier
                    .size(swatch)
                    .border(1.dp, Color(style.colors.muted).copy(alpha = 0.45f))
                    .background(Color(color)),
            )
        }
    }
}

@Composable
private fun clickable(onClick: (() -> Unit)?, description: String?): Modifier {
    val click =
        if (onClick != null) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
        } else {
            Modifier
        }
    val labeled =
        if (description != null) {
            Modifier.semantics { contentDescription = description }
        } else {
            Modifier
        }
    return click.then(labeled)
}
