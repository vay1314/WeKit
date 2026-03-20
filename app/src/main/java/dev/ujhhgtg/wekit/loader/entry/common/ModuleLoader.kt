package dev.ujhhgtg.wekit.loader.entry.common

import android.util.Log
import dev.ujhhgtg.nameof.nameof
import dev.ujhhgtg.wekit.BuildConfig
import dev.ujhhgtg.wekit.loader.abc.IHookBridge
import dev.ujhhgtg.wekit.loader.abc.ILoaderService
import dev.ujhhgtg.wekit.loader.startup.UnifiedEntryPoint

object ModuleLoader {

    private val TAG = nameof(ModuleLoader)
    private var isInitialized = false

    @JvmStatic
    fun init(
        hostDataDir: String,
        hostClassLoader: ClassLoader,
        loaderService: ILoaderService,
        hookBridge: IHookBridge?,
        modulePath: String,
        allowDynamicLoad: Boolean
    ) {
        if (isInitialized) return
        isInitialized = true

        Log.i(BuildConfig.TAG, "$TAG: initializing from entry point ${loaderService.entryPointName}")
        UnifiedEntryPoint.entry(loaderService, hostClassLoader, modulePath)
    }
}
