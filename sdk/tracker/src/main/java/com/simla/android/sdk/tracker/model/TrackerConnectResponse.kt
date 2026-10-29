package com.simla.android.sdk.tracker.model

import com.simla.android.sdk.session.SdkBehaviour
import com.simla.android.sdk.session.SdkConnectedSession
import com.simla.android.sdk.session.SdkConnectedUser
data class TrackerConnectResponse(
    val ts: String? = null,
    val user: SdkConnectedUser = SdkConnectedUser(),
    val session: SdkConnectedSession = SdkConnectedSession(),
    val behaviour: SdkBehaviour = SdkBehaviour(),
)
