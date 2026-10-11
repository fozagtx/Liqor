package com.liqor.ui

import com.liqor.domain.Freshness
import com.liqor.domain.Level
import com.liqor.domain.Reading
import com.liqor.domain.Watched
import com.liqor.domain.samplePosition
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class UiModelsTest {
    @Test
    fun affectedPositionsRotateInAscendingDistanceOrder() {
        val safe = watched("SOL", distance = 0.01, level = Level.Safe)
        val caution = watched("ETH", distance = 0.03, level = Level.Caution)
        val danger = watched("BTC", distance = 0.05, level = Level.Danger)

        val ui = bubbleUi(
            Reading.Live(listOf(danger, safe, caution), Instant.EPOCH),
            Freshness.Fresh,
        )

        assertEquals("SOL", ui.asset)
        assertEquals(BubbleState.Safe, ui.state)
        assertEquals(listOf("ETH", "BTC"), ui.rotation.map { it.asset })
        assertEquals(
            listOf(BubbleState.Caution, BubbleState.Danger),
            ui.rotation.map { it.state },
        )
    }

    @Test
    fun singleAffectedPositionDoesNotCreateRotation() {
        val caution = watched("ETH", distance = 0.03, level = Level.Caution)
        val safe = watched("SOL", distance = 0.05, level = Level.Safe)

        val ui = bubbleUi(
            Reading.Live(listOf(caution, safe), Instant.EPOCH),
            Freshness.Fresh,
        )

        assertEquals(emptyList<BubbleUi>(), ui.rotation)
    }

    private fun watched(asset: String, distance: Double, level: Level) = Watched(
        position = samplePosition.position.copy(asset = asset, address = asset),
        distance = distance,
        travelled = when (level) {
            Level.Safe -> 0.25
            Level.Caution -> 0.6
            Level.Danger -> 0.9
        },
        level = level,
    )
}
