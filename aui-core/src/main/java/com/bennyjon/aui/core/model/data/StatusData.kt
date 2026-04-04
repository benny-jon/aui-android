package com.bennyjon.aui.core.model.data

import kotlinx.serialization.Serializable

/** Data for the `badge_success` block. A small success-colored pill. */
@Serializable
data class BadgeSuccessData(val text: String)

/** Data for the `status_banner_success` block. A full-width success confirmation banner. */
@Serializable
data class StatusBannerSuccessData(val text: String)
