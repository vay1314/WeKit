package dev.ujhhgtg.wekit.ui.utils

object MainActivityLifecycleOwnerProvider {
    val lifecycleOwner by lazy { XposedLifecycleOwner().apply { onCreate(); onResume() } }
}
