package com.bennyjon.aui.core

import com.bennyjon.aui.core.model.AuiBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class InputDataSerializationTest {

    @Test
    fun `input_text_single submit_label maps to submitLabel`() {
        val response = AuiParser().parse(
            """
            {
              "display": "inline",
              "blocks": [
                {
                  "type": "input_text_single",
                  "data": {
                    "key": "nickname",
                    "label": "Nickname",
                    "placeholder": "Optional",
                    "submit_label": "Save nickname"
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val block = response.blocks.single() as AuiBlock.InputTextSingle
        assertEquals("Save nickname", block.data.submitLabel)
    }
}
