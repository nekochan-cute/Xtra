package com.github.andreyasadchy.xtra.ui.view

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs

/**
 * Non-interactive, Canvas-based scrolling comments with configurable appearance.
 * The preference listener updates the overlay immediately without restarting playback.
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
    private val scaledDensity = resources.displayMetrics.scaledDensity
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(outlinePaint).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private var style = DanmakuStyle()
    private val scheduler = DanmakuLaneScheduler(16f * density)
    private val comments = ArrayList<Comment>()
    private val preferences by lazy { context.prefs() }
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in STYLE_KEYS) {
            // Preference updates normally occur on the main thread; post also handles other callers.
            post { if (isAttachedToWindow) refreshStyle() }
        }
    }
    private val laneHeightPx: Int
        get() = (textPaint.fontSpacing + (6f + style.outlineWidthDp) * density).toInt().coerceAtLeast(1)

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        configurePaints()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refreshStyle()
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onDetachedFromWindow() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        clear()
        super.onDetachedFromWindow()
    }

    /** Read values saved by SeekBarPreference / ListPreference and rebuild the paint. */
    private fun refreshStyle() {
        val updated = DanmakuStyle.sanitized(
            textSizeSp = preferences.getInt(C.PLAYER_DANMAKU_TEXT_SIZE, 20),
            travelTimeSeconds = preferences.getInt(C.PLAYER_DANMAKU_TRAVEL_TIME, 6),
            outlineWidthDp = preferences.getInt(C.PLAYER_DANMAKU_OUTLINE_WIDTH, 3),
            opacityPercent = preferences.getInt(C.PLAYER_DANMAKU_OPACITY, 100),
            maxLanes = preferences.getInt(C.PLAYER_DANMAKU_LANES, 12),
            textColorName = preferences.getString(C.PLAYER_DANMAKU_TEXT_COLOR, "white"),
            outlineColorName = preferences.getString(C.PLAYER_DANMAKU_OUTLINE_COLOR, "black"),
        )
        if (style != updated) {
            style = updated
            configurePaints()
            // Existing baselines and lane allocations were calculated for the old style.
            clear()
        }
    }

    private fun configurePaints() {
        val alpha = (style.opacityPercent * 255 / 100).coerceIn(0, 255)
        outlinePaint.apply {
            textSize = style.textSizeSp * scaledDensity
            strokeWidth = style.outlineWidthDp * density
            color = when (style.outlineColorName) {
                "white" -> Color.WHITE
                "dark_gray" -> Color.DKGRAY
                else -> Color.BLACK
            }
            this.alpha = alpha
        }
        textPaint.apply {
            textSize = style.textSizeSp * scaledDensity
            color = when (style.textColorName) {
                "yellow" -> Color.YELLOW
                "cyan" -> Color.CYAN
                "green" -> Color.GREEN
                "pink" -> 0xffff80c0.toInt()
                else -> Color.WHITE
            }
            this.alpha = alpha
        }
    }

    fun addComment(message: String) {
        if (!isShown || width <= 0 || height < laneHeightPx) return
        val text = message.replace('\n', ' ').replace('\r', ' ').trim().take(MAX_TEXT_LENGTH)
        if (text.isEmpty()) return

        val now = SystemClock.uptimeMillis()
        val speedPxPerMs = width / (style.travelTimeSeconds * 1000f)
        removeExpired(now)
        val textWidth = textPaint.measureText(text)
        val lane = scheduler.claimLane(
            nowMs = now,
            textWidthPx = textWidth,
            laneCount = (height / laneHeightPx).coerceAtMost(style.maxLanes),
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
            if (style.outlineWidthDp > 0) {
                canvas.drawText(comment.text, x, comment.baselinePx, outlinePaint)
            }
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
        const val MAX_COMMENTS = 48
        const val MAX_TEXT_LENGTH = 120
        val STYLE_KEYS = setOf(
            C.PLAYER_DANMAKU_TEXT_SIZE,
            C.PLAYER_DANMAKU_TRAVEL_TIME,
            C.PLAYER_DANMAKU_OUTLINE_WIDTH,
            C.PLAYER_DANMAKU_OPACITY,
            C.PLAYER_DANMAKU_LANES,
            C.PLAYER_DANMAKU_TEXT_COLOR,
            C.PLAYER_DANMAKU_OUTLINE_COLOR,
        )
    }
}
