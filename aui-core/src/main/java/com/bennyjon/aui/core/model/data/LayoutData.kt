package com.bennyjon.aui.core.model.data

import kotlinx.serialization.Serializable

/** Data for the `divider` block. Renders a visual separator line. No configurable fields. */
@Serializable
data class DividerData(
    val placeholder: String? = null,
)
