package com.liqor.ui

import com.liqor.domain.Freshness
import com.liqor.domain.Level
import com.liqor.domain.Reading
import com.liqor.domain.Watched
import com.liqor.domain.distanceText

enum class BubbleState {
    Safe,
    Caution,
    Danger,
    Stale,
    Unknown,
    Sample,
}

data class BubbleUi(
    val value: String,
    val state: BubbleState,
    val cushion: Float,
    val animateDanger: Boolean = true,
)

fun bubbleUi(
    reading: Reading,
    freshness: Freshness,
    sample: Boolean = false,
): BubbleUi {
    val live = reading as? Reading.Live ?: return BubbleUi("–", BubbleState.Unknown, 0f)
    val closest = live.watched.minByOrNull { it.distance }
        ?: return BubbleUi("–", BubbleState.Unknown, 0f)
    val level = live.watched.maxByOrNull { it.level.ordinal }?.level ?: closest.level
    val value = distanceText(closest.distance)
    if (freshness == Freshness.Unknown) {
        return BubbleUi("–", BubbleState.Unknown, 0f)
    }
    if (freshness == Freshness.Stale) {
        return BubbleUi(value, BubbleState.Stale, 1f - closest.travelled.toFloat())
    }
    val state = if (sample) {
        BubbleState.Sample
    } else {
        level.toBubbleState()
    }
    return BubbleUi(value, state, 1f - closest.travelled.toFloat())
}

fun Level.toBubbleState(): BubbleState = when (this) {
    Level.Safe -> BubbleState.Safe
    Level.Caution -> BubbleState.Caution
    Level.Danger -> BubbleState.Danger
}

fun closestPosition(reading: Reading.Live): Watched? =
    reading.watched.minByOrNull { it.distance }
