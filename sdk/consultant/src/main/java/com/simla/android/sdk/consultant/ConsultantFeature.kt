package com.simla.android.sdk.consultant

import com.simla.android.sdk.CrmFeature
import com.simla.android.sdk.CrmFeatureContext
import com.simla.android.sdk.CrmFeatureKey
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.CrmSdkModulesBuilder
import com.simla.android.sdk.consultant.controller.OnlineConsultantController
import com.simla.android.sdk.consultant.storage.ConsultantSharedPreferencesDownloadedFileRegistry

internal class ConsultantFeatureRuntime internal constructor(
    val onlineConsultant: OnlineConsultant,
    internal val controller: OnlineConsultantController,
)

/** Online consultant feature. */
internal object ConsultantFeature : CrmFeature<ConsultantFeatureRuntime> {
    override val key = CrmFeatureKey<ConsultantFeatureRuntime>("consultant")

    override fun install(context: CrmFeatureContext): ConsultantFeatureRuntime {
        val implementation = OnlineConsultantImpl(
            appContext = context.applicationContext,
            config = context.config,
            logger = context.logger,
            networkEngine = context.networkEngine,
            downloadedFileRegistry = ConsultantSharedPreferencesDownloadedFileRegistry(
                context.applicationContext,
            ),
            runtime = context.runtime,
        )
        return ConsultantFeatureRuntime(
            onlineConsultant = implementation,
            controller = implementation.controller,
        )
    }
}

fun CrmSdk.onlineConsultant(): OnlineConsultant = consultantFeature().onlineConsultant

/** Installs the online consultant while [CrmSdk.init] configures the SDK. */
fun CrmSdkModulesBuilder.onlineConsultant() {
    register(ConsultantFeature)
}

internal fun CrmSdk.consultantController(): OnlineConsultantController =
    consultantFeature().controller

private fun CrmSdk.consultantFeature(): ConsultantFeatureRuntime =
    moduleOrNull(ConsultantFeature.key) ?: installModule(ConsultantFeature)
