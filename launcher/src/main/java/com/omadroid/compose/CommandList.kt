package com.omadroid.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.omadroid.launcher.CommandItem

@Composable
fun CommandList(
    items: List<CommandItem>,
    modifier: Modifier = Modifier,
    onItemClick: (CommandItem) -> Unit = {},
) {
    val style = LocalGridStyle.current
    val density = LocalDensity.current
    val row = with(density) { style.cellPx.toDp() }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState(), overscrollEffect = NoOverscroll, flingBehavior = NoFling),
    ) {
        if (items.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(row),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("No commands", color = style.colors.muted)
            }
        }
        items.forEachIndexed { index, item ->
            Box(
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onItemClick(item) },
                ),
            ) {
                CommandRow(item, row)
            }
            if (index < items.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = with(density) { style.spacePx.toDp() })
                        .height(1.dp)
                        .background(Color(style.colors.muted).copy(alpha = 0.2f)),
                )
            }
        }
    }
}

@Composable
private fun CommandRow(
    item: CommandItem,
    row: androidx.compose.ui.unit.Dp,
) {
    val style = LocalGridStyle.current
    Stack(Direction.Horizontal, Modifier.height(row)) {
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
        if (item.hint != null) {
            Node(Slot.fit) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                    Text(item.hint, color = style.colors.muted, description = item.hint)
                }
            }
        }
    }
}
