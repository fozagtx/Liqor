package com.liqor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertGateTest {
    @Test
    fun firesAfterTwentySecondsButNotNineteen() {
        var now = 0L
        val gate = AlertGate({ now })
        gate.observe(Level.Safe)
        gate.observe(Level.Caution)
        now = 19_000L
        assertNull(gate.observe(Level.Caution))
        now = 20_000L
        assertTrue(requireNotNull(gate.observe(Level.Caution)).alert)
    }

    @Test
    fun flickerDoesNotFireUntilTheNewLevelHolds() {
        var now = 0L
        val gate = AlertGate({ now })
        gate.observe(Level.Safe)
        gate.observe(Level.Caution)
        now = 10_000L
        assertNull(gate.observe(Level.Safe))
        now = 20_000L
        assertNull(gate.observe(Level.Caution))
        now = 39_999L
        assertNull(gate.observe(Level.Caution))
        now = 40_000L
        assertTrue(requireNotNull(gate.observe(Level.Caution)).alert)
    }

    @Test
    fun recoveryIsConfirmedSilently() {
        var now = 0L
        val gate = AlertGate({ now })
        gate.observe(Level.Danger)
        gate.observe(Level.Caution)
        now = 20_000L
        val recovery = requireNotNull(gate.observe(Level.Caution))
        assertEquals(Level.Caution, recovery.current)
        assertFalse(recovery.alert)
    }

    @Test
    fun worseningAfterConfirmedRecoveryFiresAgain() {
        var now = 0L
        val gate = AlertGate({ now })
        gate.observe(Level.Safe)
        gate.observe(Level.Caution)
        now = 20_000L
        assertTrue(requireNotNull(gate.observe(Level.Caution)).alert)
        gate.observe(Level.Safe)
        now = 40_000L
        assertFalse(requireNotNull(gate.observe(Level.Safe)).alert)
        gate.observe(Level.Caution)
        now = 60_000L
        assertTrue(requireNotNull(gate.observe(Level.Caution)).alert)
    }
}
