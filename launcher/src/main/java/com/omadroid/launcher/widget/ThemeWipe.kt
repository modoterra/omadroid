package com.omadroid.launcher.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.DecelerateInterpolator

class ThemeWipe(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()
    private var frame: Bitmap? = null
    private var progress: Float = 0f
    private var runner: ValueAnimator? = null

    fun play(snapshot: Bitmap, done: () -> Unit) {
        runner?.cancel()
        frame?.recycle()
        frame = snapshot
        progress = 0f
        val anim =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = WIPE_MS
                interpolator = DecelerateInterpolator()
                addUpdateListener { value ->
                    progress = value.animatedValue as Float
                    invalidate()
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            frame?.recycle()
                            frame = null
                            done()
                        }
                    },
                )
            }
        runner = anim
        anim.start()
    }

    override fun onDraw(canvas: Canvas) {
        val bitmap = frame ?: return
        val slant = width * SLANT
        val travel = width + slant
        val top = travel * progress
        val bottom = top - slant
        clip.reset()
        clip.moveTo(top, 0f)
        clip.lineTo(width.toFloat(), 0f)
        clip.lineTo(width.toFloat(), height.toFloat())
        clip.lineTo(bottom, height.toFloat())
        clip.close()
        val save = canvas.save()
        canvas.clipPath(clip)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restoreToCount(save)
    }

    override fun onDetachedFromWindow() {
        runner?.cancel()
        frame?.recycle()
        frame = null
        super.onDetachedFromWindow()
    }

    companion object {
        private const val WIPE_MS = 360L
        private const val SLANT = 0.18f
    }
}
