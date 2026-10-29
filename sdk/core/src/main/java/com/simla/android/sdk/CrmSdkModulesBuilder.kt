package com.simla.android.sdk

import androidx.annotation.RestrictTo

/** Selects SDK modules that must be installed during [CrmSdk.init]. */
class CrmSdkModulesBuilder internal constructor() {
    private val modules = linkedMapOf<CrmFeatureKey<*>, CrmFeature<*>>()

    /** Registers a module contributed by an SDK artifact. */
    @CrmSdkInternalApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun register(module: CrmFeature<*>) {
        val existing = modules[module.key]
        check(existing == null || existing === module) {
            "SDK module '${module.key.value}' is already configured"
        }
        modules.putIfAbsent(module.key, module)
    }

    internal fun build(): List<CrmFeature<*>> = modules.values.toList()
}
