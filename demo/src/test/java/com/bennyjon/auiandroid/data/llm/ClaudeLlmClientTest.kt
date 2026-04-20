package com.bennyjon.auiandroid.data.llm

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClaudeLlmClientTest {

    private val client = ClaudeLlmClient(
        apiKey = "test-key",
        httpClient = HttpClient(),
    )

    @Test
    fun `extractContentText concatenates multiple text blocks`() {
        val root = Json.parseToJsonElement(
            """
            {
              "id": "msg_split",
              "content": [
                { "type": "text", "text": "```json\n{\"text\": \"Hello" },
                { "type": "text", "text": " there\", \"aui\": {\"display\": \"expanded\", \"blocks\": []}}\n```" }
              ]
            }
            """.trimIndent()
        ).jsonObject

        val result = client.extractContentText(root)

        assertEquals(
            "```json\n{\"text\": \"Hello there\", \"aui\": {\"display\": \"expanded\", \"blocks\": []}}\n```",
            result,
        )
    }

    @Test
    fun `extractContentText ignores non text blocks`() {
        val root = Json.parseToJsonElement(
            """
            {
              "content": [
                { "type": "tool_use", "id": "tool_1", "name": "search" },
                { "type": "text", "text": "hello" }
              ]
            }
            """.trimIndent()
        ).jsonObject

        val result = client.extractContentText(root)

        assertEquals("hello", result)
    }

    @Test
    fun `extractContentText returns null when no text blocks exist`() {
        val root = Json.parseToJsonElement(
            """
            {
              "content": [
                { "type": "tool_use", "id": "tool_1", "name": "search" }
              ]
            }
            """.trimIndent()
        ).jsonObject

        val result = client.extractContentText(root)

        assertNull(result)
    }
}
