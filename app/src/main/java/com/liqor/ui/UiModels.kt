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
    val asset: String? = null,
    val side: String? = null,
    val rotation: List<BubbleUi> = emptyList(),
)

fun bubbleUi(
    reading: Reading,
    freshness: Freshness,
    sample: Boolean = false,
): BubbleUi {
    val live = reading as? Reading.Live ?: return BubbleUi("–", BubbleState.Unknown, 0f)
    val closest = live.watched.minByOrNull { it.distance }
        ?: return BubbleUi("–", BubbleState.Unknown, 0f)
    if (freshness == Freshness.Unknown) {
        return BubbleUi("–", BubbleState.Unknown, 0f)
    }
    val closestUi = watchedBubbleUi(
        closest,
        state = when {
            freshness == Freshness.Stale -> BubbleState.Stale
            sample -> BubbleState.Sample
            else -> closest.level.toBubbleState()
        },
    )
    if (freshness != Freshness.Fresh || sample) return closestUi

    val affected = live.watched
        .filter { it.level == Level.Caution || it.level == Level.Danger }
        .sortedBy { it.distance }
        .map { watchedBubbleUi(it) }
    return closestUi.copy(rotation = affected.takeIf { it.size > 1 } ?: emptyList())
}

private fun watchedBubbleUi(
    watched: Watched,
    state: BubbleState = watched.level.toBubbleState(),
) = BubbleUi(
    value = distanceText(watched.distance),
    state = state,
    cushion = 1f - watched.travelled.toFloat(),
    asset = watched.position.asset,
    side = watched.position.side,
)

fun Level.toBubbleState(): BubbleState = when (this) {
    Level.Safe -> BubbleState.Safe
    Level.Caution -> BubbleState.Caution
    Level.Danger -> BubbleState.Danger
}

fun closestPosition(reading: Reading.Live): Watched? =
    reading.watched.minByOrNull { it.distance }
