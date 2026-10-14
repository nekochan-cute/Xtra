package com.github.andreyasadchy.xtra.ui.view

/**
 * Reserves horizontal lanes for equally fast comments. A lane is available only
 * after the previous comment's trailing edge has entered the screen by [gapPx].
 *
 * The clock must be monotonic (e.g. SystemClock.uptimeMillis).
 */
internal class DanmakuLaneScheduler(private val gapPx: Float) {
    private data class LastComment(val startedAtMs: Long, val widthPx: Float)

    private var lastComments = emptyArray<LastComment?>()
    private var nextLane = 0

    fun claimLane(nowMs: Long, textWidthPx: Float, laneCount: Int, speedPxPerMs: Float): Int? {
        if (laneCount <= 0 || textWidthPx <= 0f || speedPxPerMs <= 0f) return null
        if (lastComments.size != laneCount) {
            lastComments = arrayOfNulls(laneCount)
            nextLane = 0
        }
        repeat(laneCount) { offset ->
            val lane = (nextLane + offset) % laneCount
            val previous = lastComments[lane]
            if (previous == null ||
                (nowMs - previous.startedAtMs).coerceAtLeast(0L) * speedPxPerMs >= previous.widthPx + gapPx
            ) {
                lastComments[lane] = LastComment(nowMs, textWidthPx)
                nextLane = (lane + 1) % laneCount
                return lane
            }
        }
        return null
    }

    fun reset() {
        lastComments = emptyArray()
        nextLane = 0
    }
}
