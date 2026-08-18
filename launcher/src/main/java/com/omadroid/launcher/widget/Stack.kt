package com.omadroid.launcher.widget

import android.content.Context
import android.view.View
import android.view.ViewGroup

enum class Direction {
    Horizontal,
    Vertical,
}

data class Slot(
    val grow: Float = 0f,
    val units: Int? = null,
    val square: Boolean = false,
) {
    companion object {
        fun grow(weight: Float = 1f) = Slot(grow = weight)

        fun units(count: Int) = Slot(units = count)

        val square = Slot(square = true)

        val fit = Slot()
    }
}

/** Empty Node used to soak leftover space (left/center/right). */
class Spacer(
    context: Context,
    private var style: GridStyle,
) : View(context), Node {
    override val nodes: List<Node>
        get() = emptyList()

    override fun style(next: GridStyle) {
        style = next
    }
}

/**
 * One-axis stack. Children are Nodes (and Views).
 * Cross axis stretches. Main axis uses [Slot].
 */
open class Stack(
    context: Context,
    private var style: GridStyle,
    val direction: Direction,
) : ViewGroup(context), Node {
    private val host = NodeHost()
    private val slots = mutableListOf<Slot>()

    override val nodes: List<Node>
        get() = host.nodes

    init {
        setWillNotDraw(true)
    }

    fun add(child: Node, slot: Slot = Slot.fit) {
        val view = child as? View ?: error("Stack children must be Views")
        host.add(child)
        slots.add(slot)
        addView(view)
    }

    fun reset() {
        host.clear()
        slots.clear()
        removeAllViews()
    }

    override fun style(next: GridStyle) {
        style = next
        host.style(next)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val pad = style.spacePx
        val innerW = (MeasureSpec.getSize(widthMeasureSpec) - pad * 2).coerceAtLeast(0)
        val innerH = (MeasureSpec.getSize(heightMeasureSpec) - pad * 2).coerceAtLeast(0)
        val row = direction == Direction.Horizontal
        val mainInner = if (row) innerW else innerH
        val crossInner = if (row) innerH else innerW
        val bases = IntArray(childCount)
        for (index in 0 until childCount) {
            val slot = slots[index]
            val child = getChildAt(index)
            bases[index] =
                when {
                    slot.square -> crossInner
                    slot.units != null -> slot.units * style.unitPx
                    slot.grow > 0f -> 0
                    else -> {
                        val mainSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                        val crossSpec = MeasureSpec.makeMeasureSpec(crossInner, MeasureSpec.EXACTLY)
                        if (row) {
                            child.measure(mainSpec, crossSpec)
                            child.measuredWidth
                        } else {
                            child.measure(crossSpec, mainSpec)
                            child.measuredHeight
                        }
                    }
                }
        }
        val grows = FloatArray(childCount) { slots[it].grow }
        val mains = stackDistribute(mainInner, pad, bases, grows)
        for (index in 0 until childCount) {
            val childW = if (row) mains[index] else crossInner
            val childH = if (row) crossInner else mains[index]
            getChildAt(index).measure(
                MeasureSpec.makeMeasureSpec(childW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(childH, MeasureSpec.EXACTLY),
            )
        }
        setMeasuredDimension(
            resolveSize(innerW + pad * 2, widthMeasureSpec),
            resolveSize(innerH + pad * 2, heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val pad = style.spacePx
        val row = direction == Direction.Horizontal
        var main = pad
        val cross = pad
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val mainSize = if (row) child.measuredWidth else child.measuredHeight
            if (row) {
                child.layout(main, cross, main + child.measuredWidth, cross + child.measuredHeight)
            } else {
                child.layout(cross, main, cross + child.measuredWidth, main + child.measuredHeight)
            }
            main += mainSize + pad
        }
    }
}

fun stackDistribute(
    innerMain: Int,
    gap: Int,
    bases: IntArray,
    grows: FloatArray,
): IntArray {
    val count = bases.size
    if (count == 0) {
        return intArrayOf()
    }
    val gaps = if (count > 1) (count - 1) * gap else 0
    val budget = (innerMain - gaps).coerceAtLeast(0)
    val sizes = bases.copyOf()
    var used = 0
    var growTotal = 0f
    for (index in 0 until count) {
        used += sizes[index]
        growTotal += grows[index]
    }
    var leftover = (budget - used).coerceAtLeast(0)
    if (growTotal <= 0f || leftover == 0) {
        return sizes
    }
    for (index in 0 until count) {
        if (grows[index] <= 0f) {
            continue
        }
        val share = (leftover * grows[index] / growTotal).toInt()
        sizes[index] += share
        leftover -= share
        growTotal -= grows[index]
    }
    if (leftover > 0) {
        for (index in count - 1 downTo 0) {
            if (grows[index] > 0f) {
                sizes[index] += leftover
                break
            }
        }
    }
    return sizes
}
