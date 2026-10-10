package com.liqor.domain

class AlertGate(
    private val clock: () -> Long,
    private val holdMillis: Long = 20_000L,
) {
    data class Transition(
        val previous: Level,
        val current: Level,
        val alert: Boolean,
    )

    private var baseline: Level? = null
    private var candidate: Level? = null
    private var candidateSince = 0L

    fun observe(level: Level): Transition? {
        val currentBaseline = baseline
        if (currentBaseline == null) {
            baseline = level
            return null
        }
        if (level == currentBaseline) {
            candidate = null
            return null
        }
        if (level != candidate) {
            candidate = level
            candidateSince = clock()
            return null
        }
        if (clock() - candidateSince < holdMillis) return null

        baseline = level
        candidate = null
        return Transition(
            previous = currentBaseline,
            current = level,
            alert = level.ordinal > currentBaseline.ordinal,
        )
    }
}
