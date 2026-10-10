package com.liqor.data

import com.liqor.Config
import com.liqor.chain.ChainHttp
import com.liqor.chain.GumPerps
import com.liqor.chain.JupiterPerps
import com.liqor.chain.OkHttpChainHttp
import com.liqor.domain.PositionsResponse
import com.liqor.domain.Reading
import com.liqor.domain.read
import com.liqor.wallet.Base58
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class DirectChainReader(
    http: ChainHttp = OkHttpChainHttp(),
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val jupiter = JupiterPerps(http, nowMs)
    private val gum = GumPerps(http) { nowMs() / 1_000.0 }
    private val walletCache = mutableMapOf<String, CachedReading>()

    suspend fun positions(walletAddress: String): Reading {
        val requestTime = nowMs()
        synchronized(walletCache) {
            walletCache[walletAddress]
                ?.takeIf { requestTime - it.cachedAtMs < WALLET_CACHE_MS }
                ?.let { return it.reading }
        }
        return try {
            require(Base58.decode(walletAddress).size == 32) { "invalid wallet" }
            val (jupiterResult, betaPositions) = coroutineScope {
                val jupiterRead = async { jupiter.positionsFor(walletAddress) }
                val betaRead = async { gum.positionsFor(walletAddress) }
                jupiterRead.await() to betaRead.await()
            }
            val serverTime = Instant.ofEpochMilli(nowMs())
            val nowSeconds = serverTime.toEpochMilli() / 1_000.0
            if (jupiterResult.positions.any {
                    !it.priceTime.isFinite() || nowSeconds - it.priceTime > MAX_ORACLE_AGE_SECONDS
                }
            ) {
                return Reading.CouldNotCheck
            }
            val response = PositionsResponse(
                slot = jupiterResult.slot,
                serverTime = serverTime.toString(),
                codeVersion = Config.CODE_VERSION,
                positions = jupiterResult.positions.map { it.position } +
                    betaPositions.map { it.position },
            )
            val reading = read(response, serverTime)
            if (reading != Reading.CouldNotCheck) {
                synchronized(walletCache) {
                    walletCache.entries.removeAll { requestTime - it.value.cachedAtMs >= WALLET_CACHE_MS }
                    walletCache[walletAddress] = CachedReading(requestTime, reading)
                }
            }
            reading
        } catch (_: Exception) {
            Reading.CouldNotCheck
        }
    }

    private data class CachedReading(
        val cachedAtMs: Long,
        val reading: Reading,
    )

    private companion object {
        const val WALLET_CACHE_MS = 5_000L
        const val MAX_ORACLE_AGE_SECONDS = 120.0
    }
}
