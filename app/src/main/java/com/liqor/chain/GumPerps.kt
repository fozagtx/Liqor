package com.liqor.chain

import com.liqor.Config
import com.liqor.domain.PositionDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal class GumPerps(
    private val http: ChainHttp,
    private val nowSeconds: () -> Double = { System.currentTimeMillis() / 1_000.0 },
) {
    suspend fun positionsFor(wallet: String): List<ChainPosition> {
        val (marketsElement, accountElement) = coroutineScope {
            val markets = async { getJson("/markets") }
            val accounts = async {
                getJson(
                    "/tradingAccount",
                    mapOf("X-PUBKEY" to wallet),
                )
            }
            markets.await() to accounts.await()
        }
        val markets = marketsElement as? JsonArray ?: error("GUM markets response is not an array")
        val marketsBySymbol = markets.associateBy {
            val market = it as? JsonObject ?: error("GUM market is invalid")
            market["symbol"].requiredString("GUM market symbol")
        }
        val accountResponse = accountElement as? JsonObject
            ?: error("GUM trading accounts response is not an object")
        val exists = accountResponse["exists"].requiredBoolean("GUM exists flag")
        if (!exists) return emptyList()
        val accounts = accountResponse["accounts"] as? JsonArray
            ?: error("GUM accounts response is not an array")
        val positions = coroutineScope {
            accounts.map { accountElement ->
                async {
                    val account = readAccount(accountElement)
                    if (account.isolated && account.market == null) {
                        error("GUM ${account.pubkey} market is missing")
                    }
                    val path = "/tradingAccount/${account.pubkey}/positions"
                    val response = getJson(path) as? JsonArray
                        ?: error("GUM $path response is not an array")
                    response.mapNotNull { positionElement ->
                        val position = positionElement as? JsonObject
                            ?: error("GUM ${account.pubkey} position is invalid")
                        val isolated = position["isolated"].requiredBoolean(
                            "GUM ${account.pubkey} position isolation",
                        )
                        val relevant = if (account.isolated) {
                            isolated && position["symbol"] == JsonPrimitive(account.market ?: "")
                        } else {
                            !isolated
                        }
                        if (!relevant) {
                            null
                        } else {
                            val symbol = position["symbol"].requiredString(
                                "GUM ${account.pubkey} position symbol",
                            )
                            val market = marketsBySymbol[symbol]
                                ?: error("GUM unknown market $symbol")
                            validateFreshMarket(market, symbol)
                            toOutput(account, position, market, symbol)
                        }
                    }
                }
            }.awaitAll().flatten()
        }
        return positions
    }

    private suspend fun getJson(path: String, headers: Map<String, String> = emptyMap()): JsonElement {
        val response = http.get("${Config.GUM_API_URL}$path", headers)
        val body = response.body.toString(Charsets.UTF_8)
        check(response.status in 200..299) {
            "GUM $path HTTP ${response.status}: ${body.take(200)}"
        }
        val value = try {
            Json.parseToJsonElement(body)
        } catch (exception: Exception) {
            throw IllegalStateException("GUM $path returned invalid JSON", exception)
        }
        if (value is JsonPrimitive && value.isString) {
            error("GUM $path: ${value.content}")
        }
        if (value == JsonNull) error("GUM $path returned an invalid body")
        if (value is JsonObject) {
            val errorBody = value.containsKey("error") ||
                value["code"]?.contentEquals("SERVICE_DEGRADED") == true ||
                value["status"]?.contentEquals("error") == true ||
                value["status"]?.contentEquals("SERVICE_DEGRADED") == true
            if (errorBody) {
                val message = value["error"] ?: value["message"] ?: value["code"] ?: value["status"]
                error("GUM $path: ${message ?: "unknown error"}")
            }
        }
        if (value !is JsonArray && value !is JsonObject) {
            error("GUM $path returned an invalid body")
        }
        return value
    }

    private fun readAccount(element: JsonElement): GumAccount {
        val account = element as? JsonObject ?: error("GUM trading account is invalid")
        val pubkey = account["pubkey"].requiredString("GUM trading account pubkey")
        if (pubkey.isEmpty()) error("GUM trading account is invalid")
        val isolated = account["isolated"].requiredBoolean("GUM trading account isolation")
        val market = if (isolated) {
            account["market"].requiredString("GUM $pubkey market")
        } else {
            null
        }
        return GumAccount(
            pubkey = pubkey,
            isolated = isolated,
            market = market,
            data = account["data"] as? JsonObject,
        )
    }

    private fun validateFreshMarket(market: JsonElement, symbol: String) {
        val marketObject = market as? JsonObject ?: error("GUM $symbol market is invalid")
        val timestamp = marketObject["timestamp"].requiredNumber("GUM $symbol market timestamp")
        if (nowSeconds() - timestamp > 60.0) error("GUM $symbol market is stale")
    }

    private fun toOutput(
        account: GumAccount,
        position: JsonObject,
        market: JsonElement,
        symbol: String,
    ): ChainPosition? {
        val size = position["size"].requiredNumber("GUM $symbol size")
        if (size < 0.0) error("GUM $symbol size is negative")
        val liquidationElement = position["liquidationPrice"]
        if (size == 0.0 || (position.containsKey("liquidationPrice") && liquidationElement == JsonNull)) {
            return null
        }
        val sideValue = position["side"].requiredString("GUM $symbol side")
        if (sideValue != "Bid" && sideValue != "Ask") error("GUM $symbol side is invalid")

        val entryPrice = position["avgEntryPrice"].requiredNumber("GUM $symbol entry price")
        val markPrice = position["markPrice"].requiredNumber("GUM $symbol mark price")
        val liquidationPrice =
            liquidationElement.requiredNumber("GUM $symbol liquidation price")
        val maintenanceMarginRatioBps =
            position["maintenanceMarginRatioBps"].requiredNumber("GUM $symbol maintenance margin ratio")
        if (entryPrice <= 0.0 || markPrice <= 0.0 || liquidationPrice <= 0.0) {
            error("GUM $symbol prices must be positive")
        }

        val accountData = account.data ?: error("GUM ${account.pubkey} account data is missing")
        val accountValue = accountData["accountValue"]
            .requiredNumber("GUM ${account.pubkey} account value")
        val totalLiquidationRequirement = accountData["totalLiquidationRequirement"]
            .requiredNumber("GUM ${account.pubkey} liquidation requirement")
        if (accountValue <= 0.0) error("GUM ${account.pubkey} account value is not positive")

        val marketObject = market as JsonObject
        val takerFeeBps = marketObject["takerFeeBps"].requiredNumber("GUM $symbol taker fee")
        val liquidationFeeBps = marketObject["liquidationFeeBps"]
            .requiredNumber("GUM $symbol liquidation fee")
        val sizeUsd = size * markPrice
        val ratio = maintenanceMarginRatioBps / 10_000.0 +
            (takerFeeBps + liquidationFeeBps) / 10_000.0
        if (ratio < 0.0 || ratio >= 1.0) error("GUM $symbol margin ratio is invalid")
        val other = totalLiquidationRequirement - sizeUsd * ratio
        val side = if (sideValue == "Bid") "long" else "short"
        val calculatedLiquidationPrice = if (side == "long") {
            (other - accountValue + markPrice * size) / (size * (1.0 - ratio))
        } else {
            (accountValue - other + markPrice * size) / (size * (1.0 + ratio))
        }
        val relativeError = kotlin.math.abs(calculatedLiquidationPrice - liquidationPrice) /
            liquidationPrice
        if (!sizeUsd.isFinite() || !ratio.isFinite() || !other.isFinite()) {
            error("GUM $symbol calculation is not finite")
        }
        if (!calculatedLiquidationPrice.isFinite() || relativeError > 0.005) {
            error("GUM $symbol liquidation mismatch $calculatedLiquidationPrice vs $liquidationPrice")
        }
        val closeFeeUsd = sizeUsd * takerFeeBps / 10_000.0
        val priceTime = marketObject["timestamp"].requiredNumber("GUM $symbol market timestamp")
        if (!closeFeeUsd.isFinite()) error("GUM $symbol close fee is not finite")
        val collateralUsd = if (account.isolated) {
            position["collateral"].requiredNumber("GUM $symbol collateral").also {
                if (it <= 0.0) error("GUM $symbol collateral is not positive")
            }
        } else {
            accountValue
        }
        return ChainPosition(
            position = PositionDto(
                protocol = "jupiter-beta",
                address = "${account.pubkey}:$symbol",
                asset = symbol,
                side = side,
                entryPrice = entryPrice,
                sizeUsd = sizeUsd,
                collateralUsd = collateralUsd,
                closeFeeUsd = closeFeeUsd,
                borrowFeeUsd = 0.0,
                oraclePrice = markPrice,
                priceSource = "protocol",
                liquidationPrice = liquidationPrice,
                link = Config.JUPITER_LINK,
            ),
            priceTime = priceTime,
        )
    }
}

private data class GumAccount(
    val pubkey: String,
    val isolated: Boolean,
    val market: String?,
    val data: JsonObject?,
)

private fun JsonElement?.requiredString(label: String): String {
    val primitive = this as? JsonPrimitive ?: error("$label is missing")
    if (!primitive.isString) error("$label is invalid")
    return primitive.content
}

private fun JsonElement?.requiredBoolean(label: String): Boolean {
    val primitive = this as? JsonPrimitive ?: error("$label is missing")
    if (primitive.isString) error("$label is invalid")
    return when (primitive.content) {
        "true" -> true
        "false" -> false
        else -> error("$label is invalid")
    }
}

private fun JsonElement?.requiredNumber(label: String): Double {
    val primitive = this as? JsonPrimitive ?: error("$label is missing")
    if (primitive.isString && primitive.content.isBlank()) error("$label is missing")
    val value = primitive.content.toDoubleOrNull() ?: error("$label is not finite")
    if (!value.isFinite()) error("$label is not finite")
    return value
}

private fun JsonElement?.contentEquals(expected: String): Boolean =
    (this as? JsonPrimitive)?.content == expected
