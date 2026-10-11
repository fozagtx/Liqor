package com.liqor.data

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val values: SharedPreferences =
        context.getSharedPreferences("liqor", Context.MODE_PRIVATE)

    var walletAddress: String?
        get() = values.getString(KEY_WALLET, null)
        set(value) {
            values.edit().putString(KEY_WALLET, value).apply()
        }

    var mode: String
        get() = when (values.getString(KEY_MODE, MODE_NONE)) {
            MODE_NONE -> MODE_NONE
            MODE_LIVE -> MODE_LIVE
            else -> MODE_NONE
        }
        set(value) {
            require(value in setOf(MODE_NONE, MODE_LIVE))
            values.edit().putString(KEY_MODE, value).apply()
        }

    var watchingEnabled: Boolean
        get() = values.getBoolean(KEY_WATCHING_ENABLED, true)
        set(value) {
            values.edit().putBoolean(KEY_WATCHING_ENABLED, value).apply()
        }

    var notificationAsked: Boolean
        get() = values.getBoolean(KEY_NOTIFICATION_ASKED, false)
        set(value) {
            values.edit().putBoolean(KEY_NOTIFICATION_ASKED, value).apply()
        }

    var bubbleSide: String
        get() = values.getString(KEY_SIDE, SIDE_RIGHT) ?: SIDE_RIGHT
        set(value) {
            require(value in setOf(SIDE_LEFT, SIDE_RIGHT))
            values.edit().putString(KEY_SIDE, value).apply()
        }

    var bubbleYFraction: Float
        get() = values.getFloat(KEY_Y, 0.4f).coerceIn(0f, 1f)
        set(value) {
            values.edit().putFloat(KEY_Y, value.coerceIn(0f, 1f)).apply()
        }

    var hidden: Boolean
        get() = values.getBoolean(KEY_HIDDEN, false)
        set(value) {
            values.edit().putBoolean(KEY_HIDDEN, value).apply()
            hiddenAt = if (value) System.currentTimeMillis() else 0L
        }

    var hiddenAt: Long
        get() = values.getLong(KEY_HIDDEN_AT, 0L)
        set(value) {
            values.edit().putLong(KEY_HIDDEN_AT, value).apply()
        }

    fun clear() {
        values.edit().clear().apply()
    }

    companion object {
        const val MODE_NONE = "none"
        const val MODE_LIVE = "live"
        const val SIDE_LEFT = "left"
        const val SIDE_RIGHT = "right"

        private const val KEY_WALLET = "wallet"
        private const val KEY_MODE = "mode"
        private const val KEY_WATCHING_ENABLED = "watching_enabled"
        private const val KEY_NOTIFICATION_ASKED = "notification_asked"
        private const val KEY_SIDE = "bubble_side"
        private const val KEY_Y = "bubble_y"
        private const val KEY_HIDDEN = "bubble_hidden"
        private const val KEY_HIDDEN_AT = "bubble_hidden_at"
    }
}
