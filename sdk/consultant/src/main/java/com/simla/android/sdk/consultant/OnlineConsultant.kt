package com.simla.android.sdk.consultant

import kotlinx.coroutines.flow.StateFlow

interface OnlineConsultant {
    val unreadCount: StateFlow<Int>
}
