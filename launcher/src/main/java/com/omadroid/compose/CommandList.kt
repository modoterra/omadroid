package com.omadroid.compose

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.omadroid.launcher.CommandItem

@Composable
fun CommandList(
    items: List<CommandItem>,
    modifier: Modifier = Modifier,
    onItemClick: (CommandItem) -> Unit = {},
) {
    Page(modifier) {
        if (items.isEmpty()) {
            Line("No commands")
        }
        items.forEachIndexed { index, item ->
            Block(onClick = { onItemClick(item) }) {
                CommandRow(item)
            }
            if (index < items.lastIndex) {
                Rule()
            }
        }
    }
}

@Composable
private fun CommandRow(item: CommandItem) {
    val style = LocalGridStyle.current
    Stack(Direction.Horizontal, Modifier.height(style.cellDp())) {
        if (item.icon != null) {
            Node(Slot.square) {
                Cell(align = Alignment.Center) {
                    Glyph(item.icon)
                }
            }
        }
        Node(Slot.grow()) {
            Cell(description = item.title) {
                Text(item.title, description = item.title)
            }
        }
        if (item.hint != null) {
            Node(Slot.fit) {
                Cell(align = Alignment.CenterEnd) {
                    Text(item.hint, color = style.colors.muted, description = item.hint)
                }
            }
        }
    }
}
