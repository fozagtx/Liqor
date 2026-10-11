package com.liqor.chain

import com.liqor.Config
import java.util.Base64
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal const val JLP_WALLET = "CYEd2AVkTCfDP7G5M8NVUQYvNi6H13icLpaz2KoUkJcF"

class ChainParityTest {
    @Test
    fun capturedJupiterAndGumResponsesMatchTheirLiveApis() = runBlocking {
        val http = CapturedChainHttp()
        val oracleTime = jupiterOracleTime()
        val jupiter = JupiterPerps(http) { oracleTime * 1_000L }
            .positionsFor(JLP_WALLET)
        val jupiterReference = json("jupiter-reference.json").jsonObject
        val referencePositions = jupiterReference.getValue("dataList").jsonArray
            .associateBy { it.jsonObject.getValue("positionPubkey").jsonPrimitive.content }
        assertEquals(jupiterReference.getValue("count").jsonPrimitive.content.toInt(), jupiter.positions.size)
        jupiter.positions.forEach { result ->
            val expected = referencePositions.getValue(result.position.address)
                .jsonObject.getValue("liquidationPrice").jsonPrimitive.content.toDouble()
            assertWithinHalfPercent(result.position.liquidationPrice, expected)
        }

        val markets = json("gum-markets.json").jsonArray
        val hypeTime = markets.single {
            it.jsonObject.getValue("symbol").jsonPrimitive.content == "HYPE"
        }.jsonObject.getValue("timestamp").jsonPrimitive.content.toDouble()
        val gum = GumPerps(http) { hypeTime }
        val frPositions = gum.positionsFor(FR_WALLET)
        val edPositions = gum.positionsFor(ED_WALLET)
        assertTrue(frPositions.isEmpty())
        assertEquals(1, edPositions.size)
        edPositions.forEach { result ->
            val account = result.position.address.substringBefore(':')
            val reference = json("gum-positions-$account.json").jsonArray
                .map { it.jsonObject }
                .single {
                    it.getValue("symbol").jsonPrimitive.content == result.position.asset &&
                        it.getValue("side").jsonPrimitive.content ==
                        if (result.position.side == "long") "Bid" else "Ask"
                }
            val expected = reference.getValue("liquidationPrice").jsonPrimitive.content.toDouble()
            assertWithinHalfPercent(result.position.liquidationPrice, expected)
        }
    }

    private fun jupiterOracleTime(): Long {
        val custodyCount = json("rpc-02.json")
            .jsonObject.getValue("result").jsonObject.getValue("value").jsonArray.size
        val accounts = json("rpc-03.json")
            .jsonObject.getValue("result").jsonObject.getValue("value").jsonArray
        return accounts.drop(custodyCount).take(custodyCount).mapNotNull { element ->
            val account = element as? JsonObject ?: return@mapNotNull null
            val encoded = account.getValue("data").jsonArray.first().jsonPrimitive.content
            decodeAgPrice(Base64.getDecoder().decode(encoded)).timestamp
        }.max()
    }

    private fun assertWithinHalfPercent(actual: Double, expected: Double) {
        val relativeError = abs(actual - expected) / expected
        assertTrue("$actual vs $expected differs by ${relativeError * 100}%", relativeError <= 0.005)
    }

    private fun json(name: String): JsonElement =
        Json.parseToJsonElement(resource(name).decodeToString())

    private fun resource(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/chain-captures/$name")).use { it.readBytes() }

    private inner class CapturedChainHttp : ChainHttp {
        private var rpcRequest = 0

        override suspend fun get(url: String, headers: Map<String, String>): HttpBytes {
            val path = url.removePrefix(Config.GUM_API_URL)
            val name = when {
                path == "/markets" -> "gum-markets.json"
                path == "/tradingAccount" -> {
                    val wallet = headers.getValue("X-PUBKEY")
                    "gum-account-$wallet.json"
                }
                path.endsWith("/positions") -> {
                    val account = path.substringAfter("/tradingAccount/").substringBefore("/positions")
                    "gum-positions-$account.json"
                }
                else -> error("unhandled captured GUM path $path")
            }
            return HttpBytes(200, resource(name))
        }

        override suspend fun post(
            url: String,
            body: String,
            headers: Map<String, String>,
        ): HttpBytes {
            rpcRequest += 1
            return HttpBytes(200, resource("rpc-${rpcRequest.toString().padStart(2, '0')}.json"))
        }
    }

    private companion object {
        const val FR_WALLET = "FRfHg2CdTChB3KaaW7LELXSft17FCMpRyMpmTzeuPFYk"
        const val ED_WALLET = "EDik6ZQi4P2GEmvw1RxNSWbiemdPSdb83r1CxromtHWQ"
    }
}
