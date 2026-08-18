package com.omadroid.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.omadroid.launcher.NavRoute
import com.omadroid.launcher.NavStack
import com.omadroid.launcher.widget.IconGlyphs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

const val SHEET_IN_MS = 320
const val SHEET_OUT_MS = 240
const val SHEET_BACK_SCALE = 0.94f

private val SheetInEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
private val SheetOutEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

@Composable
fun rememberSheetProgress(open: Boolean): Animatable<Float, AnimationVector1D> {
    val progress = remember { Animatable(if (open) 1f else 0f) }
    LaunchedEffect(open) {
        val target = if (open) 1f else 0f
        if (progress.value == target) {
            return@LaunchedEffect
        }
        if (open) {
            progress.animateTo(1f, tween(SHEET_IN_MS, easing = SheetInEasing))
        } else {
            progress.animateTo(0f, tween(SHEET_OUT_MS, easing = SheetOutEasing))
        }
    }
    return progress
}

@Composable
fun Sheet(
    stack: NavStack,
    progress: Animatable<Float, AnimationVector1D>,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (NavRoute) -> Unit,
) {
    val style = LocalGridStyle.current
    var lastRoute by remember { mutableStateOf(stack.current) }
    stack.current?.let { lastRoute = it }
    val route = stack.current ?: lastRoute
    if (progress.value <= 0.001f || route == null) {
        return
    }
    val density = LocalDensity.current
    val peekPx = style.unitPx.toFloat()
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val sheetWidth = (widthPx - peekPx).coerceAtLeast(1f)
        val x = (progress.value - 1f) * sheetWidth
        val sheetRight = x + sheetWidth
        val gapPx = (widthPx - sheetRight).coerceAtLeast(0f)
        val shadowAlpha = (gapPx / peekPx).coerceIn(0f, 1f) * 0.4f
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(with(density) { peekPx.toDp() })
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        val drag =
            rememberDraggableState { delta ->
                val next = (progress.value + delta / sheetWidth).coerceIn(0f, 1f)
                scope.launch { progress.snapTo(next) }
            }
        Box(
            Modifier
                .fillMaxHeight()
                .width(with(density) { sheetWidth.toDp() })
                .offset { IntOffset(x.roundToInt(), 0) }
                .draggable(
                    state = drag,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        scope.launch {
                            val flingOut = velocity < -1200f
                            val pastMid = progress.value < 0.55f
                            if (flingOut || pastMid) {
                                progress.animateTo(0f, tween(SHEET_OUT_MS, easing = SheetOutEasing))
                                onDismiss()
                            } else {
                                progress.animateTo(1f, tween(SHEET_IN_MS, easing = SheetInEasing))
                            }
                        }
                    },
                )
                .background(Color(style.colors.background)),
        ) {
            Stack(Direction.Vertical, gap = false) {
                Node(Slot.units(1)) {
                    Box(Modifier.fillMaxSize().background(Color(style.colors.lighterBackground))) {
                        Stack(Direction.Horizontal) {
                            Node(Slot.square) {
                                IconButton(IconGlyphs.BACK, "Back", onClick = onBack)
                            }
                            Node(Slot.grow()) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(route.title, description = route.title)
                                }
                            }
                        }
                    }
                }
                Node(Slot.grow()) {
                    StackPages(stack, content)
                }
            }
            if (shadowAlpha > 0.01f) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(with(density) { style.unitPx.toDp() })
                        .offset(x = with(density) { style.unitPx.toDp() })
                        .background(
                            Brush.horizontalGradient(
                                colors =
                                    listOf(
                                        Color.Black.copy(alpha = shadowAlpha),
                                        Color.Transparent,
                                    ),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun StackPages(
    stack: NavStack,
    content: @Composable (NavRoute) -> Unit,
) {
    var lastRoutes by remember { mutableStateOf(stack.routes) }
    var from by remember { mutableStateOf<NavRoute?>(null) }
    var to by remember { mutableStateOf<NavRoute?>(null) }
    var pushing by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(1f) }
    var runId by remember { mutableIntStateOf(0) }

    val routes = stack.routes
    if (routes != lastRoutes) {
        val oldTop = lastRoutes.lastOrNull()
        val newTop = routes.lastOrNull()
        if (oldTop != null && newTop != null && oldTop.id != newTop.id) {
            from = oldTop
            to = newTop
            pushing = routes.size > lastRoutes.size
            progress = 0f
            runId++
        } else {
            from = null
            to = null
            progress = 1f
        }
        lastRoutes = routes
    }

    LaunchedEffect(runId) {
        val outgoing = from
        val incoming = to
        if (outgoing == null || incoming == null || runId == 0) {
            return@LaunchedEffect
        }
        val spec =
            if (pushing) {
                tween<Float>(SHEET_IN_MS, easing = SheetInEasing)
            } else {
                tween(SHEET_OUT_MS, easing = SheetOutEasing)
            }
        animate(0f, 1f, animationSpec = spec) { value, _ ->
            progress = value
        }
        from = null
        to = null
        progress = 1f
    }

    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val w = constraints.maxWidth.toFloat()
        val p = progress
        val outgoing = from
        val incoming = to
        if (outgoing != null && incoming != null) {
            val back = if (pushing) outgoing else incoming
            val front = if (pushing) incoming else outgoing
            val backAlpha = if (pushing) 1f - p else p
            val frontX = if (pushing) (1f - p) * w else p * w
            key(back.id) {
                PageLayer(back, 0f, backAlpha, content)
            }
            key(front.id) {
                PageLayer(front, frontX, 1f, content)
            }
        } else {
            val current = stack.current
            if (current != null) {
                key(current.id) {
                    PageLayer(current, 0f, 1f, content)
                }
            }
        }
    }
}

@Composable
private fun PageLayer(
    route: NavRoute,
    x: Float,
    alpha: Float,
    content: @Composable (NavRoute) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(x.roundToInt(), 0) }
            .alpha(alpha),
    ) {
        content(route)
    }
}
