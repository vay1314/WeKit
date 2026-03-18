package moe.ouom.wekit.loader.entry.lsp100

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.XposedApiExact
import moe.ouom.wekit.constants.PackageNames
import moe.ouom.wekit.loader.entry.common.ModuleLoader
import moe.ouom.wekit.loader.entry.lsp100.Lsp100HookImpl.Companion.init
import moe.ouom.wekit.loader.entry.lsp10x.Lsp10xHookEntryHandler

@XposedApiExact(100)
class Lsp100HookEntry(private val self: XposedModule) : Lsp10xHookEntryHandler {

    init {
        init(self)
    }

    @XposedApiExact(100)
    override fun onPackageLoaded(param: PackageLoadedParam) {
        val packageName = param.getPackageName()
        if (packageName.startsWith(PackageNames.WECHAT)) {
            if (param.isFirstPackage()) {
                val ai = param.applicationInfo
                ModuleLoader.initialize(
                    ai.dataDir,
                    param.classLoader,
                    Lsp100HookImpl.INSTANCE,
                    Lsp100HookImpl.INSTANCE,
                    self.applicationInfo.sourceDir,
                    true
                )
            }
        }
    }
}
