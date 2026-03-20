package dev.ujhhgtg.wekit.loader.entry.lsp10x

import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

interface Lsp10xHookEntryHandler {
    fun onPackageLoaded(param: PackageLoadedParam)
}
