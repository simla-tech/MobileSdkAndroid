package com.simla.android.sdk

import android.content.Context
import androidx.annotation.RestrictTo
import com.simla.android.sdk.logging.SdkLogger
import com.simla.android.sdk.network.SdkNetworkEngine
import com.simla.android.sdk.runtime.CrmConnectResponse
import com.simla.android.sdk.runtime.CrmRuntime
import com.simla.android.sdk.session.SdkSession
import com.simla.android.sdk.session.SdkUserPayload
import com.simla.android.sdk.session.SessionPageContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObject

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CrmFeatureContext internal constructor(
    val applicationContext: Context,
    val config: CrmSdkConfig,
    val networkEngine: SdkNetworkEngine,
    val logger: SdkLogger,
    val runtime: CrmRuntime,
    val session: CrmSessionFeatureContext,
    initialTracker: CrmTrackerFeatureContext?,
) {
    var tracker: CrmTrackerFeatureContext? = initialTracker
        private set

    fun registerTracker(feature: CrmTrackerFeatureContext) {
        check(tracker == null) { "Tracker feature is already installed" }
        tracker = feature
    }
}

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CrmSessionFeatureContext internal constructor(
    val scope: CoroutineScope,
    val currentSession: suspend () -> SdkSession?,
    val currentSiteCustomerId: () -> String?,
    val currentCustomerIdentity: () -> CrmCustomerIdentity?,
    val authorizedHeaders: suspend () -> Map<String, String>,
    val resolveClientApiUrl: (String) -> String,
    val addLifecycleListener: (CrmSessionLifecycleListener) -> Unit,
)

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CrmSessionLifecycleListener {
    suspend fun onConnected(
        response: CrmConnectResponse,
        pageContext: SessionPageContext,
        user: SdkUserPayload,
    ) = Unit

    suspend fun onDisconnected() = Unit
    suspend fun onForeground() = Unit
    suspend fun onBackground() = Unit
    fun onClosed() = Unit
}

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CrmTrackerFeatureContext(
    val resolveSiteCustomerId: () -> String?,
    val trackPageView: suspend (String, String?, JsonObject) -> Unit,
    val trackEvent: suspend (String, JsonObject) -> Unit,
)
