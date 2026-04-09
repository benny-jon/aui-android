package com.bennyjon.aui.core.model.data

/**
 * Shared contract for data classes that represent a user-input component.
 *
 * All built-in input data classes ([ChipSelectSingleData], [InputSliderData], etc.) implement
 * this interface, giving the feedback pipeline a uniform way to discover the input's registry
 * key and human-readable label without type-checking each subclass.
 *
 * @see com.bennyjon.aui.core.model.AuiInputBlock
 */
interface AuiInputData {
    /** Key used to identify this input's value in feedback params and the value registry. */
    val key: String

    /** Human-readable label for this input, used as the entry question in feedback summaries. */
    val label: String?
}
