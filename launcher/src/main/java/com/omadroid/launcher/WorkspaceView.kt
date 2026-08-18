package com.omadroid.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.omadroid.compose.Direction
import com.omadroid.compose.Glyph
import com.omadroid.compose.LocalGridStyle
import com.omadroid.compose.NoFling
import com.omadroid.compose.NoOverscroll
import com.omadroid.compose.Slot
import com.omadroid.compose.Spacer
import com.omadroid.compose.Stack
import com.omadroid.compose.Text
import com.omadroid.launcher.widget.IconGlyphs

@Composable
internal fun ActiveWorkspace(
    workspace: Workspace,
    onClientClick: (WorkspaceClient) -> Unit,
    onFocusPage: (Int) -> Unit,
) {
    when (workspace.layout) {
        WorkspaceLayout.Dwindle -> DwindleWorkspace(workspace.clients, onClientClick)
        WorkspaceLayout.Scrolling ->
            key(workspace.id) {
                ScrollingWorkspace(workspace, onClientClick, onFocusPage)
            }
    }
}

@Composable
private fun DwindleWorkspace(
    clients: List<WorkspaceClient>,
    onClientClick: (WorkspaceClient) -> Unit,
) {
    val tree = dwindleTree(clients.map { it.id }) ?: return
    DwindleBranch(tree, clients.associateBy { it.id }, onClientClick)
}

@Composable
private fun DwindleBranch(
    node: DwindleNode,
    byId: Map<String, WorkspaceClient>,
    onClientClick: (WorkspaceClient) -> Unit,
) {
    when (node) {
        is DwindleNode.Leaf -> {
            val client = byId[node.id] ?: return
            WorkspaceTile(client, onClick = { onClientClick(client) })
        }
        is DwindleNode.Split -> {
            val direction =
                if (node.axis == DwindleAxis.Vertical) {
                    Direction.Horizontal
                } else {
                    Direction.Vertical
                }
            Stack(direction, gap = true, pad = false) {
                Node(Slot.grow()) {
                    DwindleBranch(node.first, byId, onClientClick)
                }
                Node(Slot.grow()) {
                    DwindleBranch(node.second, byId, onClientClick)
                }
            }
        }
    }
}

@Composable
private fun ScrollingWorkspace(
    workspace: Workspace,
    onClientClick: (WorkspaceClient) -> Unit,
    onFocusPage: (Int) -> Unit,
) {
    val clients = workspace.clients
    if (clients.isEmpty()) {
        return
    }
    val style = LocalGridStyle.current
    val density = LocalDensity.current
    val index = clampScrollIndex(workspace.focusedIndex, clients.size)
    val scroll = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pagePx = constraints.maxWidth
        val edge = with(density) { style.unitPx.toDp() }
        LaunchedEffect(index, pagePx) {
            if (pagePx <= 0) {
                return@LaunchedEffect
            }
            val target = index * pagePx
            if (scroll.value != target) {
                scroll.animateScrollTo(target)
            }
        }
        Row(
            Modifier
                .fillMaxSize()
                .horizontalScroll(
                    state = scroll,
                    enabled = false,
                    overscrollEffect = NoOverscroll,
                    flingBehavior = NoFling,
                ),
        ) {
            val pageDp = with(density) { pagePx.toDp() }
            clients.forEach { client ->
                Box(
                    Modifier
                        .width(pageDp)
                        .fillMaxHeight(),
                ) {
                    WorkspaceTile(client, onClick = { onClientClick(client) })
                }
            }
        }
        val threshold = style.unitPx / 2f
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(edge)
                .pointerInput(index, clients.size, threshold) {
                    var acc = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { acc = 0f },
                        onHorizontalDrag = { _, amount -> acc += amount },
                        onDragEnd = {
                            if (acc > threshold) {
                                onFocusPage(prevScrollIndex(index, clients.size))
                            }
                        },
                    )
                },
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(edge)
                .pointerInput(index, clients.size, threshold) {
                    var acc = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { acc = 0f },
                        onHorizontalDrag = { _, amount -> acc += amount },
                        onDragEnd = {
                            if (acc < -threshold) {
                                onFocusPage(nextScrollIndex(index, clients.size))
                            }
                        },
                    )
                },
        )
    }
}

@Composable
private fun WorkspaceTile(
    client: WorkspaceClient,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val style = LocalGridStyle.current
    Box(
        modifier
            .fillMaxSize()
            .background(Color(style.colors.lighterBackground))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = client.title },
        contentAlignment = Alignment.Center,
    ) {
        Stack(Direction.Vertical, gap = true, pad = false) {
            Spacer(Slot.grow())
            Node(Slot.fit) {
                Glyph(IconGlyphs.APP)
            }
            Node(Slot.fit) {
                Text(client.title)
            }
            Spacer(Slot.grow())
        }
    }
}
