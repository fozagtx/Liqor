package com.liqor.wallet

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Base58Test {
    @Test
    fun decodesAndEncodesTheRealWalletAddress() {
        val address = "PERPHjGBqRHArX4DySjwM6UJHiR3sWAatqfdBS2qQJu"

        assertEquals(address, Base58.encode(Base58.decode(address)))
    }

    @Test
    fun roundTripsLeadingZeroBytes() {
        val bytes = byteArrayOf(0, 0, 1, 127, -1)

        assertArrayEquals(bytes, Base58.decode(Base58.encode(bytes)))
    }
}
