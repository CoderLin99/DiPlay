package com.shilapi.xcertplay

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class GeelyDisplaySelectionTest {
    private val displays = listOf(GeelyHudDisplay(3, "Internal", 1920, 532), GeelyHudDisplay(4, "Internal", 1920, 532))

    @Test fun duplicateDisplayNamesDoNotSelectTheWrongLayer() {
        assertEquals(1, GeelyHudProjection.selectedIndex(displays, 4, "Internal"))
        assertEquals(-1, GeelyHudProjection.selectedIndex(displays, 8, "Internal"))
        assertEquals(0, GeelyHudProjection.selectedIndex(displays.take(1), 8, "Internal"))
    }
}
