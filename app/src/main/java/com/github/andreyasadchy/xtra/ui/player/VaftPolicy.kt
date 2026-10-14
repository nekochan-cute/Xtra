package com.github.andreyasadchy.xtra.ui.player

/**
 * Native variant of the fallback strategy used by TwitchAdSolutions VAFT.
 * https://github.com/pixeltris/TwitchAdSolutions/tree/master/vaft (MIT)
 *
 * The Android playback pipeline already has playlist/token exchange and a
 * secondary player. Keep fallback ordering and ad detection independent of it.
 */
internal object VaftPolicy {
    data class Candidate(val platform: String, val playerType: String)

    val backupPlayerTypes = listOf(
        Candidate("web", "embed"),
        Candidate("web", "popout"),
        Candidate("android", "autoplay"),
    )

    fun containsAdMarkers(tags: List<String>, segmentTitles: List<String>): Boolean {
        return tags.any { tag ->
            tag.contains("stitched-ad-", ignoreCase = true) ||
                tag.contains("twitch-stitched-ad", ignoreCase = true) ||
                tag.contains("X-TV-TWITCH-AD-", ignoreCase = true)
        } || segmentTitles.any { title ->
            title.substringBefore('|') in setOf("Amazon", "Adform", "DCM")
        }
    }
}
