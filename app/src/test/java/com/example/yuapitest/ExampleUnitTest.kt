package com.example.yuapitest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import okhttp3.FormBody
import okhttp3.MultipartBody
import org.junit.Test

class ApiTesterUnitTest {
    @Test
    fun historyCodecRoundTripPreservesRequest() {
        val draft = ApiRequestDraft(
            method = "POST",
            url = "https://example.com/api",
            headers = listOf(
                HeaderPair("Authorization", "Bearer token"),
                HeaderPair("Content-Type", "application/json")
            ),
            params = listOf(HeaderPair("page", "2")),
            auth = AuthConfig(type = AuthType.API_KEY, apiKeyName = "api_key", apiKeyValue = "secret"),
            bodyType = BodyType.FORM,
            formFields = listOf(HeaderPair("name", "mobile")),
            multipartFields = listOf(HeaderPair("note", "hello")),
            body = """{"hello":"world"}"""
        ).normalized()

        val history = RequestHistoryRules.addToHistory(
            current = emptyList(),
            draft = draft,
            timestamp = 1234L,
            id = "first"
        )

        val decoded = RequestHistoryCodec.decode(RequestHistoryCodec.encode(history))

        assertEquals(1, decoded.size)
        assertEquals("first", decoded.first().id)
        assertEquals("POST", decoded.first().method)
        assertEquals("https://example.com/api", decoded.first().url)
        assertEquals("Authorization", decoded.first().headers.first().key)
        assertEquals("""{"hello":"world"}""", decoded.first().body)
        assertEquals("page", decoded.first().params.first().key)
        assertEquals(AuthType.API_KEY, decoded.first().auth.type)
        assertEquals("secret", decoded.first().auth.apiKeyValue)
        assertEquals(BodyType.FORM, decoded.first().bodyType)
        assertEquals("mobile", decoded.first().formFields.first().value)
        assertEquals("hello", decoded.first().multipartFields.first().value)
    }

    @Test
    fun requestBuilderAppliesParamsBearerAndFormBody() {
        val request = ApiHttpClient().createRequest(
            ApiRequestDraft(
                method = "POST",
                url = "https://example.com/items?existing=yes",
                params = listOf(HeaderPair("page", "2")),
                auth = AuthConfig(type = AuthType.BEARER, token = "abc123"),
                bodyType = BodyType.FORM,
                formFields = listOf(HeaderPair("name", "mobile"))
            )
        )

        assertEquals("yes", request.url.queryParameter("existing"))
        assertEquals("2", request.url.queryParameter("page"))
        assertEquals("Bearer abc123", request.header("Authorization"))
        val body = request.body as FormBody
        assertEquals("name", body.name(0))
        assertEquals("mobile", body.value(0))
    }

    @Test
    fun requestBuilderAppliesQueryApiKeyAndMultipartBody() {
        val request = ApiHttpClient().createRequest(
            ApiRequestDraft(
                method = "PUT",
                url = "https://example.com/upload",
                auth = AuthConfig(
                    type = AuthType.API_KEY,
                    apiKeyName = "key",
                    apiKeyValue = "value",
                    apiKeyLocation = ApiKeyLocation.QUERY
                ),
                bodyType = BodyType.MULTIPART,
                multipartFields = listOf(HeaderPair("description", "sample"))
            )
        )

        assertEquals("value", request.url.queryParameter("key"))
        assertTrue(request.body is MultipartBody)
        assertEquals(1, (request.body as MultipartBody).size)
    }

    @Test
    fun historyKeepsMostRecentFiftyEntries() {
        var history = emptyList<RequestHistoryEntry>()

        repeat(55) { index ->
            history = RequestHistoryRules.addToHistory(
                current = history,
                draft = ApiRequestDraft(url = "https://example.com/$index").normalized(),
                timestamp = index.toLong(),
                id = "id-$index"
            )
        }

        assertEquals(50, history.size)
        assertEquals("id-54", history.first().id)
        assertEquals("id-5", history.last().id)
    }

    @Test
    fun jsonParserFormatsObjectsAndArrays() {
        val result = JsonTools.parse("""{"ok":true,"items":[1,"two",null]}""")

        assertTrue(result is JsonParseResult.Success)
        val pretty = JsonTools.prettyPrint((result as JsonParseResult.Success).value)

        assertTrue(pretty.contains(""""ok": true"""))
        assertTrue(pretty.contains(""""items": ["""))
        assertTrue(pretty.contains("null"))
    }

    @Test
    fun invalidJsonReturnsFailure() {
        val result = JsonTools.parse("""{"missing":]""")

        assertTrue(result is JsonParseResult.Failure)
    }
}
