package com.bennyjon.aui.core.model.data

import kotlinx.serialization.Serializable

/** A single selectable option in a selection list. */
@Serializable
data class SelectionOption(
    /** Display label. */
    val label: String,
    /** Optional supporting description shown below the label. */
    val description: String? = null,
    /** Machine-readable value sent in feedback params when selected. */
    val value: String,
)

/** Data for the `radio_list` block. Allows picking exactly one option, each with an optional description. */
@Serializable
data class RadioListData(
    /** Key used to identify this input in feedback params. */
    val key: String,
    /** Optional label displayed above the list. */
    val label: String? = null,
    /** The selectable options. */
    val options: List<SelectionOption>,
    /** Pre-selected option value, if any. */
    val selected: String? = null,
)

/** Data for the `checkbox_list` block. Allows picking multiple options, each with an optional description. */
@Serializable
data class CheckboxListData(
    /** Key used to identify this input in feedback params. */
    val key: String,
    /** Optional label displayed above the list. */
    val label: String? = null,
    /** The selectable options. */
    val options: List<SelectionOption>,
    /** Pre-selected option values, if any. */
    val selected: List<String> = emptyList(),
)
