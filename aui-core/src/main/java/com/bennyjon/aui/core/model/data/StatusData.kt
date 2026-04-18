package com.bennyjon.aui.core.model.data

import kotlinx.serialization.Serializable

/** Data for the `badge_info` block. A small info-colored pill. */
@Serializable
data class BadgeInfoData(val text: String)

/** Data for the `badge_success` block. A small success-colored pill. */
@Serializable
data class BadgeSuccessData(val text: String)

/** Data for the `badge_warning` block. A small warning-colored pill. */
@Serializable
data class BadgeWarningData(val text: String)

/** Data for the `badge_error` block. A small error-colored pill. */
@Serializable
data class BadgeErrorData(val text: String)

/** Data for the `status_banner_info` block. A full-width info banner. */
@Serializable
data class StatusBannerInfoData(val text: String)

/** Data for the `status_banner_success` block. A full-width success confirmation banner. */
@Serializable
data class StatusBannerSuccessData(val text: String)

/** Data for the `status_banner_warning` block. A full-width warning banner. */
@Serializable
data class StatusBannerWarningData(val text: String)

/** Data for the `status_banner_error` block. A full-width error banner. */
@Serializable
data class StatusBannerErrorData(val text: String)
