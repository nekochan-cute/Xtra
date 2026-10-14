package com.github.andreyasadchy.xtra.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View

/**
 * Non-interactive, Canvas-based scrolling comments. Uses a bounded number of
 * messages and reserves lanes so comments cannot overtake one another.
 */
class DanmakuOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private data class Comment(
        val text: String,
        val widthPx: Float,
        val baselinePx: Float,
        val startedAtMs: Long,
        val speedPxPerMs: Float,
    )

    private val density = resources.displayMetrics.density
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 20f * resources.displayMetrics.scaledDensity
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(outlinePaint).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val laneHeightPx = (textPaint.fontSpacing + 6f * density).toInt().coerceAtLeast(1)
    private val scheduler = DanmakuLaneScheduler(16f * density)
    private val comments = ArrayList<Comment>()

    init {
        // All player gestures should pass through this overlay.
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun addComment(message: String) {
        if (!isShown || width <= 0 || height < laneHeightPx) return
        val text = message.replace('\n', ' ').replace('\r', ' ').trim().take(MAX_TEXT_LENGTH)
        if (text.isEmpty()) return

        val now = SystemClock.uptimeMillis()
        val speedPxPerMs = width / TRAVEL_TIME_MS
        removeExpired(now)
        val textWidth = textPaint.measureText(text)
        val lane = scheduler.claimLane(
            nowMs = now,
            textWidthPx = textWidth,
            laneCount = (height / laneHeightPx).coerceAtMost(MAX_LANES),
            speedPxPerMs = speedPxPerMs,
        ) ?: return

        if (comments.size >= MAX_COMMENTS) comments.removeAt(0)
        val baseline = (lane + 1) * laneHeightPx -
            (laneHeightPx - textPaint.fontSpacing) / 2f - textPaint.fontMetrics.bottom
        comments.add(Comment(text, textWidth, baseline, now, speedPxPerMs))
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (comments.isEmpty()) return

        val now = SystemClock.uptimeMillis()
        removeExpired(now)
        if (comments.isEmpty()) return

        val save = canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        comments.forEach { comment ->
            val x = width - (now - comment.startedAtMs) * comment.speedPxPerMs
            canvas.drawText(comment.text, x, comment.baselinePx, outlinePaint)
            canvas.drawText(comment.text, x, comment.baselinePx, textPaint)
        }
        canvas.restoreToCount(save)
        postInvalidateOnAnimation()
    }

    private fun removeExpired(now: Long) {
        comments.removeAll {
            width - (now - it.startedAtMs) * it.speedPxPerMs + it.widthPx <= 0f
        }
    }

    fun clear() {
        comments.clear()
        scheduler.reset()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clear()
    }

    private companion object {
        const val MAX_LANES = 12
        const val MAX_COMMENTS = 48
        const val MAX_TEXT_LENGTH = 120
        const val TRAVEL_TIME_MS = 6000f
    }
}
