package com.liqor.domain

import java.util.Locale
import kotlin.math.floor

fun distanceText(distance: Double): String {
    if (!distance.isFinite()) return "–"
    val percent = distance.coerceAtLeast(0.0) * 100.0
    if (percent == 0.0) return "0"
    return if (percent >= 10.0) {
        floor(percent).toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", floor(percent * 10.0) / 10.0)
    }
}

fun sentence(watched: Watched): String {
    val action = if (watched.position.side == "long") "drop" else "rise"
    return "${watched.position.asset} can $action ${distanceText(watched.distance)}%"
}

fun usd(value: Double): String = String.format(Locale.US, "$%,.2f", value)
