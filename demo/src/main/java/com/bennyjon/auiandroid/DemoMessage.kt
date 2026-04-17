package com.bennyjon.auiandroid

import java.util.UUID

/**
 * The demo app's own message model.
 *
 * This is NOT part of the AUI library — every host app defines its own message type.
 * The library is a pure renderer; it does not manage chat history or message models.
 */
sealed class DemoMessage {

    /**
     * A message from the AI assistant.
     *
     * @param id Stable identifier used as a LazyColumn key.
     * @param text Optional plain-text portion of the response.
     * @param auiJson Optional raw AUI JSON to render via [com.bennyjon.aui.compose.AuiRenderer].
     *   Set to `null` after a survey submission to prevent the survey from re-opening on scroll.
     */
    data class Ai(
        val id: String = UUID.randomUUID().toString(),
        val text: String? = null,
        val auiJson: String? = null,
    ) : DemoMessage()

    /**
     * A message from the user (typed text or feedback summary).
     *  @param id Stable identifier used as a LazyColumn key.
     *  */
    data class User(
        val id: String = UUID.randomUUID().toString(),
        val text: String
    ) : DemoMessage()
}
