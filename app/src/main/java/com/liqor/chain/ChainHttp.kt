package com.liqor.chain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit

internal data class HttpBytes(
    val status: Int,
    val body: ByteArray,
)

internal interface ChainHttp {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpBytes

    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpBytes
}

internal class OkHttpChainHttp(
    private val client: OkHttpClient = defaultClient(),
) : ChainHttp {
    override suspend fun get(url: String, headers: Map<String, String>): HttpBytes =
        execute(Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
        }.get().build())

    override suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): HttpBytes =
        execute(Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
        }.post(body.toRequestBody(JSON)).build())

    private suspend fun execute(request: Request): HttpBytes = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            HttpBytes(
                status = response.code,
                body = response.body.string().toByteArray(UTF_8),
            )
        }
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}
