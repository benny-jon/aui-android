package com.bennyjon.auiandroid.data.llm

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenAiLlmClientTest {

    private val client = OpenAiLlmClient(
        apiKey = "test-key",
        httpClient = HttpClient(),
    )

    @Test
    fun `extractErrorMessage returns provider error details`() {
        val root = Json.parseToJsonElement(
            """
            {
              "error": {
                "message": "The server is overloaded.",
                "type": "server_error",
                "code": "server_overloaded"
              }
            }
            """.trimIndent()
        ).jsonObject

        val result = client.extractErrorMessage(root)

        assertEquals(
            "OpenAI API error (server_error): The server is overloaded. [server_overloaded]",
            result,
        )
    }

    @Test
    fun `extractErrorMessage returns null for success payload`() {
        val root = Json.parseToJsonElement(
            """
            {
              "id": "chatcmpl_123",
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "{\"text\":\"hello\"}"
                  }
                }
              ]
            }
            """.trimIndent()
        ).jsonObject

        val result = client.extractErrorMessage(root)

        assertNull(result)
    }
}
