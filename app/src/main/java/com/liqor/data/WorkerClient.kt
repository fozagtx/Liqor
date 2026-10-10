package com.liqor.data

import com.liqor.Config.WORKER_URL
import com.liqor.domain.ErrorResponse
import com.liqor.domain.PositionsResponse
import com.liqor.domain.Reading
import com.liqor.domain.positionJson
import com.liqor.domain.read
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl

class WorkerClient(
    private val client: OkHttpClient = defaultClient(),
) {
    suspend fun positions(walletAddress: String): Reading = withContext(Dispatchers.IO) {
        try {
            val url = WORKER_URL.toHttpUrl()
                .newBuilder()
                .addPathSegments("v1/positions")
                .addQueryParameter("wallet", walletAddress)
                .build()
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Reading.CouldNotCheck
                val body = response.body.string()
                val element = positionJson.parseToJsonElement(body)
                val obj = element as? JsonObject ?: return@withContext Reading.CouldNotCheck
                if ("error" in obj) {
                    positionJson.decodeFromJsonElement<ErrorResponse>(obj)
                    return@withContext Reading.CouldNotCheck
                }
                val positions = positionJson.decodeFromJsonElement<PositionsResponse>(obj)
                read(positions, Instant.now())
            }
        } catch (_: Exception) {
            Reading.CouldNotCheck
        }
    }

    private companion object {
        fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}
