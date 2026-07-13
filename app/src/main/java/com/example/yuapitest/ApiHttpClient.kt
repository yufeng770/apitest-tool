package com.example.yuapitest

import android.os.SystemClock
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ApiHttpClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    suspend fun execute(draft: ApiRequestDraft): ApiResponseResult = withContext(Dispatchers.IO) {
        val requestDraft = draft.normalized()
        if (requestDraft.url.isBlank()) {
            return@withContext ApiResponseResult(errorMessage = "请输入 URL")
        }

        try {
            requestDraft.url.toHttpUrl()
        } catch (_: IllegalArgumentException) {
            return@withContext ApiResponseResult(errorMessage = "URL 无效，请使用 http:// 或 https:// 开头的完整地址")
        }

        val request = try {
            createRequest(requestDraft)
        } catch (error: IllegalArgumentException) {
            return@withContext ApiResponseResult(errorMessage = error.message ?: "请求参数无效")
        }

        val startedAt = SystemClock.elapsedRealtime()
        try {
            client.newCall(request).execute().use { response ->
                val elapsedMs = SystemClock.elapsedRealtime() - startedAt
                val responseBytes = response.body?.bytes() ?: ByteArray(0)
                val charset = response.body?.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                ApiResponseResult(
                    statusCode = response.code,
                    statusMessage = response.message,
                    elapsedMs = elapsedMs,
                    bodySizeBytes = responseBytes.size.toLong(),
                    protocol = response.protocol.displayName(),
                    headers = response.headers.toHeaderPairs(),
                    body = responseBytes.toString(charset),
                    errorMessage = null,
                    requestWasSent = true
                )
            }
        } catch (error: IOException) {
            val elapsedMs = SystemClock.elapsedRealtime() - startedAt
            ApiResponseResult(
                elapsedMs = elapsedMs,
                errorMessage = "请求失败：${error.message ?: error.javaClass.simpleName}",
                requestWasSent = true
            )
        }
    }

    internal fun createRequest(draft: ApiRequestDraft): Request {
        val normalized = draft.normalized()
        val url = buildUrl(normalized)
        return buildRequest(normalized.copy(url = url.toString()))
    }

    private fun buildUrl(draft: ApiRequestDraft): okhttp3.HttpUrl {
        val builder = draft.url.toHttpUrl().newBuilder()
        draft.params.forEach { param ->
            builder.addQueryParameter(param.key, param.value)
        }
        if (draft.auth.type == AuthType.API_KEY && draft.auth.apiKeyLocation == ApiKeyLocation.QUERY) {
            require(draft.auth.apiKeyName.isNotBlank()) { "API Key 名称不能为空" }
            builder.addQueryParameter(draft.auth.apiKeyName, draft.auth.apiKeyValue)
        }
        return builder.build()
    }

    private fun buildRequest(draft: ApiRequestDraft): Request {
        val builder = Request.Builder().url(draft.url)
        draft.headers.forEach { header ->
            builder.addHeader(header.key, header.value)
        }

        when (draft.auth.type) {
            AuthType.NONE -> Unit
            AuthType.BEARER -> builder.header("Authorization", "Bearer ${draft.auth.token}")
            AuthType.BASIC -> builder.header(
                "Authorization",
                Credentials.basic(draft.auth.username, draft.auth.password)
            )
            AuthType.API_KEY -> if (draft.auth.apiKeyLocation == ApiKeyLocation.HEADER) {
                require(draft.auth.apiKeyName.isNotBlank()) { "API Key 名称不能为空" }
                builder.header(draft.auth.apiKeyName, draft.auth.apiKeyValue)
            }
        }

        val body = when {
            draft.method.supportsRequestBody() -> when (draft.bodyType) {
                BodyType.JSON -> draft.body.toRequestBody(JsonMediaType)
                BodyType.RAW -> draft.body.toRequestBody(TextMediaType)
                BodyType.FORM -> FormBody.Builder().apply {
                    draft.formFields.forEach { add(it.key, it.value) }
                }.build()
                BodyType.MULTIPART -> MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .apply {
                        draft.multipartFields.forEach { addFormDataPart(it.key, it.value) }
                    }
                    .build()
            }
            else -> null
        }

        return builder.method(draft.method, body).build()
    }

    private companion object {
        val JsonMediaType = "application/json; charset=utf-8".toMediaType()
        val TextMediaType = "text/plain; charset=utf-8".toMediaType()
    }
}

private fun Protocol.displayName(): String = when (this) {
    Protocol.HTTP_2 -> "HTTP/2"
    Protocol.HTTP_1_1 -> "HTTP/1.1"
    Protocol.HTTP_1_0 -> "HTTP/1.0"
    Protocol.H2_PRIOR_KNOWLEDGE -> "H2"
    Protocol.QUIC -> "QUIC"
    Protocol.SPDY_3 -> "SPDY/3"
}

private fun okhttp3.Headers.toHeaderPairs(): List<HeaderPair> {
    return names().flatMap { name ->
        values(name).map { value -> HeaderPair(name, value) }
    }
}
