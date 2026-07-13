package com.example.yuapitest

val SupportedHttpMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD")

private val MethodsWithRequestBody = setOf("POST", "PUT", "PATCH", "DELETE")
private val MethodsRequiringRequestBody = setOf("POST", "PUT", "PATCH")

data class HeaderPair(
    val key: String = "",
    val value: String = ""
)

enum class AuthType {
    NONE,
    BEARER,
    BASIC,
    API_KEY
}

enum class ApiKeyLocation {
    HEADER,
    QUERY
}

data class AuthConfig(
    val type: AuthType = AuthType.NONE,
    val token: String = "",
    val username: String = "",
    val password: String = "",
    val apiKeyName: String = "X-API-Key",
    val apiKeyValue: String = "",
    val apiKeyLocation: ApiKeyLocation = ApiKeyLocation.HEADER
)

enum class BodyType {
    JSON,
    RAW,
    FORM,
    MULTIPART
}

data class ApiRequestDraft(
    val method: String = "GET",
    val url: String = "",
    val headers: List<HeaderPair> = listOf(HeaderPair()),
    val params: List<HeaderPair> = listOf(HeaderPair()),
    val auth: AuthConfig = AuthConfig(),
    val bodyType: BodyType = BodyType.JSON,
    val body: String = "",
    val formFields: List<HeaderPair> = listOf(HeaderPair()),
    val multipartFields: List<HeaderPair> = listOf(HeaderPair())
) {
    fun normalized(): ApiRequestDraft {
        val cleanMethod = method.uppercase().takeIf { it in SupportedHttpMethods } ?: "GET"
        val cleanHeaders = headers
            .map { HeaderPair(it.key.trim(), it.value.trim()) }
            .filter { it.key.isNotEmpty() }

        return copy(
            method = cleanMethod,
            url = url.trim(),
            headers = cleanHeaders,
            params = params.normalizedPairs(),
            auth = auth.copy(
                token = auth.token.trim(),
                username = auth.username.trim(),
                apiKeyName = auth.apiKeyName.trim(),
                apiKeyValue = auth.apiKeyValue.trim()
            ),
            formFields = formFields.normalizedPairs(),
            multipartFields = multipartFields.normalizedPairs(),
            body = body
        )
    }
}

data class ApiResponseResult(
    val statusCode: Int? = null,
    val statusMessage: String = "",
    val elapsedMs: Long? = null,
    val bodySizeBytes: Long? = null,
    val protocol: String = "",
    val headers: List<HeaderPair> = emptyList(),
    val body: String = "",
    val errorMessage: String? = null,
    val requestWasSent: Boolean = false
)

data class RequestHistoryEntry(
    val id: String,
    val timestamp: Long,
    val method: String,
    val url: String,
    val headers: List<HeaderPair>,
    val body: String,
    val params: List<HeaderPair> = emptyList(),
    val auth: AuthConfig = AuthConfig(),
    val bodyType: BodyType = BodyType.JSON,
    val formFields: List<HeaderPair> = emptyList(),
    val multipartFields: List<HeaderPair> = emptyList()
) {
    fun toDraft(): ApiRequestDraft {
        return ApiRequestDraft(
            method = method,
            url = url,
            headers = headers.ifEmpty { listOf(HeaderPair()) },
            params = params.ifEmpty { listOf(HeaderPair()) },
            auth = auth,
            bodyType = bodyType,
            body = body,
            formFields = formFields.ifEmpty { listOf(HeaderPair()) },
            multipartFields = multipartFields.ifEmpty { listOf(HeaderPair()) }
        )
    }
}

fun String.supportsRequestBody(): Boolean = uppercase() in MethodsWithRequestBody

fun String.requiresRequestBody(): Boolean = uppercase() in MethodsRequiringRequestBody

private fun List<HeaderPair>.normalizedPairs(): List<HeaderPair> {
    return map { HeaderPair(it.key.trim(), it.value.trim()) }
        .filter { it.key.isNotEmpty() }
}
