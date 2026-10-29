package com.simla.android.sdk.consultant.models.widget

import com.simla.android.sdk.consultant.models.connection.ConsultantDomainConfig
import com.simla.android.sdk.consultant.models.contactform.CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantWidgetConfig(
    val name: String? = null,
    val position: String? = null,
    @SerialName("bg_color")
    val backgroundTheme: String? = null,
    val inverse: Boolean = false,
    @SerialName("element_shape")
    val elementShape: String? = null,
    @SerialName("logo_type")
    val logoType: String? = null,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    @SerialName("logo_stretch")
    val logoStretch: Boolean = false,
    @SerialName("manager_display_type")
    val managerDisplayType: String? = null,
    @SerialName("manager_name")
    val managerName: String? = null,
    @SerialName("manager_logo")
    val managerLogo: String? = null,
    @SerialName("elements_color")
    val elementsColor: String? = null,
    @SerialName("intro_text")
    val introText: String? = null,
    @SerialName("use_offline_mode")
    val useOfflineMode: Boolean = false,
    @SerialName("offline_text")
    val offlineText: String? = null,
    @SerialName("offline_form")
    val offlineForm: ConsultantContactFormConfig = ConsultantContactFormConfig(),
    @SerialName("offline_form_result_text")
    val offlineFormResultText: String? = null,
    @SerialName("use_contact_form")
    val useContactForm: Boolean = false,
    @SerialName("contact_form_display")
    val contactFormDisplay: String = CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE,
    @SerialName("contact_form_text")
    val contactFormText: String? = null,
    @SerialName("contact_form")
    val contactForm: ConsultantContactFormConfig = ConsultantContactFormConfig(),
    @SerialName("personal_data_consent")
    val personalDataConsent: Boolean = false,
    @SerialName("personal_data_link")
    val personalDataLink: String? = null,
    @SerialName("use_sound_alerts")
    val useSoundAlerts: Boolean = false,
    @SerialName("use_unread_counter")
    val useUnreadCounter: Boolean = true,
    @SerialName("always_show_links_on_desktops")
    val alwaysShowLinksOnDesktops: Boolean = false,
    val links: List<ConsultantWidgetLink> = emptyList(),
    @SerialName("links_mode")
    val linksMode: String? = null,
    @SerialName("links_orientation")
    val linksOrientation: String? = null,
    @SerialName("domain_config")
    val domainConfig: ConsultantDomainConfig? = null,
)
