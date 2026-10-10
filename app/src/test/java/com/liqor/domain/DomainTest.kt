package com.liqor.domain

import java.time.Instant
import kotlin.math.abs
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    private val fixture by lazy(::loadFixture)

    @Test
    fun liveWorkerFixtureValidatesAndReadsAsLive() {
        val response = fixture
        val serverTime = Instant.parse(response.serverTime)
        val watched = response.positions.mapNotNull(::validate)
        val reading = read(response, serverTime.plusSeconds(5))

        assertEquals(5, watched.size)
        assertTrue(watched.all { it.distance.isFinite() && it.distance >= 0.0 })
        assertTrue(watched.all { it.travelled in 0.0..1.0 })
        assertTrue(reading is Reading.Live)
        assertEquals(watched.sortedWith(
            compareByDescending<Watched> { it.level.ordinal }.thenBy { it.distance },
        ), (reading as Reading.Live).watched)
    }

    @Test
    fun errorBodyHasItsOwnDto() {
        val error = positionJson.decodeFromString<ErrorResponse>("""{"error":"unavailable"}""")

        assertEquals("unavailable", error.error)
    }

    @Test
    fun rejectsWrongSideAndSixPercentRoughEstimateMismatch() {
        val short = fixture.positions.first { it.side == "short" }
        val rough = short.entryPrice * (1.0 + short.collateralUsd / short.sizeUsd)
        val wrongSide = short.copy(liquidationPrice = short.entryPrice - 1.0)
        val mismatched = short.copy(liquidationPrice = rough / 0.94)

        assertNull(validate(wrongSide))
        assertNull(validate(mismatched))
    }

    @Test
    fun betaPositionDoesNotRequireRoughCollateralEstimateToMatch() {
        val long = fixture.positions.first { it.side == "long" }
        val position = long.copy(
            protocol = "jupiter-beta",
            collateralUsd = long.sizeUsd * 0.01,
        )
        val roughLiquidation = position.entryPrice * (1.0 - position.collateralUsd / position.sizeUsd)

        assertTrue(abs(roughLiquidation - position.liquidationPrice) / position.liquidationPrice > 0.05)
        assertNotNull(validate(position))
    }

    @Test
    fun betaWrongSideLiquidationIsDanger() {
        val long = fixture.positions.first { it.side == "long" }
        val position = long.copy(
            protocol = "jupiter-beta",
            liquidationPrice = long.entryPrice + 1.0,
        )

        val watched = requireNotNull(validate(position))
        assertEquals(1.0, watched.travelled, 0.0)
        assertEquals(Level.Danger, watched.level)
    }

    @Test
    fun rejectsNonFiniteAndNonPositiveInputs() {
        val position = fixture.positions.first()
        val invalid = listOf(
            position.copy(entryPrice = Double.NaN),
            position.copy(oraclePrice = 0.0),
            position.copy(liquidationPrice = Double.NaN),
            position.copy(sizeUsd = 0.0),
            position.copy(collateralUsd = 0.0),
        )

        assertTrue(invalid.all { validate(it) == null })
    }

    @Test
    fun oneInvalidPositionInvalidatesWholeResponse() {
        val now = Instant.parse("2026-10-10T00:00:00Z")
        val response = response(
            now,
            listOf(fixture.positions[0], fixture.positions[1].copy(side = "both")),
        )

        assertEquals(Reading.CouldNotCheck, read(response, now))
    }

    @Test
    fun rejectsUnparseableAndOlderThanTwoMinuteServerTimes() {
        val now = Instant.parse("2026-10-10T00:00:00Z")
        val valid = response(now, listOf(fixture.positions.first()))

        assertEquals(Reading.CouldNotCheck, read(valid.copy(serverTime = "not-an-instant"), now))
        assertEquals(
            Reading.CouldNotCheck,
            read(valid.copy(serverTime = now.minusSeconds(121).toString()), now),
        )
    }

    @Test
    fun emptyResponseIsNoPositions() {
        val now = Instant.parse("2026-10-10T00:00:00Z")

        assertEquals(Reading.NoPositions, read(response(now, emptyList()), now))
    }

    @Test
    fun freshnessUsesThirtyAndOneHundredTwentySecondBoundaries() {
        assertEquals(Freshness.Fresh, freshness(30))
        assertEquals(Freshness.Stale, freshness(31))
        assertEquals(Freshness.Stale, freshness(120))
        assertEquals(Freshness.Unknown, freshness(121))
    }

    @Test
    fun distanceTextFloorsPercentWithoutRoundingTowardSafety() {
        assertEquals("4.1", distanceText(0.0414))
        assertEquals("18", distanceText(0.189))
        assertEquals("9.9", distanceText(0.0999))
        assertEquals("0", distanceText(-0.01))
    }

    @Test
    fun usdGroupsThousandsAndKeepsTwoDecimalPlaces() {
        assertEquals("$86.00", usd(86.0))
        assertEquals("$83,538.54", usd(83538.54))
    }

    @Test
    fun sentenceUsesSideSpecificPriceDirection() {
        val long = requireNotNull(validate(fixture.positions.first { it.side == "long" }))
        val short = requireNotNull(validate(fixture.positions.first { it.side == "short" }))

        assertEquals(
            "${long.position.asset} can drop ${distanceText(long.distance)}%",
            sentence(long),
        )
        assertEquals(
            "${short.position.asset} can rise ${distanceText(short.distance)}%",
            sentence(short),
        )
    }

    @Test
    fun travelledBoundariesMapToExpectedSeverity() {
        assertEquals(Level.Safe, watchedAt(0.49).level)
        assertEquals(Level.Caution, watchedAt(0.5).level)
        assertEquals(Level.Caution, watchedAt(0.8).level)
        assertEquals(Level.Danger, watchedAt(0.81).level)
    }

    @Test
    fun sampleStartsAtTwentyPercentAndCyclesThroughCautionAndDanger() {
        assertTrue(samplePosition.isSample)
        assertEquals("20", distanceText(samplePosition.at(0).distance))
        assertTrue(samplePosition.at(8_000).level.ordinal >= Level.Caution.ordinal)
        assertEquals(Level.Danger, samplePosition.at(20_000).level)
        assertEquals(73.0, samplePrice(40_000), 0.0001)
    }

    private fun watchedAt(travelled: Double): Watched {
        val position = fixture.positions.first { it.side == "long" }
        val oraclePrice = position.entryPrice -
            travelled * (position.entryPrice - position.liquidationPrice)
        return validate(position.copy(oraclePrice = oraclePrice)).also {
            assertNotNull(it)
        }!!
    }

    private fun response(now: Instant, positions: List<PositionDto>) =
        fixture.copy(serverTime = now.toString(), positions = positions)

    private fun loadFixture(): PositionsResponse {
        val stream = requireNotNull(javaClass.getResourceAsStream("/live-2026-10-09.json"))
        return stream.bufferedReader().use {
            positionJson.decodeFromString<PositionsResponse>(it.readText())
        }
    }
}
