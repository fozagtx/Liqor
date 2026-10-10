package com.liqor.domain

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

@Serializable
data class PositionsResponse(
    val slot: Long,
    val serverTime: String,
    val codeVersion: String,
    val positions: List<PositionDto>,
)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class PositionDto(
    val protocol: String,
    val address: String,
    val asset: String,
    val side: String,
    val entryPrice: Double,
    val sizeUsd: Double,
    val collateralUsd: Double,
    val closeFeeUsd: Double,
    val borrowFeeUsd: Double,
    val oraclePrice: Double,
    val priceSource: String,
    val liquidationPrice: Double,
    val link: String,
)

val positionJson = Json {
    ignoreUnknownKeys = true
}

enum class Level {
    Safe,
    Caution,
    Danger,
}

data class Watched(
    val position: PositionDto,
    val distance: Double,
    val travelled: Double,
    val level: Level,
)

sealed interface Reading {
    data class Live(
        val watched: List<Watched>,
        val serverTime: Instant,
    ) : Reading

    data object NoPositions : Reading
    data object CouldNotCheck : Reading
}

enum class Freshness {
    Fresh,
    Stale,
    Unknown,
}

fun validate(position: PositionDto): Watched? {
    val pricesAndValues = listOf(
        position.entryPrice,
        position.oraclePrice,
        position.liquidationPrice,
        position.sizeUsd,
        position.collateralUsd,
    )
    if (pricesAndValues.any { !it.isFinite() || it <= 0.0 }) return null

    val isBeta = position.protocol == "jupiter-beta"
    val isLong = position.side == "long"
    if (!isLong && position.side != "short") return null
    if (!isBeta && isLong && position.liquidationPrice >= position.entryPrice) return null
    if (!isBeta && !isLong && position.liquidationPrice <= position.entryPrice) return null

    if (!isBeta) {
        val collateralRatio = position.collateralUsd / position.sizeUsd
        val roughLiquidation = if (isLong) {
            position.entryPrice * (1.0 - collateralRatio)
        } else {
            position.entryPrice * (1.0 + collateralRatio)
        }
        if (!roughLiquidation.isFinite()) return null
        if (abs(roughLiquidation - position.liquidationPrice) / position.liquidationPrice > 0.05) {
            return null
        }
    }

    val distance = if (isLong) {
        (position.oraclePrice - position.liquidationPrice) / position.oraclePrice
    } else {
        (position.liquidationPrice - position.oraclePrice) / position.oraclePrice
    }.coerceAtLeast(0.0)
    val liquidationSpan = if (isLong) {
        position.entryPrice - position.liquidationPrice
    } else {
        position.liquidationPrice - position.entryPrice
    }
    val travelled = if (isBeta && liquidationSpan <= 0.0) {
        1.0
    } else if (isLong) {
        ((position.entryPrice - position.oraclePrice) / liquidationSpan).coerceIn(0.0, 1.0)
    } else {
        ((position.oraclePrice - position.entryPrice) / liquidationSpan).coerceIn(0.0, 1.0)
    }

    val normalizedTravelled = when {
        abs(travelled - 0.5) < 1e-12 -> 0.5
        abs(travelled - 0.8) < 1e-12 -> 0.8
        else -> travelled
    }
    val level = when {
        normalizedTravelled < 0.5 -> Level.Safe
        normalizedTravelled <= 0.8 -> Level.Caution
        else -> Level.Danger
    }
    return Watched(position, distance, travelled, level)
}

fun read(response: PositionsResponse, now: Instant): Reading {
    val serverTime = try {
        Instant.parse(response.serverTime)
    } catch (_: DateTimeParseException) {
        return Reading.CouldNotCheck
    }
    if (Duration.between(serverTime, now) > Duration.ofSeconds(120)) {
        return Reading.CouldNotCheck
    }
    if (response.positions.isEmpty()) return Reading.NoPositions

    val watched = mutableListOf<Watched>()
    for (position in response.positions) {
        watched += validate(position) ?: return Reading.CouldNotCheck
    }
    return Reading.Live(
        watched = watched.sortedWith(
            compareByDescending<Watched> { it.level.ordinal }
                .thenBy { it.distance },
        ),
        serverTime = serverTime,
    )
}

fun freshness(ageSeconds: Long): Freshness = when {
    ageSeconds <= 30 -> Freshness.Fresh
    ageSeconds <= 120 -> Freshness.Stale
    else -> Freshness.Unknown
}
