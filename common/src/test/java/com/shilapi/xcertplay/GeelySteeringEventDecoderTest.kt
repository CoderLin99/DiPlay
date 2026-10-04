package com.shilapi.xcertplay

import org.junit.Assert.*
import org.junit.Test

class GeelySteeringEventDecoderTest {
    private val events = mutableListOf<GeelySteeringKeyEvent>()
    private val log = mutableListOf<String>()
    private val decoder = GeelySteeringEventDecoder(log::add, events::add)

    @Test fun standardMediaKeysReachCarPlayAndGestureDoesNotRepeatThePress() {
        for (raw in listOf(85, 87, 88, 231)) {
            decoder.accept(1, raw, 0, 10_000)
            decoder.accept(1, raw, 1, 10_050)
            decoder.accept(2, raw, -1, 10_080)
        }
        assertEquals(listOf(200085, 200087, 200088, 200231), events.filter { it.action == 0 }.map { it.keyCode })
        assertEquals(8, events.size)
    }

    @Test fun aliasesAndHeldKeyRepeatsDoNotDoubleSendButAnotherPressStillWorks() {
        decoder.accept(1, 200087, 0, 10_000)
        decoder.accept(1, 87, 0, 10_020)
        decoder.accept(1, 200087, 1, 10_050)
        decoder.accept(1, 87, 0, 10_100)
        decoder.accept(1, 87, 1, 10_150)
        assertEquals(listOf(0, 1, 0, 1), events.map { it.action })
    }

    @Test fun gestureOnlyFirmwareAndUnknownEventsAreDiagnosable() {
        decoder.accept(2, 110005, -1, 100)
        decoder.accept(2, 555555, -1, 200)
        assertEquals(210005, events.single().keyCode)
        assertTrue(log.last().contains("raw=555555"))
        assertTrue(log.last().contains("result=ignored"))
    }
}
