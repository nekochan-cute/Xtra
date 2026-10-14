package com.github.andreyasadchy.xtra.ui.player

import com.github.andreyasadchy.xtra.model.VideoQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaftPolicyTest {
    @Test
    fun attemptsOfficialVaftTokenTypesInOrder() {
        assertEquals(
            listOf("web:embed", "web:popout", "android:autoplay"),
            VaftPolicy.backupPlayerTypes.map { "${it.platform}:${it.playerType}" }
        )
    }

    @Test
    fun keepsCurrentPlaybackResolution() {
        val source = VideoQuality(name = "720p", resolution = 720, codecs = "avc1")
        val variants = listOf(
            VideoQuality(name = "1080p", resolution = 1080),
            VideoQuality(name = "480p", resolution = 480),
            VideoQuality(name = "720p", resolution = 720, codecs = "avc1")
        )
        assertEquals("720p", VaftPolicy.closestQuality(source, variants)?.name)
        assertEquals("1080p", VaftPolicy.closestQuality(null, variants)?.name)
    }

    @Test
    fun detectsStitchedAdMarkers() {
        assertTrue(VaftPolicy.containsAdMarkers(
            listOf("#EXT-X-DATERANGE:ID=\"stitched-ad-42\""), emptyList()
        ))
        assertTrue(VaftPolicy.containsAdMarkers(
            listOf("#EXT-X-DATERANGE:CLASS=\"twitch-stitched-ad\""), emptyList()
        ))
        assertTrue(VaftPolicy.containsAdMarkers(
            listOf("#EXT-X-DATERANGE:X-TV-TWITCH-AD-POD-LENGTH=\"1\""), emptyList()
        ))
        assertTrue(VaftPolicy.containsAdMarkers(emptyList(), listOf("Amazon|MIDROLL")))
    }

    @Test
    fun allowsOrdinaryStream() {
        assertFalse(VaftPolicy.containsAdMarkers(
            listOf("#EXT-X-VERSION:6", "#EXT-X-TARGETDURATION:6"), listOf("live")
        ))
    }
}
