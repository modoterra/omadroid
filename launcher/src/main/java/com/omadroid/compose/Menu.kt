package com.omadroid.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.omadroid.launcher.widget.IconGlyphs
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
    Page(modifier, scrollY, onScroll) {
        spec.sections.forEach { section ->
            if (section.header.isNotEmpty()) {
                Line(section.header, caption = true)
            }
            section.items.forEachIndexed { index, item ->
                Block(
                    onClick = {
                        val current = item.toggled
                        if (current != null) {
                            onItemToggle(item, !current)
                        } else {
                            onItemClick(item)
                        }
                    },
                ) {
                    MenuRow(item)
                }
                if (index < section.items.lastIndex) {
                    Rule()
                }
            }
        }
    }
}

@Composable
private fun MenuRow(item: MenuItem) {
    if (item.swatches.isEmpty()) {
        TitleRow(item)
        return
    }
    Column(Modifier.fillMaxWidth()) {
        TitleRow(item)
        Swatches(item.swatches)
    }
}

@Composable
private fun TitleRow(item: MenuItem) {
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
        when {
            item.toggled != null -> {
                Node(Slot.square) {
                    Cell(align = Alignment.Center) {
                        Glyph(
                            if (item.toggled == true) {
                                IconGlyphs.TOGGLE_ON
                            } else {
                                IconGlyphs.TOGGLE_OFF
                            },
                            color = if (item.toggled == true) style.colors.accent else style.colors.muted,
                        )
                    }
                }
            }
            item.selected -> {
                Node(Slot.square) {
                    Cell(align = Alignment.Center) {
                        Check(true)
                    }
                }
            }
        }
    }
}
