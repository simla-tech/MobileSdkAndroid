package com.simla.android.sdk

import androidx.annotation.RestrictTo
import java.util.concurrent.atomic.AtomicReference

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CrmUtmAttributionStorage {
    private val activeAttribution = AtomicReference<CrmUtmAttribution?>()

    fun save(attribution: CrmUtmAttribution) {
        activeAttribution.set(attribution.takeUnless(CrmUtmAttribution::isEmpty))
    }

    fun load(): CrmUtmAttribution? = activeAttribution.get()

    internal fun clear() = activeAttribution.set(null)
}
