package com.github.andreyasadchy.xtra.ui.view

import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuStyleTest {
    @Test fun defaultsPreserveExistingAppearance() {
        assertEquals(DanmakuStyle(), DanmakuStyle.sanitized())
    }

    @Test fun respectsUserChoices() {
        assertEquals(
            DanmakuStyle(32, 4, 0, 70, 5, "pink", "white"),
            DanmakuStyle.sanitized(32, 4, 0, 70, 5, "pink", "white")
        )
    }

    @Test fun clampsInvalidPreferenceValues() {
        assertEquals(
            DanmakuStyle(12, 15, 6, 20, 1, "white", "black"),
            DanmakuStyle.sanitized(-100, 999, 50, -12, 0, "unknown", "")
        )
    }

    @Test fun clampsValuesAboveMaximum() {
        assertEquals(
            DanmakuStyle(48, 3, 0, 100, 12, "cyan", "dark_gray"),
            DanmakuStyle.sanitized(100, -1, -2, 150, 999, "cyan", "dark_gray")
        )
    }
}
