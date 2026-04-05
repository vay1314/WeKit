package dev.ujhhgtg.wekit.hooks.items.chat

import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Modifier

@HookItem(path = "聊天/移除通话时聊天限制", description = "绕过「正在通话, 可稍后再试」提示 (没写完)")
object RemoveLimitsDuringCalls : SwitchHookItem(), IResolvesDex {

    override fun onEnable() {
        listOf(methodIsDuringCall, methodIsDuringCallAndDisplayToast, methodIsDuringCallAndDisplayToast2).forEach {
            it.hookBefore {
                result = false
            }
        }
    }

    private val methodIsDuringCall by dexMethod()
    private val methodIsDuringCallAndDisplayToast by dexMethod()
    private val methodIsDuringCallAndDisplayToast2 by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge) {
        methodIsDuringCall.find(dexKit) {
            matcher {
                declaredClass {
                    modifiers(Modifier.ABSTRACT)
                }

                modifiers(Modifier.STATIC)
                paramCount = 0
                returnType = "boolean"

                addInvoke {
                    declaredClass = "com.tencent.mm.autogen.events.MultiTalkActionEvent"
                }
            }
        }

        methodIsDuringCallAndDisplayToast.find(dexKit) {
            matcher {
                declaredClass(methodIsDuringCall.method.declaringClass)
                usingEqStrings("MicroMsg.DeviceOccupy", "isMultiTalking")
                paramCount = 1
            }
        }

        methodIsDuringCallAndDisplayToast2.find(dexKit) {
            matcher {
                declaredClass(methodIsDuringCall.method.declaringClass)
                usingEqStrings("MicroMsg.DeviceOccupy", "isMultiTalking")
                paramCount = 2
            }
        }
    }
}
