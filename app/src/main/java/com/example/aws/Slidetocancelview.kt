package com.example.aws

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils

class SlideToCancelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onCancelled: (() -> Unit)? = null

    private val trackPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrowPaint  = Paint(Paint.ANTI_ALIAS_FLAG)

    private val trackRect   = RectF()
    private val fillRect    = RectF()

    private var thumbX      = 0f
    private var thumbRadius = 0f
    private var dragging    = false
    private var progress    = 0f  // 0.0 to 1.0

    private val colorStart  = 0x33FFFFFF.toInt()  // transparent white track
    private val colorEnd    = 0xFFD32F2F.toInt()  // solid red when complete
    private val thumbColor  = 0xFFFFFFFF.toInt()  // white thumb

    init {
        textPaint.apply {
            color     = 0xCCFFFFFF.toInt()
            textSize  = 42f
            textAlign = Paint.Align.CENTER
        }
        arrowPaint.apply {
            color       = 0xCCFFFFFF.toInt()
            strokeWidth = 4f
            style       = Paint.Style.STROKE
            strokeCap   = Paint.Cap.ROUND
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        thumbRadius = h / 2f - 6f
        thumbX      = thumbRadius + 6f
        trackRect.set(0f, 0f, w.toFloat(), h.toFloat())
        updateFillRect()
    }

    private fun updateFillRect() {
        fillRect.set(0f, 0f, thumbX + thumbRadius, height.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        val cornerR = height / 2f

        // Track background
        trackPaint.color = colorStart
        canvas.drawRoundRect(trackRect, cornerR, cornerR, trackPaint)

        // Red fill that grows as thumb slides right
        val blended = ColorUtils.blendARGB(0x22FF5252.toInt(), colorEnd, progress)
        fillPaint.color = blended
        canvas.drawRoundRect(fillRect, cornerR, cornerR, fillPaint)

        // Label text — fades out as thumb moves right
        val textAlpha = ((1f - progress) * 200).toInt()
        textPaint.alpha = textAlpha
        val label = "← Slide to cancel session"
        canvas.drawText(
            label,
            width / 2f + thumbRadius,
            height / 2f + textPaint.textSize / 3f,
            textPaint
        )

        // Thumb circle
        thumbPaint.color = thumbColor
        canvas.drawCircle(thumbX, height / 2f, thumbRadius, thumbPaint)

        // Arrow on thumb
        val cx  = thumbX
        val cy  = height / 2f
        val ar  = thumbRadius * 0.4f
        arrowPaint.color = if (progress > 0.7f) colorEnd else 0xFF2E4F3D.toInt()
        canvas.drawLine(cx - ar, cy, cx + ar, cy, arrowPaint)
        canvas.drawLine(cx + ar * 0.3f, cy - ar * 0.6f, cx + ar, cy, arrowPaint)
        canvas.drawLine(cx + ar * 0.3f, cy + ar * 0.6f, cx + ar, cy, arrowPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val maxX = width - thumbRadius - 6f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Only start drag if touching the thumb
                if (Math.abs(event.x - thumbX) < thumbRadius * 1.5f) {
                    dragging = true
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                thumbX    = event.x.coerceIn(thumbRadius + 6f, maxX)
                progress  = (thumbX - thumbRadius - 6f) / (maxX - thumbRadius - 6f)
                updateFillRect()
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!dragging) return false
                dragging = false

                if (progress >= 0.85f) {
                    // Fully slid — trigger cancel
                    onCancelled?.invoke()
                } else {
                    // Snap back to start
                    animateBack()
                }
            }
        }
        return true
    }

    private fun animateBack() {
        val startX    = thumbX
        val targetX   = thumbRadius + 6f
        val duration  = 250L
        val startTime = System.currentTimeMillis()

        post(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val t       = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                // Ease out
                val eased   = 1f - (1f - t) * (1f - t)
                thumbX      = startX + (targetX - startX) * eased
                progress    = (thumbX - thumbRadius - 6f) /
                        ((width - thumbRadius - 6f) - thumbRadius - 6f)
                updateFillRect()
                invalidate()

                if (t < 1f) post(this)
                else {
                    thumbX   = targetX
                    progress = 0f
                    updateFillRect()
                    invalidate()
                }
            }
        })
    }

    // Reset to initial state (call after session cancelled)
    fun reset() {
        thumbX   = thumbRadius + 6f
        progress = 0f
        updateFillRect()
        invalidate()
    }
}