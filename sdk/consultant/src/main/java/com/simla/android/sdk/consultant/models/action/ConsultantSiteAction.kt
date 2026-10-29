package com.simla.android.sdk.consultant.models.action

import com.simla.android.sdk.consultant.models.message.ConsultantMessageAttachments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ConsultantSiteAction(
    val id: JsonElement? = null,
    val type: String? = null,
    val priority: Int = 0,
    val header: String? = null,
    val image: String? = null,
    val messages: List<String> = emptyList(),
    val buttons: ConsultantMessageAttachments? = null,
    @SerialName("sec_on_site")
    val secOnSite: Int = 0,
    @SerialName("sec_on_page")
    val secOnPage: Int = 0,
    @SerialName("user_leaves_page")
    val userLeavesPage: Boolean = false,
    @SerialName("scroll_position")
    val scrollPosition: Int = 0,
    @SerialName("scroll_position_mode")
    val scrollPositionMode: String? = null,
    @SerialName("duration_after_previous_message")
    val durationAfterPreviousMessage: Int = 0,
    @SerialName("duration_after_previous_message_mode")
    val durationAfterPreviousMessageMode: String? = null,
) {
    val isImmediateChatAction: Boolean
        get() = secOnSite <= 0 &&
                secOnPage <= 0 &&
                !userLeavesPage &&
                (scrollPosition <= 0 || scrollPositionMode.isNullOrBlank()) &&
                (durationAfterPreviousMessage <= 0 ||
                        durationAfterPreviousMessageMode.isNullOrBlank())
}
