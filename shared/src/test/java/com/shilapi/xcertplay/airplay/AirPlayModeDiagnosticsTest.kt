package com.shilapi.xcertplay.airplay

import org.junit.Assert.*
import org.junit.Test

class AirPlayModeDiagnosticsTest {
    @Test fun retainsOwnershipAndAppStateWithoutExportingPhoneText() {
        val line = AirPlayModeDiagnostics.summary(mapOf(
            "resources" to listOf(mapOf("resourceID" to 2L, "entity" to 1, "owner" to "private-name", "extra" to "secret")),
            "appStates" to listOf(mapOf("appStateID" to 1, "speechMode" to 2, "state" to true)),
        ))
        assertTrue(line.contains("resourceID=2,entity=1"))
        assertTrue(line.contains("state=true"))
        assertFalse(line.contains("private-name"))
        assertFalse(line.contains("secret"))
    }

    @Test fun malformedAndOversizedPhoneDataIsBounded() {
        val line = AirPlayModeDiagnostics.summary(mapOf("resources" to List(1_000) { mapOf("resourceID" to 2) }, "appStates" to "unexpected"))
        assertEquals(8, Regex("resourceID=").findAll(line).count())
        assertTrue(line.contains("appStates=[unavailable]"))
    }
}
