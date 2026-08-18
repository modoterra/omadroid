package com.omadroid.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.omadroid.gallery.widget.GridStyle

interface StackScope {
    @Composable
    fun Node(
        slot: Slot = Slot.fit,
        content: @Composable BoxScope.() -> Unit,
    )
}

@Composable
fun Stack(
    direction: Direction,
    modifier: Modifier = Modifier,
    gap: Boolean = true,
    pad: Boolean = gap,
    content: @Composable StackScope.() -> Unit,
) {
    val style = LocalGridStyle.current
    val space = style.spaceDp()
    when (direction) {
        Direction.Horizontal -> {
            Row(
                modifier = modifier.fillMaxSize().then(if (pad) Modifier.padding(space) else Modifier),
                horizontalArrangement = if (gap) Arrangement.spacedBy(space) else Arrangement.Start,
            ) {
                HorizontalStackScope(this, style).content()
            }
        }
        Direction.Vertical -> {
            Column(
                modifier = modifier.fillMaxSize().then(if (pad) Modifier.padding(space) else Modifier),
                verticalArrangement = if (gap) Arrangement.spacedBy(space) else Arrangement.Top,
            ) {
                VerticalStackScope(this, style).content()
            }
        }
    }
}

@Composable
fun StackScope.Spacer(slot: Slot = Slot.grow()) {
    Node(slot) {}
}

@Composable
fun StackScope.Stack(
    direction: Direction,
    slot: Slot = Slot.fit,
    gap: Boolean = true,
    pad: Boolean = gap,
    content: @Composable StackScope.() -> Unit,
) {
    Node(slot) {
        com.omadroid.compose.Stack(direction, Modifier.fillMaxSize(), gap, pad, content)
    }
}

private class HorizontalStackScope(
    private val row: RowScope,
    private val style: GridStyle,
) : StackScope {
    @Composable
    override fun Node(
        slot: Slot,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Box(modifier = with(row) { rowModifier(slot, style) }, content = content)
    }
}

private class VerticalStackScope(
    private val column: ColumnScope,
    private val style: GridStyle,
) : StackScope {
    @Composable
    override fun Node(
        slot: Slot,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Box(modifier = with(column) { columnModifier(slot, style) }, content = content)
    }
}

@Composable
private fun GridStyle.spaceDp(): Dp = with(LocalDensity.current) { spacePx.toDp() }

@Composable
private fun GridStyle.unitsDp(count: Int): Dp = with(LocalDensity.current) { (count * unitPx).toDp() }

@Composable
private fun RowScope.rowModifier(slot: Slot, style: GridStyle): Modifier {
    return when {
        slot.square -> Modifier.fillMaxHeight().aspectRatio(1f)
        slot.units != null -> Modifier.fillMaxHeight().width(style.unitsDp(slot.units))
        slot.grow > 0f -> Modifier.fillMaxHeight().weight(slot.grow)
        else -> Modifier.fillMaxHeight().wrapContentWidth()
    }
}

@Composable
private fun ColumnScope.columnModifier(slot: Slot, style: GridStyle): Modifier {
    return when {
        slot.square -> Modifier.fillMaxWidth().aspectRatio(1f)
        slot.units != null -> Modifier.fillMaxWidth().height(style.unitsDp(slot.units))
        slot.grow > 0f -> Modifier.fillMaxWidth().weight(slot.grow)
        else -> Modifier.fillMaxWidth().wrapContentHeight()
    }
}
