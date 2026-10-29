package com.simla.android.sdk.session

data class SdkBehaviour(
    val isOffline: Boolean = false,
    val widget: Boolean? = null,
    val tracker: Boolean? = null,
    val gpc: Boolean? = null,
    val consentForm: Boolean? = null,
)
