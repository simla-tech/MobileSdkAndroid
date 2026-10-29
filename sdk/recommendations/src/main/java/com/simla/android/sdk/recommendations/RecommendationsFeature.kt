package com.simla.android.sdk.recommendations

import com.simla.android.sdk.CrmFeature
import com.simla.android.sdk.CrmFeatureContext
import com.simla.android.sdk.CrmFeatureKey
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.CrmSdkModulesBuilder
import com.simla.android.sdk.tracker.tracker

/** Product recommendations feature. */
internal object RecommendationsFeature : CrmFeature<Recommendations> {
    override val key = CrmFeatureKey<Recommendations>("recommendations")

    override fun install(context: CrmFeatureContext): Recommendations {
        val tracker = requireNotNull(context.tracker) {
            "Recommendations require the tracker feature"
        }
        return RecommendationsImpl(
            networkEngine = context.networkEngine,
            logger = context.logger,
            authorizedHeadersProvider = context.session.authorizedHeaders,
            customerIdentityProvider = context.session.currentCustomerIdentity,
            endpointResolver = context.session.resolveClientApiUrl,
            pageViewTracker = tracker.trackPageView,
            eventTracker = tracker.trackEvent,
        )
    }
}

fun CrmSdk.recommendations(): Recommendations {
    tracker()
    return moduleOrNull(RecommendationsFeature.key) ?: installModule(RecommendationsFeature)
}

/** Installs recommendations and their tracker dependency during [CrmSdk.init]. */
fun CrmSdkModulesBuilder.recommendations() {
    tracker()
    register(RecommendationsFeature)
}
