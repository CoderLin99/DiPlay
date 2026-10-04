package com.shilapi.xcertplay

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Display
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SecondaryDisplayProbeTest {
    @Test fun probeResultsArePersistedAndBounded() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("diplay_display_probes", Context.MODE_PRIVATE).edit().clear().commit()
        repeat(30) { SecondaryDisplayProbeHistory.record(context, "probe id=$it result=requested") }
        val report = SecondaryDisplayProbeHistory.report(ContextWrapper(context))
        assertEquals(24, report.lines().size)
        assertFalse(report.contains("probe id=0 "))
        assertTrue(report.contains("probe id=29 result=requested"))
    }

    @Test fun refusedPrimaryDisplayAttemptRemainsInTheReport() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            assertFalse(SecondaryDisplayProbe.show(activity.get(), Display.DEFAULT_DISPLAY))
            val report = SecondaryDisplayProbe.diagnostics(activity.get().applicationContext)
            assertTrue(report.contains("probe id=0 result=requested"))
            assertTrue(report.contains("probe id=0 result=unavailable"))
        } finally {
            SecondaryDisplayProbe.stop()
            activity.pause().stop().destroy()
        }
    }
}
