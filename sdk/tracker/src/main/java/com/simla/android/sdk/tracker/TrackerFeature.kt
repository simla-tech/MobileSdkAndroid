package com.simla.android.sdk.tracker

import com.simla.android.sdk.CrmFeature
import com.simla.android.sdk.CrmFeatureContext
import com.simla.android.sdk.CrmFeatureKey
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.CrmSdkModulesBuilder
import com.simla.android.sdk.CrmSessionLifecycleListener
import com.simla.android.sdk.CrmTrackerFeatureContext
import com.simla.android.sdk.CrmUtmAttributionStorage
import com.simla.android.sdk.runtime.CrmConnectResponse
import com.simla.android.sdk.session.SdkUserPayload
import com.simla.android.sdk.session.SessionPageContext
import com.simla.android.sdk.tracker.model.TrackerEvent
import com.simla.android.sdk.tracker.storage.SQLiteTrackerStorage

/** Analytics tracker feature. */
internal object TrackerFeature : CrmFeature<Tracker> {
    override val key = CrmFeatureKey<Tracker>("tracker")

    override fun install(context: CrmFeatureContext): Tracker {
        val config = object : TrackerRuntimeConfig {
            override val trackerBatchSize = 50
            override val trackerFlushIntervalMs = 15_000L
            override val trackerTimeSyncIntervalMs = 60 * 60 * 1000L
            override val trackerMaxQueueSize = 1_000
        }
        val tracker = CrmTracker.createManaged(
            config = config,
            logger = context.logger,
            networkEngine = context.networkEngine,
            trackerStorage = SQLiteTrackerStorage(context.applicationContext),
            scope = context.session.scope,
            sessionProvider = context.session.currentSession,
            authorizedHeadersProvider = context.session.authorizedHeaders,
            endpointResolver = context.session.resolveClientApiUrl,
            customerSystemIdProvider = {
                context.session.currentCustomerIdentity()?.crmCustomerId
            },
            siteCustomerIdProvider = context.session.currentSiteCustomerId,
            utmAttributionProvider = CrmUtmAttributionStorage::load,
        )
        context.session.addLifecycleListener(object : CrmSessionLifecycleListener {
            override suspend fun onConnected(
                response: CrmConnectResponse,
                pageContext: SessionPageContext,
                user: SdkUserPayload,
            ) {
                tracker.onHostConnected(
                    serverTs = response.ts,
                    trackerEnabled = response.behaviour?.tracker == true,
                    consentRequired = response.behaviour?.consentForm == true,
                    pageContext = pageContext,
                )
                tracker.flush()
            }

            override suspend fun onDisconnected() = tracker.onHostDisconnected()

            override fun onClosed() = tracker.close()
        })
        context.registerTracker(
            CrmTrackerFeatureContext(
                resolveSiteCustomerId = {
                    tracker.resolveCustomerIdentity().siteCustomerId
                },
                trackPageView = tracker::trackPageViewEvent,
                trackEvent = { type, data ->
                    tracker.enqueueEvent(TrackerEvent(type = type, data = data))
                },
            ),
        )
        return tracker
    }
}

fun CrmSdk.tracker(): Tracker = moduleOrNull(TrackerFeature.key) ?: installModule(TrackerFeature)

/** Installs the tracker while [CrmSdk.init] configures the SDK. */
fun CrmSdkModulesBuilder.tracker() {
    register(TrackerFeature)
}

fun CrmSdk.tracker(consentHandler: TrackerConsentHandler): Tracker =
    tracker().also { it.setConsentHandler(consentHandler) }
