package com.liqor.ui

import com.liqor.chain.JLP_WALLET
import com.liqor.domain.PositionDto
import com.liqor.domain.PositionsResponse
import com.liqor.domain.Reading
import com.liqor.domain.Watched
import com.liqor.domain.positionJson
import com.liqor.domain.read
import com.liqor.domain.validate
import com.liqor.wallet.shortAddress
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
private data class BetaWorkerCapture(
    val capturedAt: String,
    val positions: List<PositionDto>,
)

internal object WorkerSnapshotData {
    val walletAddress = shortAddress(JLP_WALLET)

    private val response: PositionsResponse by lazy {
        val stream = requireNotNull(
            javaClass.getResourceAsStream("/live-2026-10-09.json"),
        )
        stream.bufferedReader().use {
            positionJson.decodeFromString<PositionsResponse>(it.readText())
        }
    }

    private val betaCapture: BetaWorkerCapture by lazy {
        val stream = requireNotNull(
            javaClass.getResourceAsStream("/live-beta-worker.json"),
        )
        stream.bufferedReader().use {
            positionJson.decodeFromString<BetaWorkerCapture>(it.readText())
        }
    }

    val all: Reading.Live by lazy {
        val now = Instant.parse(response.serverTime).plusSeconds(12)
        requireNotNull(read(response, now) as? Reading.Live)
    }

    val beta: Reading.Live by lazy {
        val watched = betaCapture.positions.map { requireNotNull(validate(it)) }
        Reading.Live(
            watched = watched.sortedWith(
                compareByDescending<Watched> { it.level.ordinal }.thenBy { it.distance },
            ),
            serverTime = Instant.parse(betaCapture.capturedAt),
        )
    }

    val walletPositions: Reading.Live by lazy {
        val addresses = setOf(
            "A6Ke3XhgXogDUTAmxAHWg8czoL9JQg9PYwbBRZwxNRTs",
            "BTeM8V1TLgZwEe7ENCxCQtQmvMoxcseipryLawCKyRPd",
        )
        all.copy(watched = all.watched.filter { it.position.address in addresses })
    }
}
