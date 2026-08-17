package com.omadroid.launcher.widget

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.omadroid.launcher.NavRoute
import com.omadroid.launcher.NavStack

class GridSheet(
    context: Context,
    private val style: GridStyle,
) : LinearLayout(context) {
    var stack: NavStack = NavStack()
        private set

    var render: (NavRoute) -> View = { _ -> View(context) }
    var onChanged: (NavStack) -> Unit = {}

    private val titleView: GridText
    private val backButton: GridIconButton
    private val body: FrameLayout

    val isOpen: Boolean
        get() = stack.isOpen && visibility == VISIBLE

    init {
        orientation = VERTICAL
        setBackgroundColor(style.colors.background)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        visibility = GONE
        val header =
            GridRow(context, style).apply {
                contentDescription = "Sheet"
            }
        backButton =
            GridIconButton(context, style).apply {
                text = IconGlyphs.BACK
                contentDescription = "Back"
                setOnClickListener { popOrDismiss() }
            }
        header.addView(backButton, gridCellParams(style))
        titleView = GridText(context, style)
        header.addView(
            titleView,
            LinearLayout.LayoutParams(0, style.innerPx, 1f).apply {
                marginStart = style.spacePx
            },
        )
        addView(
            header,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, style.cellPx),
        )
        body =
            FrameLayout(context).apply {
                setBackgroundColor(style.colors.background)
            }
        addView(
            body,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
    }

    fun show(route: NavRoute) {
        animate().cancel()
        stack = NavStack.root(route)
        bind()
        translationY = offscreenY()
        visibility = VISIBLE
        post {
            translationY = offscreenY()
            animate().translationY(0f).setDuration(SLIDE_MS).start()
        }
        onChanged(stack)
    }

    fun push(route: NavRoute) {
        if (!stack.isOpen) {
            show(route)
            return
        }
        stack = stack.push(route)
        bind()
        onChanged(stack)
    }

    fun popOrDismiss(): Boolean {
        if (!stack.isOpen) {
            return false
        }
        if (stack.canPop) {
            stack = stack.pop()
            bind()
            onChanged(stack)
            return true
        }
        dismiss()
        return true
    }

    fun dismiss() {
        if (visibility != VISIBLE) {
            stack = NavStack()
            onChanged(stack)
            return
        }
        animate()
            .translationY(height.toFloat())
            .setDuration(SLIDE_MS)
            .withEndAction {
                visibility = GONE
                translationY = offscreenY()
                body.removeAllViews()
                stack = NavStack()
                onChanged(stack)
            }
            .start()
    }

    private fun bind() {
        val route = stack.current
        if (route == null) {
            body.removeAllViews()
            return
        }
        titleView.text = route.title
        backButton.visibility = VISIBLE
        body.removeAllViews()
        body.addView(
            render(route),
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.TOP,
            ),
        )
    }

    private fun offscreenY(): Float {
        val measured = height
        return if (measured > 0) {
            measured.toFloat()
        } else {
            resources.displayMetrics.heightPixels.toFloat()
        }
    }

    companion object {
        private const val SLIDE_MS = 200L
    }
}
