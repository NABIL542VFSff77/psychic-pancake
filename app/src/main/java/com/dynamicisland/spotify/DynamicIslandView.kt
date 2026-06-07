package com.dynamicisland.spotify

import android.animation.*
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class DynamicIslandView(
    context: Context,
    private val layoutParams: WindowManager.LayoutParams,
    private val windowManager: WindowManager
) : LinearLayout(context) {

    private val albumArt: ImageView
    private val trackTitle: TextView
    private val waveformView: WaveformView
    private var animatorSet: AnimatorSet? = null

    private val density = resources.displayMetrics.density
    private val cornerRadius = 32f * density

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val rectF = RectF()

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        setBackgroundColor(Color.TRANSPARENT)
        setPadding(
            (10 * density).toInt(),
            (6 * density).toInt(),
            (14 * density).toInt(),
            (6 * density).toInt()
        )

        albumArt = ImageView(context).apply {
            layoutParams = LayoutParams(
                (24 * density).toInt(),
                (24 * density).toInt()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 6 * density)
                }
            }
        }

        trackTitle = TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (8 * density).toInt()
                marginEnd = (8 * density).toInt()
            }
            setTextColor(Color.WHITE)
            textSize = 10f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
            isFocusable = true
            isFocusableInTouchMode = true
            isSelected = true
        }

        waveformView = WaveformView(context).apply {
            layoutParams = LayoutParams(
                (40 * density).toInt(),
                (20 * density).toInt()
            )
        }

        addView(albumArt)
        addView(trackTitle)
        addView(waveformView)

        scaleX = 0f
        alpha = 0f

        setupDrag()
    }

    override fun dispatchDraw(canvas: Canvas) {
        rectF.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint)
        super.dispatchDraw(canvas)
    }

    private fun setupDrag() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(this, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    context.getSharedPreferences(
                        FloatingMusicService.PREFS_NAME, Context.MODE_PRIVATE
                    ).edit()
                        .putInt(FloatingMusicService.PREF_X, layoutParams.x)
                        .putInt(FloatingMusicService.PREF_Y, layoutParams.y)
                        .apply()
                    true
                }
                else -> false
            }
        }
    }

    fun updateTrackInfo(title: String, artist: String, art: Bitmap?) {
        post {
            trackTitle.text = if (artist.isNotEmpty()) "$title • $artist" else title
            if (art != null) {
                albumArt.setImageBitmap(art)
            } else {
                albumArt.setImageResource(R.drawable.ic_music_note)
            }
        }
    }

    fun animateIn() {
        animatorSet?.cancel()
        visibility = VISIBLE
        waveformView.startWave()

        val scaleXAnim = ObjectAnimator.ofFloat(this, "scaleX", 0f, 1.08f, 1f).apply {
            duration = 520
            interpolator = OvershootInterpolator(1.2f)
        }
        val scaleYAnim = ObjectAnimator.ofFloat(this, "scaleY", 0f, 1.08f, 1f).apply {
            duration = 520
            interpolator = OvershootInterpolator(1.2f)
        }
        val alphaAnim = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
            duration = 250
        }

        animatorSet = AnimatorSet().apply {
            playTogether(scaleXAnim, scaleYAnim, alphaAnim)
            start()
        }
    }

    fun animateOut(onEnd: () -> Unit) {
        animatorSet?.cancel()
        waveformView.stopWave()

        val scaleXAnim = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0f).apply {
            duration = 380
            interpolator = DecelerateInterpolator(1.8f)
        }
        val scaleYAnim = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0f).apply {
            duration = 380
            interpolator = DecelerateInterpolator(1.8f)
        }
        val alphaAnim = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
            duration = 300
            startDelay = 80
        }

        animatorSet = AnimatorSet().apply {
            playTogether(scaleXAnim, scaleYAnim, alphaAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = GONE
                    onEnd()
                }
            })
            start()
        }
    }

    fun hideInstantly() {
        animatorSet?.cancel()
        waveformView.stopWave()
        visibility = GONE
    }

    fun resize(widthDp: Int, heightDp: Int) {
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()
        layoutParams.width = widthPx
        layoutParams.height = heightPx
        windowManager.updateViewLayout(this, layoutParams)
    }
}
