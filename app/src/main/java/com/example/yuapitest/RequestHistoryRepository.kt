package com.example.yuapitest

import android.content.Context

class RequestHistoryRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "api_request_history",
        Context.MODE_PRIVATE
    )

    fun load(): List<RequestHistoryEntry> {
        val json = preferences.getString(KEY_HISTORY, "[]").orEmpty()
        return RequestHistoryCodec.decode(json)
    }

    fun addRequest(draft: ApiRequestDraft): List<RequestHistoryEntry> {
        val next = RequestHistoryRules.addToHistory(
            current = load(),
            draft = draft.normalized(),
            timestamp = System.currentTimeMillis()
        )
        save(next)
        return next
    }

    fun clear() {
        preferences.edit().remove(KEY_HISTORY).apply()
    }

    private fun save(entries: List<RequestHistoryEntry>) {
        preferences.edit()
            .putString(KEY_HISTORY, RequestHistoryCodec.encode(entries))
            .apply()
    }

    private companion object {
        const val KEY_HISTORY = "history"
    }
}

object RequestHistoryRules {
    const val MaxEntries = 50

    fun addToHistory(
        current: List<RequestHistoryEntry>,
        draft: ApiRequestDraft,
        timestamp: Long,
        id: String = "$timestamp-${draft.method}-${draft.url.hashCode()}"
    ): List<RequestHistoryEntry> {
        if (draft.url.isBlank()) {
            return current.take(MaxEntries)
        }

        val entry = RequestHistoryEntry(
            id = id,
            timestamp = timestamp,
            method = draft.method,
            url = draft.url,
            headers = draft.headers,
            body = draft.body,
            params = draft.params,
            auth = draft.auth,
            bodyType = draft.bodyType,
            formFields = draft.formFields,
            multipartFields = draft.multipartFields
        )
        return (listOf(entry) + current).take(MaxEntries)
    }
}

object RequestHistoryCodec {
    fun encode(entries: List<RequestHistoryEntry>): String {
        val historyJson = JsonValue.Arr(entries.map { entry ->
            JsonValue.Obj(
                listOf(
                    JsonMember("id", JsonValue.Str(entry.id)),
                    JsonMember("timestamp", JsonValue.Num(entry.timestamp.toString())),
                    JsonMember("method", JsonValue.Str(entry.method)),
                    JsonMember("url", JsonValue.Str(entry.url)),
                    JsonMember("headers", encodeHeaders(entry.headers)),
                    JsonMember("body", JsonValue.Str(entry.body)),
                    JsonMember("params", encodeHeaders(entry.params)),
                    JsonMember("auth", encodeAuth(entry.auth)),
                    JsonMember("bodyType", JsonValue.Str(entry.bodyType.name)),
                    JsonMember("formFields", encodeHeaders(entry.formFields)),
                    JsonMember("multipartFields", encodeHeaders(entry.multipartFields))
                )
            )
        })
        return JsonTools.compactPrint(historyJson)
    }

    fun decode(json: String): List<RequestHistoryEntry> {
        if (json.isBlank()) {
            return emptyList()
        }

        val parsed = JsonTools.parse(json)
        if (parsed !is JsonParseResult.Success) {
            return emptyList()
        }

        val root = parsed.value as? JsonValue.Arr ?: return emptyList()
        return root.values.mapNotNull { value ->
            val obj = value as? JsonValue.Obj ?: return@mapNotNull null
            val members = obj.entries.associate { it.key to it.value }
            RequestHistoryEntry(
                id = members["id"].asStringOrNull() ?: return@mapNotNull null,
                timestamp = members["timestamp"].asLongOrNull() ?: return@mapNotNull null,
                method = members["method"].asStringOrNull() ?: return@mapNotNull null,
                url = members["url"].asStringOrNull() ?: return@mapNotNull null,
                headers = members["headers"].asHeaders(),
                body = members["body"].asStringOrNull().orEmpty(),
                params = members["params"].asHeaders(),
                auth = members["auth"].asAuth(),
                bodyType = members["bodyType"].asEnumOrDefault(BodyType.JSON),
                formFields = members["formFields"].asHeaders(),
                multipartFields = members["multipartFields"].asHeaders()
            )
        }.take(RequestHistoryRules.MaxEntries)
    }

    private fun encodeHeaders(headers: List<HeaderPair>): JsonValue.Arr {
        return JsonValue.Arr(headers.map { header ->
            JsonValue.Obj(
                listOf(
                    JsonMember("key", JsonValue.Str(header.key)),
                    JsonMember("value", JsonValue.Str(header.value))
                )
            )
        })
    }

    private fun encodeAuth(auth: AuthConfig): JsonValue.Obj {
        return JsonValue.Obj(
            listOf(
                JsonMember("type", JsonValue.Str(auth.type.name)),
                JsonMember("token", JsonValue.Str(auth.token)),
                JsonMember("username", JsonValue.Str(auth.username)),
                JsonMember("password", JsonValue.Str(auth.password)),
                JsonMember("apiKeyName", JsonValue.Str(auth.apiKeyName)),
                JsonMember("apiKeyValue", JsonValue.Str(auth.apiKeyValue)),
                JsonMember("apiKeyLocation", JsonValue.Str(auth.apiKeyLocation.name))
            )
        )
    }

    private fun JsonValue?.asStringOrNull(): String? {
        return (this as? JsonValue.Str)?.value
    }

    private fun JsonValue?.asLongOrNull(): Long? {
        return when (this) {
            is JsonValue.Num -> raw.toLongOrNull()
            is JsonValue.Str -> value.toLongOrNull()
            else -> null
        }
    }

    private fun JsonValue?.asHeaders(): List<HeaderPair> {
        val array = this as? JsonValue.Arr ?: return emptyList()
        return array.values.mapNotNull { value ->
            val obj = value as? JsonValue.Obj ?: return@mapNotNull null
            val members = obj.entries.associate { it.key to it.value }
            val key = members["key"].asStringOrNull() ?: return@mapNotNull null
            val headerValue = members["value"].asStringOrNull().orEmpty()
            HeaderPair(key, headerValue)
        }
    }

    private fun JsonValue?.asAuth(): AuthConfig {
        val obj = this as? JsonValue.Obj ?: return AuthConfig()
        val members = obj.entries.associate { it.key to it.value }
        return AuthConfig(
            type = members["type"].asEnumOrDefault(AuthType.NONE),
            token = members["token"].asStringOrNull().orEmpty(),
            username = members["username"].asStringOrNull().orEmpty(),
            password = members["password"].asStringOrNull().orEmpty(),
            apiKeyName = members["apiKeyName"].asStringOrNull() ?: "X-API-Key",
            apiKeyValue = members["apiKeyValue"].asStringOrNull().orEmpty(),
            apiKeyLocation = members["apiKeyLocation"].asEnumOrDefault(ApiKeyLocation.HEADER)
        )
    }

    private inline fun <reified T : Enum<T>> JsonValue?.asEnumOrDefault(default: T): T {
        val name = asStringOrNull() ?: return default
        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
