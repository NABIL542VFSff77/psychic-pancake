package com.dynamicisland.spotify

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

class WaveformView(context: Context) : View(context) {

    private val barCount = 5
    private val barWidth = 3f * resources.displayMetrics.density
    private val barGap = 3f * resources.displayMetrics.density
    private val maxBarHeight = 16f * resources.displayMetrics.density
    private val minBarHeight = 3f * resources.displayMetrics.density

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
    }

    private var phase = 0f
    private var isPlaying = false
    private var animator: ValueAnimator? = null

    private val barHeights = FloatArray(barCount) { minBarHeight }

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun startAnimator() {
        animator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                phase = anim.animatedValue as Float
                for (i in 0 until barCount) {
                    val offset = i * 0.72f
                    val t = sin((phase + offset).toDouble()).toFloat()
                    barHeights[i] = minBarHeight + (maxBarHeight - minBarHeight) * ((t + 1f) / 2f)
                }
                invalidate()
            }
            start()
        }
    }

    fun startWave() {
        if (isPlaying) return
        isPlaying = true
        startAnimator()
    }

    fun stopWave() {
        isPlaying = false
        animator?.cancel()
        animator = null
        for (i in 0 until barCount) barHeights[i] = minBarHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val totalWidth = barCount * barWidth + (barCount - 1) * barGap
        var startX = (width - totalWidth) / 2f

        for (i in 0 until barCount) {
            val barH = barHeights[i]
            val top = (height - barH) / 2f
            val bottom = top + barH
            val left = startX
            val right = startX + barWidth
            paint.strokeWidth = barWidth
            canvas.drawRoundRect(left, top, right, bottom, barWidth / 2, barWidth / 2, paint)
            startX += barWidth + barGap
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopWave()
    }
}
