package com.liqor.wallet

import java.math.BigInteger

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val radix = BigInteger.valueOf(ALPHABET.length.toLong())

    fun encode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val zeroCount = bytes.takeWhile { it == 0.toByte() }.size
        var value = BigInteger(1, bytes)
        val encoded = StringBuilder()
        while (value.signum() > 0) {
            val division = value.divideAndRemainder(radix)
            encoded.append(ALPHABET[division[1].toInt()])
            value = division[0]
        }
        repeat(zeroCount) { encoded.append(ALPHABET[0]) }
        return encoded.reverse().toString()
    }

    fun decode(text: String): ByteArray {
        require(text.all { it in ALPHABET })
        if (text.isEmpty()) return byteArrayOf()
        val zeroCount = text.takeWhile { it == ALPHABET[0] }.length
        var value = BigInteger.ZERO
        text.drop(zeroCount).forEach { char ->
            value = value.multiply(radix).add(BigInteger.valueOf(ALPHABET.indexOf(char).toLong()))
        }
        val bytes = if (value.signum() == 0) {
            byteArrayOf()
        } else {
            value.toByteArray().let { if (it.first() == 0.toByte()) it.drop(1).toByteArray() else it }
        }
        return ByteArray(zeroCount) + bytes
    }
}
