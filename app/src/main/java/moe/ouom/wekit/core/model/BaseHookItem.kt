package moe.ouom.wekit.core.model

import com.highcapable.kavaref.resolver.ConstructorResolver
import com.highcapable.kavaref.resolver.MethodResolver
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import moe.ouom.wekit.constants.PreferenceKeys
import moe.ouom.wekit.core.dsl.DexMethodDelegate
import moe.ouom.wekit.hooks.utils.ExceptionFactory
import moe.ouom.wekit.preferences.WePrefs
import moe.ouom.wekit.utils.logging.WeLogger
import java.lang.reflect.Executable

abstract class BaseHookItem {

    var path: String = ""

    var description: String = ""

    var hasEnabled: Boolean = false
        private set

    val itemName: String
        get() {
            val index = path.lastIndexOf("/")
            return if (index == -1) path else path.substring(index + 1)
        }

    fun enable() {
        if (hasEnabled) return
        runCatching {
            hasEnabled = true
            onEnable()
        }.onFailure { e ->
            WeLogger.e("failed to enable item", e)
            ExceptionFactory.add(this, e)
        }
    }

    fun disable() {
        if (!hasEnabled) return
        runCatching {
            hasEnabled = false
            onDisable()
        }.onFailure { e -> WeLogger.e("failed to disable item", e) }
    }

    open fun onEnable() {}

    open fun onDisable() {}

    // --- hookBefore ---

    inline fun hookBefore(method: Executable, crossinline action: HookAction): XC_MethodHook.Unhook {
        return XposedBridge.hookMethod(
            method,
            object :
                XC_MethodHook(WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50)) {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    executeHookAction(param, action)
                }
            }
        )
    }

    @JvmName("hookBefore2")
    inline fun Executable.hookBefore(
        crossinline action: HookAction
    ): XC_MethodHook.Unhook {
        return hookBefore(this, action)
    }

    @JvmName("hookBefore3")
    fun MethodResolver<*>.hookBefore(action: HookAction): XC_MethodHook.Unhook {
        return hookBefore(this.self, action)
    }

    @JvmName("hookBefore4")
    inline fun ConstructorResolver<*>.hookBefore(
        crossinline action: HookAction
    ): XC_MethodHook.Unhook {
        return hookBefore(this.self, action)
    }

    // --- end hookBefore ---

    // --- hookAfter ---

    inline fun hookAfter(method: Executable, crossinline action: HookAction): XC_MethodHook.Unhook {
        return XposedBridge.hookMethod(
            method,
            object :
                XC_MethodHook(WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50)) {
                override fun afterHookedMethod(param: MethodHookParam) {
                    executeHookAction(param, action)
                }
            }
        )
    }

    @JvmName("hookAfter2")
    inline fun Executable.hookAfter(
        crossinline action: HookAction
    ): XC_MethodHook.Unhook {
        return hookAfter(this, action)
    }

    @JvmName("hookAfter3")
    inline fun MethodResolver<*>.hookAfter(
        crossinline action: HookAction
    ): XC_MethodHook.Unhook {
        return hookAfter(this.self, action)
    }

    @JvmName("hookAfter4")
    inline fun ConstructorResolver<*>.hookAfter(
        crossinline action: HookAction
    ): XC_MethodHook.Unhook {
        return hookAfter(this.self, action)
    }

    // --- end hookAfter ---

    // --- dex delegate ---

    inline fun DexMethodDelegate.hookBefore(
        crossinline action: HookAction
    ): XC_MethodHook.Unhook {
        return hookBefore(this.method, action)
    }

    inline fun DexMethodDelegate.hookAfter(
        crossinline action: HookAction
    ): XC_MethodHook.Unhook {
        return hookAfter(this.method, action)
    }

    // --- end dex delegate ---

    inline fun executeHookAction(param: XC_MethodHook.MethodHookParam, action: HookAction) {
        if (this is SwitchHookItem && !this.isEnabled) return
        if (!hasEnabled) return
        runCatching {
            action(param)
        }.onFailure { e -> ExceptionFactory.add(this, e) }
    }

    typealias HookAction = (param: XC_MethodHook.MethodHookParam) -> Unit
}
