package dev.ujhhgtg.wekit.hooks.items.system

import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "系统与隐私/禁止应用检测 Xposed", desc = "防止应用检测 Xposed 框架是否存在")
object DisableXposedDetection : SwitchHookItem(), IResolvesDex {

    private val methodCheckStackTraceElements by dexMethod()

    override fun onEnable() {
        if (methodCheckStackTraceElements.isPlaceholder) return

        methodCheckStackTraceElements.hookBefore { param ->
            param.result = false
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodCheckStackTraceElements.find(dexKit, allowFailure = true) {
            searchPackages("com.tencent.mm.app")
            matcher {
                usingEqStrings(
                    "de.robv.android.xposed.XposedBridge",
                    "com.zte.heartyservice.SCC.FrameworkBridge"
                )
            }
        }
    }
}
