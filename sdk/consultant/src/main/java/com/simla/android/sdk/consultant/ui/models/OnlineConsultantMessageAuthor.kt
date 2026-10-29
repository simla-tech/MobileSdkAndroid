package com.simla.android.sdk.consultant.ui.models

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class OnlineConsultantMessageAuthor {
    VISITOR,
    CONSULTANT,
    BOT,
}
