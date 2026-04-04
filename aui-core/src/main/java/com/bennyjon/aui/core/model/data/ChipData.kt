package com.bennyjon.aui.core.model.data

import kotlinx.serialization.Serializable

/** A single selectable option in a chip group. */
@Serializable
data class ChipOption(
    /** Display label shown on the chip. */
    val label: String,
    /** Machine-readable value sent in feedback params when selected. */
    val value: String,
)

/** Data for the `chip_select_single` block. Allows picking exactly one option. */
@Serializable
data class ChipSelectSingleData(
    /** Key used to identify this input in feedback params. */
    val key: String,
    /** Optional label displayed above the chip group. */
    val label: String? = null,
    /** The selectable options. */
    val options: List<ChipOption>,
    /** Pre-selected option value, if any. */
    val selected: String? = null,
)

/** Data for the `chip_select_multi` block. Allows picking multiple options. */
@Serializable
data class ChipSelectMultiData(
    /** Key used to identify this input in feedback params. */
    val key: String,
    /** Optional label displayed above the chip group. */
    val label: String? = null,
    /** The selectable options. */
    val options: List<ChipOption>,
    /** Pre-selected option values, if any. */
    val selected: List<String> = emptyList(),
)
