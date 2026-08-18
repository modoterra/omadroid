package com.omadroid.compose

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/** No glow, no stretch. Scroll stops at the content edge. */
object NoOverscroll : OverscrollEffect {
    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset = performScroll(delta)

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        performFling(velocity)
    }

    override val isInProgress: Boolean
        get() = false
}

object NoFling : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float = 0f
}

object NoOverscrollFactory : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = NoOverscroll

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = other is NoOverscrollFactory
}
