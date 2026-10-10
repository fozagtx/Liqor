package com.liqor.chain

import com.liqor.Config
import com.liqor.domain.PositionDto
import com.liqor.wallet.Base58
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.pow

internal data class ChainPosition(
    val position: PositionDto,
    val priceTime: Double,
)

internal data class JupiterResult(
    val slot: Long,
    val positions: List<ChainPosition>,
)

internal data class Custody(
    val address: String,
    val mint: String,
    val isStable: Boolean,
    val tradeImpactFeeScalar: BigInteger,
    val maxLeverage: BigInteger,
    val owned: BigInteger,
    val locked: BigInteger,
    val cumulativeInterestRate: BigInteger,
    val lastUpdate: Long,
    val hourlyFundingDbps: BigInteger,
    val decreasePositionBps: BigInteger,
    val jumpMinRateBps: BigInteger,
    val jumpMaxRateBps: BigInteger,
    val jumpTargetRateBps: BigInteger,
    val jumpTargetUtilizationRate: BigInteger,
    val dovesAgOracle: String,
    val debt: BigInteger,
    val borrowLendInterestsAccured: BigInteger,
)

internal data class PerpsPosition(
    val address: String,
    val owner: String,
    val custody: String,
    val collateralCustody: String,
    val side: String,
    val price: BigInteger,
    val sizeUsd: BigInteger,
    val collateralUsd: BigInteger,
    val cumulativeInterestSnapshot: BigInteger,
)

internal data class OraclePrice(
    val price: Double,
    val timestamp: Long,
)

internal data class Liquidation(
    val liquidationPrice: BigInteger,
    val closeFeeUsd: BigInteger,
    val borrowFeeUsd: BigInteger,
)

internal data class Candidate(
    val address: String,
    val custody: String,
    val collateralCustody: String,
    val side: String,
)

internal data class PoolMeta(
    val fetchedAtMs: Long,
    val custodies: List<Custody>,
)

internal data class Account(
    val owner: String,
    val data: ByteArray,
)

internal data class AccountResponse(
    val slot: Long,
    val accounts: List<Account?>,
)

internal class JupiterPerps(
    private val http: ChainHttp,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val poolMetaMutex = Mutex()
    private var poolMeta: PoolMeta? = null

    suspend fun positionsFor(wallet: String): JupiterResult {
        val meta = loadPoolMeta()
        val keys = meta.custodies.map { it.address }
        val custodyCount = keys.size
        val candidates = positionCandidates(wallet, meta.custodies)
        val response = getMultipleAccounts(
            keys + meta.custodies.map { it.dovesAgOracle } + candidates.map { it.address },
        )
        val nowSeconds = nowMs() / 1_000L
        val byAddress = mutableMapOf<String, Custody>()
        response.accounts.take(custodyCount).forEachIndexed { index, account ->
            require(account != null && account.owner == PERPS_PROGRAM) { "custody account" }
            byAddress[keys[index]] = decodeCustody(keys[index], account.data)
        }
        val prices = response.accounts
            .drop(custodyCount)
            .take(custodyCount)
            .mapIndexed { index, account ->
                require(account != null) { "price account" }
                keys[index] to decodeAgPrice(account.data)
            }
            .toMap()
        val positions = response.accounts.drop(custodyCount * 2).mapIndexedNotNull { index, account ->
            if (account == null || account.owner != PERPS_PROGRAM || !isPosition(account.data)) {
                return@mapIndexedNotNull null
            }
            val candidate = candidates[index]
            val position = decodePosition(candidate.address, account.data)
            if (position.sizeUsd == BigInteger.ZERO || position.owner != wallet) {
                return@mapIndexedNotNull null
            }
            val custody = byAddress[position.custody]
            val collateral = byAddress[position.collateralCustody]
            val price = prices[position.custody]
            require(custody != null && collateral != null && price != null) { "unknown custody" }
            val liquidation = liquidation(position, custody, collateral, nowSeconds)
            toOutput(position, custody, liquidation, price)
        }
        return JupiterResult(response.slot, positions)
    }

    private suspend fun loadPoolMeta(): PoolMeta {
        poolMeta?.takeIf { nowMs() - it.fetchedAtMs < POOL_META_CACHE_MS }?.let { return it }
        return poolMetaMutex.withLock {
            poolMeta?.takeIf { nowMs() - it.fetchedAtMs < POOL_META_CACHE_MS }?.let { return@withLock it }
            val pool = getMultipleAccounts(listOf(JLP_POOL)).accounts.singleOrNull()
            require(pool != null && pool.owner == PERPS_PROGRAM) { "pool account" }
            val keys = decodePoolCustodies(pool.data)
            val response = getMultipleAccounts(keys)
            val custodies = response.accounts.mapIndexed { index, account ->
                require(account != null && account.owner == PERPS_PROGRAM) { "custody account" }
                decodeCustody(keys[index], account.data)
            }
            PoolMeta(nowMs(), custodies).also { poolMeta = it }
        }
    }

    private suspend fun getMultipleAccounts(keys: List<String>): AccountResponse {
        val request = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("id", JsonPrimitive(1))
            put("method", JsonPrimitive("getMultipleAccounts"))
            put(
                "params",
                JsonArray(
                    listOf(
                        JsonArray(keys.map { JsonPrimitive(it) }),
                        buildJsonObject {
                            put("encoding", JsonPrimitive("base64"))
                            put("commitment", JsonPrimitive("confirmed"))
                        },
                    ),
                ),
            )
        }
        val response = http.post(
            Config.SOLANA_RPC_URL,
            request.toString(),
            mapOf("content-type" to "application/json"),
        )
        val body = response.body.toString(Charsets.UTF_8)
        check(response.status in 200..299) { "rpc ${response.status} ${body.take(200)}" }
        val root = parseObject(body, "rpc")
        check(!root.containsKey("error")) { "rpc ${root["error"]}" }
        val result = root["result"]?.jsonObject ?: error("rpc no result")
        val context = result["context"]?.jsonObject ?: error("rpc context")
        val slot = context["slot"].asLong("rpc slot")
        val values = result["value"]?.jsonArray ?: error("rpc value")
        check(values.size == keys.size) { "rpc account count" }
        val accounts = values.map { value ->
            if (value == JsonNull) {
                null
            } else {
                val account = value.jsonObject
                val owner = account["owner"].asString("rpc owner")
                val encoded = account["data"]?.jsonArray?.firstOrNull().asString("rpc account data")
                Account(owner, Base64.getDecoder().decode(encoded))
            }
        }
        return AccountResponse(slot, accounts)
    }
}

internal fun decodePoolCustodies(data: ByteArray): List<String> {
    val reader = Reader(data)
    val nameLength = reader.u32(8).toInt()
    val vectorOffset = 12 + nameLength
    val count = reader.u32(vectorOffset).toInt()
    return List(count) { index -> reader.key(vectorOffset + 4 + index * 32) }
}

internal fun decodeCustody(address: String, data: ByteArray): Custody {
    val reader = Reader(data)
    return Custody(
        address = address,
        mint = reader.key(40),
        isStable = reader.u8(105) == 1,
        tradeImpactFeeScalar = reader.u64(151),
        maxLeverage = reader.u64(175),
        owned = reader.u64(222),
        locked = reader.u64(230),
        cumulativeInterestRate = reader.u128(262),
        lastUpdate = reader.i64(278),
        hourlyFundingDbps = reader.u64(286),
        decreasePositionBps = reader.u64(304),
        jumpMinRateBps = reader.u64(352),
        jumpMaxRateBps = reader.u64(360),
        jumpTargetRateBps = reader.u64(368),
        jumpTargetUtilizationRate = reader.u64(376),
        dovesAgOracle = reader.key(384),
        debt = reader.u128(1004),
        borrowLendInterestsAccured = reader.u128(1020),
    )
}

private val POSITION_DISCRIMINATOR = sha256("account:Position".toByteArray(UTF_8)).copyOfRange(0, 8)

internal fun isPosition(data: ByteArray): Boolean =
    data.size >= POSITION_MIN_SIZE && POSITION_DISCRIMINATOR.indices.all {
        data[it] == POSITION_DISCRIMINATOR[it]
    }

internal fun decodePosition(address: String, data: ByteArray): PerpsPosition {
    val reader = Reader(data)
    val side = reader.u8(152)
    require(side == 1 || side == 2) { "position side" }
    return PerpsPosition(
        address = address,
        owner = reader.key(8),
        custody = reader.key(72),
        collateralCustody = reader.key(104),
        side = if (side == 1) "long" else "short",
        price = reader.u64(153),
        sizeUsd = reader.u64(161),
        collateralUsd = reader.u64(169),
        cumulativeInterestSnapshot = reader.u128(185),
    )
}

internal fun decodeAgPrice(data: ByteArray): OraclePrice {
    val reader = Reader(data)
    return OraclePrice(
        price = reader.u64(168).toDouble() * 10.0.pow(reader.i8(176).toDouble()),
        timestamp = reader.i64(177),
    )
}

internal fun positionCandidates(wallet: String, custodies: List<Custody>): List<Candidate> {
    val program = Base58.decode(PERPS_PROGRAM)
    val owner = Base58.decode(wallet)
    val pool = Base58.decode(JLP_POOL)
    val seed = "position".toByteArray(UTF_8)
    fun derive(custody: String, collateral: String, side: String): Candidate =
        Candidate(
            address = findProgramAddress(
                listOf(
                    seed,
                    owner,
                    pool,
                    Base58.decode(custody),
                    Base58.decode(collateral),
                    byteArrayOf(if (side == "long") 1 else 2),
                ),
                program,
            ),
            custody = custody,
            collateralCustody = collateral,
            side = side,
        )

    val assets = custodies.filterNot { it.isStable }
    val stables = custodies.filter { it.isStable }
    return assets.flatMap { asset ->
        listOf(derive(asset.address, asset.address, "long")) +
            stables.map { stable -> derive(asset.address, stable.address, "short") }
    }
}

internal fun liquidation(
    position: PerpsPosition,
    custody: Custody,
    collateral: Custody,
    now: Long,
): Liquidation {
    val impactBps = if (custody.tradeImpactFeeScalar == BigInteger.ZERO) {
        BigInteger.ZERO
    } else {
        divCeil(position.sizeUsd * BPS_POWER, custody.tradeImpactFeeScalar)
    }
    val closeFeeUsd =
        position.sizeUsd * (custody.decreasePositionBps + impactBps) / BPS_POWER
    val borrowFeeUsd =
        (cumulativeInterestNow(collateral, now) - position.cumulativeInterestSnapshot) *
            position.sizeUsd / RATE_POWER
    val maxLossUsd = position.sizeUsd * BPS_POWER / custody.maxLeverage +
        closeFeeUsd + borrowFeeUsd
    val diff = abs(maxLossUsd - position.collateralUsd) * position.price / position.sizeUsd
    val lossExceedsMargin = maxLossUsd > position.collateralUsd
    val price = when (position.side) {
        "long" -> if (lossExceedsMargin) position.price + diff else position.price - diff
        else -> if (lossExceedsMargin) position.price - diff else position.price + diff
    }
    return Liquidation(price, closeFeeUsd, borrowFeeUsd)
}

internal fun toOutput(
    position: PerpsPosition,
    custody: Custody,
    liquidation: Liquidation,
    price: OraclePrice,
): ChainPosition {
    val asset = ASSET_SYMBOLS[custody.mint] ?: custody.mint.take(4)
    return ChainPosition(
        position = PositionDto(
            protocol = "jupiter-perps",
            address = position.address,
            asset = asset,
            side = position.side,
            entryPrice = position.price.toDouble() / USD_DECIMALS,
            sizeUsd = position.sizeUsd.toDouble() / USD_DECIMALS,
            collateralUsd = position.collateralUsd.toDouble() / USD_DECIMALS,
            closeFeeUsd = liquidation.closeFeeUsd.toDouble() / USD_DECIMALS,
            borrowFeeUsd = liquidation.borrowFeeUsd.toDouble() / USD_DECIMALS,
            oraclePrice = price.price,
            priceSource = "protocol",
            liquidationPrice = liquidation.liquidationPrice.toDouble() / USD_DECIMALS,
            link = Config.JUPITER_LINK,
        ),
        priceTime = price.timestamp.toDouble(),
    )
}

internal fun isOnCurve(bytes: ByteArray): Boolean {
    if (bytes.size != 32) return false
    val encoded = bytes.copyOf()
    val sign = (encoded[31].toInt() and 0xff) ushr 7
    encoded[31] = (encoded[31].toInt() and 0x7f).toByte()
    val y = BigInteger(1, encoded.reversedArray())
    if (y >= ED25519_P) return false
    val ySquared = y.multiply(y).mod(ED25519_P)
    val numerator = ySquared.subtract(BigInteger.ONE).mod(ED25519_P)
    val denominator = ED25519_D.multiply(ySquared).add(BigInteger.ONE).mod(ED25519_P)
    if (denominator == BigInteger.ZERO) return false
    val xSquared = numerator.multiply(denominator.modInverse(ED25519_P)).mod(ED25519_P)
    var x = xSquared.modPow(ED25519_SQRT_EXPONENT, ED25519_P)
    if (x.multiply(x).mod(ED25519_P) != xSquared) {
        x = x.multiply(ED25519_SQRT_M1).mod(ED25519_P)
    }
    if (x.multiply(x).mod(ED25519_P) != xSquared) return false
    return x != BigInteger.ZERO || sign == 0
}

private fun findProgramAddress(seeds: List<ByteArray>, program: ByteArray): String {
    for (bump in 255 downTo 0) {
        val digest = sha256(concat(seeds + byteArrayOf(bump.toByte()) + program + PDA_MARKER))
        if (!isOnCurve(digest)) return Base58.encode(digest)
    }
    error("no viable bump")
}

private fun concat(parts: List<ByteArray>): ByteArray {
    val result = ByteArray(parts.sumOf { it.size })
    var offset = 0
    parts.forEach { part ->
        part.copyInto(result, offset)
        offset += part.size
    }
    return result
}

private fun cumulativeInterestNow(custody: Custody, now: Long): BigInteger {
    if (now <= custody.lastUpdate || custody.owned == BigInteger.ZERO) {
        return custody.cumulativeInterestRate
    }
    return custody.cumulativeInterestRate +
        divCeil(hourlyBorrowRate(custody) * BigInteger.valueOf(now - custody.lastUpdate), BigInteger.valueOf(3600))
}

private fun hourlyBorrowRate(custody: Custody): BigInteger {
    val debt = divCeil(max(custody.debt - custody.borrowLendInterestsAccured, BigInteger.ZERO), DEBT_POWER)
    val owned = custody.owned + debt
    val locked = custody.locked + debt
    if (custody.hourlyFundingDbps != BigInteger.ZERO) {
        val hourly = custody.hourlyFundingDbps * RATE_POWER / DBPS_POWER
        return if (owned > BigInteger.ZERO && locked > BigInteger.ZERO) {
            divCeil(locked * hourly, owned)
        } else {
            BigInteger.ZERO
        }
    }
    val utilization = if (owned > BigInteger.ZERO && locked > BigInteger.ZERO) {
        locked * RATE_POWER / owned
    } else {
        BigInteger.ZERO
    }
    val yearly = if (utilization <= custody.jumpTargetUtilizationRate) {
        val rate = divCeil(
            (custody.jumpTargetRateBps - custody.jumpMinRateBps) * utilization,
            custody.jumpTargetUtilizationRate,
        ) + custody.jumpMinRateBps
        rate * RATE_POWER / BPS_POWER
    } else {
        val rateDifference = max(
            BigInteger.ZERO,
            custody.jumpMaxRateBps - custody.jumpTargetRateBps,
        )
        val utilizationDifference = max(
            BigInteger.ZERO,
            utilization - custody.jumpTargetUtilizationRate,
        )
        val denominator = max(
            BigInteger.ZERO,
            RATE_POWER - custody.jumpTargetUtilizationRate,
        )
        check(denominator != BigInteger.ZERO) { "borrow rate denominator is 0" }
        (divCeil(rateDifference * utilizationDifference, denominator) +
            custody.jumpTargetRateBps) * RATE_POWER / BPS_POWER
    }
    return yearly / HOURS_IN_A_YEAR
}

private fun divCeil(value: BigInteger, divisor: BigInteger): BigInteger {
    val (quotient, remainder) = value.divideAndRemainder(divisor)
    if (remainder == BigInteger.ZERO) return quotient
    return quotient + BigInteger.valueOf(if (quotient.signum() < 0) -1L else 1L)
}

private fun max(first: BigInteger, second: BigInteger): BigInteger =
    if (first > second) first else second

private fun abs(value: BigInteger): BigInteger = value.abs()

private fun sha256(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(input)

private class Reader(private val bytes: ByteArray) {
    fun u8(offset: Int): Int {
        requireRange(offset, 1)
        return bytes[offset].toInt() and 0xff
    }

    fun i8(offset: Int): Int {
        requireRange(offset, 1)
        return bytes[offset].toInt()
    }

    fun u32(offset: Int): Long {
        requireRange(offset, 4)
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffff_ffffL
    }

    fun u64(offset: Int): BigInteger = BigInteger(1, littleEndianBytes(offset, 8))

    fun i64(offset: Int): Long {
        requireRange(offset, 8)
        return ByteBuffer.wrap(bytes, offset, 8).order(ByteOrder.LITTLE_ENDIAN).long
    }

    fun u128(offset: Int): BigInteger {
        val low = u64(offset)
        val high = u64(offset + 8)
        return low + high.shiftLeft(64)
    }

    fun key(offset: Int): String = Base58.encode(byteArray(offset, 32))

    private fun littleEndianBytes(offset: Int, length: Int): ByteArray =
        byteArray(offset, length).reversedArray()

    private fun byteArray(offset: Int, length: Int): ByteArray {
        requireRange(offset, length)
        return bytes.copyOfRange(offset, offset + length)
    }

    private fun requireRange(offset: Int, length: Int) {
        require(offset >= 0 && length >= 0 && offset <= bytes.size - length) { "account data offset" }
    }
}

private fun parseObject(body: String, label: String): JsonObject =
    try {
        Json.parseToJsonElement(body).jsonObject
    } catch (exception: Exception) {
        throw IllegalStateException("$label invalid response", exception)
    }

private fun JsonElement?.asString(label: String): String {
    val primitive = this as? JsonPrimitive ?: error("$label missing")
    require(primitive.isString) { "$label invalid" }
    return primitive.content
}

private fun JsonElement?.asLong(label: String): Long {
    val primitive = this as? JsonPrimitive ?: error("$label missing")
    return primitive.content.toLongOrNull() ?: error("$label invalid")
}

private const val PERPS_PROGRAM = "PERPHjGBqRHArX4DySjwM6UJHiR3sWAatqfdBS2qQJu"
private const val JLP_POOL = "5BUwFW4nRbftYTDMbgxykoFWqWHPzahFSNAaaaJtVKsq"
private const val POOL_META_CACHE_MS = 10 * 60 * 1_000L
private const val POSITION_MIN_SIZE = 210
private const val USD_DECIMALS = 1_000_000.0
private val BPS_POWER = BigInteger.valueOf(10_000)
private val DBPS_POWER = BigInteger.valueOf(100_000)
private val RATE_POWER = BigInteger.valueOf(1_000_000_000)
private val DEBT_POWER = RATE_POWER
private val HOURS_IN_A_YEAR = BigInteger.valueOf(8_760)
private val PDA_MARKER = "ProgramDerivedAddress".toByteArray(UTF_8)
private val ED25519_P = BigInteger.ONE.shiftLeft(255).subtract(BigInteger.valueOf(19))
private val ED25519_D = BigInteger.valueOf(-121_665)
    .multiply(BigInteger.valueOf(121_666).modInverse(ED25519_P))
    .mod(ED25519_P)
private val ED25519_SQRT_M1 = BigInteger.TWO.modPow(
    ED25519_P.subtract(BigInteger.ONE).shiftRight(2),
    ED25519_P,
)
private val ED25519_SQRT_EXPONENT = ED25519_P.add(BigInteger.valueOf(3)).shiftRight(3)
private val ASSET_SYMBOLS = mapOf(
    "So11111111111111111111111111111111111111112" to "SOL",
    "7vfCXTUXx5WJV5JADk17DUJ4ksgau7utNKj4b963voxs" to "ETH",
    "3NZ9JMVBmGAqocybic2c7LQCJScmgsAZ6vQqTDzcqmJh" to "BTC",
    "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v" to "USDC",
    "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB" to "USDT",
    "JuprjznTrTSp2UFa3ZBUFgwdAmtZCq4MQCwysN55USD" to "JupUSD",
)
