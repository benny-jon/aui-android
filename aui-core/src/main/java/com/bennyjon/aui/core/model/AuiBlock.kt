package com.bennyjon.aui.core.model

import com.bennyjon.aui.core.model.data.BadgeSuccessData
import com.bennyjon.aui.core.model.data.ButtonPrimaryData
import com.bennyjon.aui.core.model.data.ButtonSecondaryData
import com.bennyjon.aui.core.model.data.CaptionData
import com.bennyjon.aui.core.model.data.CheckboxListData
import com.bennyjon.aui.core.model.data.ChipSelectMultiData
import com.bennyjon.aui.core.model.data.ChipSelectSingleData
import com.bennyjon.aui.core.model.data.DividerData
import com.bennyjon.aui.core.model.data.HeadingData
import com.bennyjon.aui.core.model.data.InputRatingStarsData
import com.bennyjon.aui.core.model.data.InputSliderData
import com.bennyjon.aui.core.model.data.InputTextSingleData
import com.bennyjon.aui.core.model.data.ProgressBarData
import com.bennyjon.aui.core.model.data.QuickRepliesData
import com.bennyjon.aui.core.model.data.RadioListData
import com.bennyjon.aui.core.model.data.StatusBannerSuccessData
import com.bennyjon.aui.core.model.data.StepperHorizontalData
import com.bennyjon.aui.core.model.data.TextData
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A single renderable unit in an [AuiResponse].
 *
 * Each subclass maps 1:1 to a component type in the AUI catalog. Use an exhaustive `when`
 * expression — never reflection — when dispatching on block type.
 *
 * Unknown `type` values in JSON produce [Unknown] and are never thrown as errors.
 */
@Serializable(with = AuiBlockSerializer::class)
sealed class AuiBlock {
    /** Optional feedback triggered when the user interacts with this block. */
    abstract val feedback: AuiFeedback?

    // ── Display ──────────────────────────────────────────────────────────────

    /** Plain text. Supports basic markdown. */
    @Serializable
    data class Text(
        val data: TextData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Bold section heading. */
    @Serializable
    data class Heading(
        val data: HeadingData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Small muted text for metadata or footnotes. */
    @Serializable
    data class Caption(
        val data: CaptionData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    // ── Input ────────────────────────────────────────────────────────────────

    /** Single-choice chip group. */
    @Serializable
    data class ChipSelectSingle(
        val data: ChipSelectSingleData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Multi-choice chip group. */
    @Serializable
    data class ChipSelectMulti(
        val data: ChipSelectMultiData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Filled primary CTA button. */
    @Serializable
    data class ButtonPrimary(
        val data: ButtonPrimaryData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Outlined secondary action button. */
    @Serializable
    data class ButtonSecondary(
        val data: ButtonSecondaryData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Horizontal row of tappable suggestion chips; each option triggers its own feedback. */
    @Serializable
    data class QuickReplies(
        val data: QuickRepliesData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** 1–5 star rating input. */
    @Serializable
    data class InputRatingStars(
        val data: InputRatingStarsData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Single-line text input with optional submit action. */
    @Serializable
    data class InputTextSingle(
        val data: InputTextSingleData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Range slider for scale questions. */
    @Serializable
    data class InputSlider(
        val data: InputSliderData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Vertical single-select list with radio buttons and optional descriptions. */
    @Serializable
    data class RadioList(
        val data: RadioListData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Vertical multi-select list with checkboxes and optional descriptions. */
    @Serializable
    data class CheckboxList(
        val data: CheckboxListData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    // ── Layout ───────────────────────────────────────────────────────────────

    /** Visual separator line. */
    @Serializable
    data class Divider(
        val data: DividerData = DividerData(),
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    // ── Progress ─────────────────────────────────────────────────────────────

    /** Horizontal step progress indicator. */
    @Serializable
    data class StepperHorizontal(
        val data: StepperHorizontalData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Linear progress bar with a label. */
    @Serializable
    data class ProgressBar(
        val data: ProgressBarData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    // ── Status ───────────────────────────────────────────────────────────────

    /** Small success-colored pill. */
    @Serializable
    data class BadgeSuccess(
        val data: BadgeSuccessData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    /** Full-width success confirmation banner. */
    @Serializable
    data class StatusBannerSuccess(
        val data: StatusBannerSuccessData,
        override val feedback: AuiFeedback? = null,
    ) : AuiBlock()

    // ── Fallback ─────────────────────────────────────────────────────────────

    /**
     * Fallback for any unrecognized `type` value.
     *
     * The renderer checks the [AuiPluginRegistry][com.bennyjon.aui.core.plugin.AuiPluginRegistry]
     * for a component plugin matching [type] before skipping. [rawData] preserves the block's
     * `data` field as a raw [JsonElement] so plugin components can deserialize it.
     */
    @Serializable
    data class Unknown(
        val type: String = "unknown",
        override val feedback: AuiFeedback? = null,
        @SerialName("data") val rawData: JsonElement? = null,
    ) : AuiBlock()
}

/**
 * Custom polymorphic serializer for [AuiBlock].
 *
 * Reads the `type` field from the JSON object to select the correct deserializer.
 * Any unrecognized type falls back to [AuiBlock.Unknown].
 */
internal object AuiBlockSerializer : JsonContentPolymorphicSerializer<AuiBlock>(AuiBlock::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AuiBlock> {
        val type = element.jsonObject["type"]?.jsonPrimitive?.content
        return when (type) {
            "text" -> AuiBlock.Text.serializer()
            "heading" -> AuiBlock.Heading.serializer()
            "caption" -> AuiBlock.Caption.serializer()
            "chip_select_single" -> AuiBlock.ChipSelectSingle.serializer()
            "chip_select_multi" -> AuiBlock.ChipSelectMulti.serializer()
            "button_primary" -> AuiBlock.ButtonPrimary.serializer()
            "button_secondary" -> AuiBlock.ButtonSecondary.serializer()
            "quick_replies" -> AuiBlock.QuickReplies.serializer()
            "input_rating_stars" -> AuiBlock.InputRatingStars.serializer()
            "input_text_single" -> AuiBlock.InputTextSingle.serializer()
            "input_slider" -> AuiBlock.InputSlider.serializer()
            "radio_list" -> AuiBlock.RadioList.serializer()
            "checkbox_list" -> AuiBlock.CheckboxList.serializer()
            "divider" -> AuiBlock.Divider.serializer()
            "stepper_horizontal" -> AuiBlock.StepperHorizontal.serializer()
            "progress_bar" -> AuiBlock.ProgressBar.serializer()
            "badge_success" -> AuiBlock.BadgeSuccess.serializer()
            "status_banner_success" -> AuiBlock.StatusBannerSuccess.serializer()
            else -> AuiBlock.Unknown.serializer()
        }
    }
}
