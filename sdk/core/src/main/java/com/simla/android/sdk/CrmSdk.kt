package com.simla.android.sdk

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.simla.android.sdk.logging.DefaultSdkLogger
import com.simla.android.sdk.network.SdkNetworkEngine
import com.simla.android.sdk.runtime.CrmConnectResponse
import com.simla.android.sdk.runtime.CrmRuntime
import com.simla.android.sdk.runtime.CrmRuntimeListener
import com.simla.android.sdk.session.SdkUserPayload
import com.simla.android.sdk.session.SessionPageContext
import com.simla.android.sdk.storage.KeystoreSdkSessionStorage

class CrmSdk private constructor(
    private val appContext: Context,
    private val config: CrmSdkConfig,
    private val networkEngine: SdkNetworkEngine,
    private val runtime: CrmRuntime,
) {
    private val installedModules = mutableMapOf<CrmFeatureKey<*>, Any>()
    private var trackerFeatureContext: CrmTrackerFeatureContext? = null

    /** Sets the shared customer identity. Pass null to switch to an anonymous customer. */
    suspend fun setCustomerIdentity(identity: CrmCustomerIdentity?) {
        runtime.setCustomerIdentity(identity)
    }

    @CrmSdkInternalApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T : Any> moduleOrNull(key: CrmFeatureKey<T>): T? = installedModules[key] as T?

    @CrmSdkInternalApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun <T : Any> requireModule(key: CrmFeatureKey<T>): T =
        moduleOrNull(key) ?: error("SDK module '${key.value}' is not installed")

    @CrmSdkInternalApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Synchronized
    fun <T : Any> installModule(module: CrmFeature<T>): T {
        moduleOrNull(module.key)?.let { return it }
        val context = featureContext()
        val instance = module.install(context)
        trackerFeatureContext = context.tracker
        installedModules[module.key] = instance
        return instance
    }

    private fun installModules(modules: List<CrmFeature<*>>) {
        modules.forEach { module ->
            check(module.key !in installedModules) {
                "SDK module '${module.key.value}' is already installed"
            }
            val context = featureContext()
            installedModules[module.key] = module.install(context)
            trackerFeatureContext = context.tracker
        }
    }

    private fun featureContext() = CrmFeatureContext(
        applicationContext = appContext,
        config = config,
        networkEngine = networkEngine,
        logger = DefaultSdkLogger(config.debug),
        runtime = runtime,
        session = CrmSessionFeatureContext(
            scope = runtime.scope,
            currentSession = runtime::currentSession,
            currentSiteCustomerId = runtime::currentSiteCustomerId,
            currentCustomerIdentity = runtime::currentCustomerIdentity,
            authorizedHeaders = {
                runtime.buildAuthorizedHttpHeaders(
                    includeSiteCustomerId = false,
                )
            },
            resolveClientApiUrl = runtime::resolveClientApiUrl,
            addLifecycleListener = { listener ->
                runtime.addListener(object : CrmRuntimeListener {
                    override suspend fun onConnected(
                        response: CrmConnectResponse,
                        pageContext: SessionPageContext,
                        user: SdkUserPayload,
                    ) = listener.onConnected(response, pageContext, user)

                    override suspend fun onDisconnected() = listener.onDisconnected()
                    override suspend fun onForeground() = listener.onForeground()
                    override suspend fun onBackground() = listener.onBackground()
                    override fun onClosed() = listener.onClosed()
                })
            },
        ),
        initialTracker = trackerFeatureContext,
    )

    companion object {
        @Volatile
        private var instance: CrmSdk? = null

        /** Initializes the process-wide SDK instance and starts its shared session. */
        @JvmStatic
        fun init(
            context: Context,
            config: CrmSdkConfig,
            networkEngine: SdkNetworkEngine,
            pageContext: SessionPageContext = SessionPageContext(),
        ) {
            initWithModules(context, config, pageContext, emptyList(), networkEngine)
        }

        /** Initializes the SDK and eagerly installs the modules selected in [configureModules]. */
        @JvmStatic
        fun init(
            context: Context,
            config: CrmSdkConfig,
            networkEngine: SdkNetworkEngine,
            pageContext: SessionPageContext = SessionPageContext(),
            configureModules: CrmSdkModulesBuilder.() -> Unit,
        ) {
            val modules = CrmSdkModulesBuilder()
                .apply(configureModules)
                .build()
            initWithModules(context, config, pageContext, modules, networkEngine)
        }

        /** Returns the process-wide SDK instance. */
        @JvmStatic
        fun getInstance(): CrmSdk = instance
            ?: error("CrmSdk.init must be called before CrmSdk.getInstance")

        @CrmSdkInternalApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun initWithModules(
            context: Context,
            config: CrmSdkConfig,
            pageContext: SessionPageContext,
            modules: List<CrmFeature<*>>,
            networkEngine: SdkNetworkEngine,
        ) {
            synchronized(this) {
                check(instance == null) { "CrmSdk is already initialized" }
                val applicationContext = context.applicationContext
                val logger = DefaultSdkLogger(config.debug)
                val runtime = CrmRuntime(
                    config = config,
                    networkEngine = networkEngine,
                    logger = logger,
                    sessionStorage = KeystoreSdkSessionStorage(
                        context = applicationContext,
                        namespace = config.siteToken,
                    ),
                )
                val sdk = CrmSdk(
                    appContext = applicationContext,
                    config = config,
                    networkEngine = networkEngine,
                    runtime = runtime,
                ).also { it.installModules(modules) }
                instance = sdk
                runtime.initialize(pageContext)
                sdk.bindToProcessLifecycle()
            }
        }
    }

    private fun bindToProcessLifecycle() {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        runtime.setAppInForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                runtime.setAppInForeground(true)
            }

            override fun onStop(owner: LifecycleOwner) {
                runtime.setAppInForeground(false)
            }
        })
    }
}
