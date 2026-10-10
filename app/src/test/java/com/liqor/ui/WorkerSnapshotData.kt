package com.liqor.ui

import com.liqor.domain.PositionsResponse
import com.liqor.domain.Reading
import com.liqor.domain.positionJson
import com.liqor.domain.read
import java.time.Instant
import kotlinx.serialization.decodeFromString

internal object WorkerSnapshotData {
    const val walletAddress = "CYEd…JcF"

    private val response: PositionsResponse by lazy {
        val stream = requireNotNull(
            javaClass.getResourceAsStream("/live-2026-10-09.json"),
        )
        stream.bufferedReader().use {
            positionJson.decodeFromString<PositionsResponse>(it.readText())
        }
    }

    val all: Reading.Live by lazy {
        val now = Instant.parse(response.serverTime).plusSeconds(12)
        requireNotNull(read(response, now) as? Reading.Live)
    }

    val walletPositions: Reading.Live by lazy {
        val addresses = setOf(
            "A6Ke3XhgXogDUTAmxAHWg8czoL9JQg9PYwbBRZwxNRTs",
            "BTeM8V1TLgZwEe7ENCxCQtQmvMoxcseipryLawCKyRPd",
        )
        all.copy(watched = all.watched.filter { it.position.address in addresses })
    }
}
