package com.bennyjon.aui.core.model.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data for the `file_content` block.
 *
 * Represents a single copyable artifact such as a markdown document, config
 * file, JSON payload, script, or source file. Unlike `text`, this preserves
 * the content as one exact file-like deliverable instead of decomposing it
 * into presentational blocks.
 */
@Serializable
data class FileContentData(
    /** Exact file body shown in a copyable monospace surface. */
    val content: String,
    /** Optional filename shown in the block header, e.g. `README.md`. */
    val filename: String? = null,
    /** Optional language / format hint such as `markdown`, `json`, or `swift`. */
    val language: String? = null,
    /** Optional human-readable title shown above filename metadata. */
    val title: String? = null,
    /** Optional short supporting description shown in the header. */
    @SerialName("description") val description: String? = null,
)
