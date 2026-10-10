package com.liqor.domain

import kotlin.math.PI
import kotlin.math.cos

data class SamplePosition(val position: PositionDto) {
    val isSample: Boolean = true

    fun at(elapsedMs: Long): Watched {
        val current = position.copy(oraclePrice = samplePrice(elapsedMs))
        return requireNotNull(validate(current))
    }
}

val samplePosition = SamplePosition(
    position = PositionDto(
        protocol = "jupiter-perps",
        address = "SAMPLE",
        asset = "SOL",
        side = "long",
        entryPrice = 86.00,
        sizeUsd = 300.00,
        collateralUsd = 96.28,
        closeFeeUsd = 0.0,
        borrowFeeUsd = 0.0,
        oraclePrice = 73.00,
        priceSource = "sample",
        liquidationPrice = 58.40,
        link = "https://jup.ag/perps",
    ),
)

fun samplePrice(elapsedMs: Long): Double {
    val phaseMs = Math.floorMod(elapsedMs, 40_000L)
    val phase = 2.0 * PI * phaseMs / 40_000.0
    return 68.0 + 5.0 * cos(phase)
}
