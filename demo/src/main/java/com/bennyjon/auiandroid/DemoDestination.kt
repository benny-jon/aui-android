package com.bennyjon.auiandroid

import androidx.compose.ui.unit.dp

/** Corner radius used by the home cards and the card-origin transition shell. */
val DemoHomeCardCornerRadius = 28.dp

/**
 * Demo destinations reachable from the landing screen.
 *
 * Each destination is anchored to a specific home card so the transition shell knows which
 * card to expand from and collapse back into.
 */
enum class DemoDestination(
    val route: String,
    val homeTitle: String,
    val homeSubtitle: String,
) {
    LIVE_CHAT(
        route = "live_chat",
        homeTitle = "Live Chat",
        homeSubtitle = "End-to-end conversation with an LLM, persisted in Room",
    ),
    SHOWCASE(
        route = "showcase",
        homeTitle = "All Blocks Showcase",
        homeSubtitle = "Every AUI component in one scrollable list",
    ),
    SETTINGS(
        route = "settings",
        homeTitle = "Settings",
        homeSubtitle = "Configure demo settings",
    ),
    SYSTEM_PROMPT(
        route = "settings/system_prompt",
        homeTitle = "System Prompt",
        homeSubtitle = "Inspect and copy the generated AUI prompt",
    ),
    WARM_ORGANIC_CHAT(
        route = "warm_organic",
        homeTitle = "Warm Organic",
        homeSubtitle = "Earthy tones, serif headings, rounded corners",
    ),
    EARTHY_GREEN_CHAT(
        route = "earthy_green",
        homeTitle = "Earthy Green",
        homeSubtitle = "Forest greens, clean sans-serif, softly rounded",
    ),
    ;
}
