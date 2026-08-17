package com.omadroid.launcher.widget

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout

class GridGroup(
    context: Context,
    look: GridStyle,
    orientation: Int = HORIZONTAL,
) : LinearLayout(context), Node {
    private val host = NodeHost()
    private var look: GridStyle = look

    override val nodes: List<Node>
        get() = host.nodes

    init {
        this.orientation = orientation
        style(look)
    }

    fun add(child: View, params: ViewGroup.LayoutParams) {
        if (child is Node) {
            host.add(child)
        }
        addView(child, params)
    }

    fun reset() {
        host.clear()
        removeAllViews()
    }

    override fun style(next: GridStyle) {
        look = next
        host.style(next)
    }
}

class GridColumn(
    context: Context,
    look: GridStyle,
) : LinearLayout(context), Node {
    private val host = NodeHost()
    private var look: GridStyle = look

    override val nodes: List<Node>
        get() = host.nodes

    init {
        orientation = VERTICAL
        style(look)
    }

    fun add(child: View, params: ViewGroup.LayoutParams) {
        if (child is Node) {
            host.add(child)
        }
        addView(child, params)
    }

    override fun style(next: GridStyle) {
        look = next
        setBackgroundColor(next.colors.background)
        host.style(next)
    }
}

class GridPane(
    context: Context,
    look: GridStyle,
) : FrameLayout(context), Node {
    private val host = NodeHost()
    private var look: GridStyle = look

    override val nodes: List<Node>
        get() = host.nodes

    init {
        style(look)
    }

    fun add(child: View, params: ViewGroup.LayoutParams) {
        if (child is Node) {
            host.add(child)
        }
        addView(child, params)
    }

    override fun style(next: GridStyle) {
        look = next
        setBackgroundColor(next.colors.background)
        host.style(next)
    }
}
