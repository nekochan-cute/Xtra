package com.github.andreyasadchy.xtra.ui.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DanmakuLaneSchedulerTest {
    @Test
    fun preventsOverlappingCommentsInOneLane() {
        val lanes = DanmakuLaneScheduler(gapPx = 20f)
        assertEquals(0, lanes.claimLane(1000L, 100f, 1, 1f))
        assertNull(lanes.claimLane(1119L, 100f, 1, 1f))
        assertEquals(0, lanes.claimLane(1120L, 80f, 1, 1f))
        assertNull(lanes.claimLane(1219L, 50f, 1, 1f))
        assertEquals(0, lanes.claimLane(1220L, 50f, 1, 1f))
    }

    @Test
    fun usesOtherLanesWhenOneIsOccupied() {
        val lanes = DanmakuLaneScheduler(gapPx = 10f)
        assertEquals(0, lanes.claimLane(0L, 100f, 2, 1f))
        assertEquals(1, lanes.claimLane(0L, 100f, 2, 1f))
        assertNull(lanes.claimLane(50L, 100f, 2, 1f))
        assertEquals(0, lanes.claimLane(110L, 100f, 2, 1f))
    }

    @Test
    fun resetAndResizeAllowNewComments() {
        val lanes = DanmakuLaneScheduler(gapPx = 10f)
        assertEquals(0, lanes.claimLane(0L, 100f, 1, 1f))
        assertEquals(0, lanes.claimLane(1L, 100f, 2, 1f))
        lanes.reset()
        assertEquals(0, lanes.claimLane(2L, 100f, 1, 1f))
    }

    @Test
    fun rejectsInvalidMeasurements() {
        val lanes = DanmakuLaneScheduler(gapPx = 10f)
        assertNull(lanes.claimLane(0L, 100f, 0, 1f))
        assertNull(lanes.claimLane(0L, 0f, 1, 1f))
        assertNull(lanes.claimLane(0L, 100f, 1, 0f))
    }
}
