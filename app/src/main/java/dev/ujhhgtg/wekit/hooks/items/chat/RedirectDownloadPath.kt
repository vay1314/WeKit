package dev.ujhhgtg.wekit.hooks.items.chat

import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.utils.KnownPaths
import dev.ujhhgtg.wekit.utils.createDirectoriesNoThrow
import org.luckypray.dexkit.DexKitBridge
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

@HookItem(path = "聊天/重定向文件下载路径", description = "将聊天文件下载到标准文件夹 Download")
object RedirectDownloadPath : SwitchHookItem(), IResolvesDex {

    override fun onEnable() {
        methodDownloadFile.hookBefore {
            val type = args[0] as? String? ?: return@hookBefore
            if (type != "attachment") return@hookBefore
            result = (KnownPaths.internalStorage / "Download" / "WeiXin")
                .createDirectoriesNoThrow().absolutePathString()
        }
    }

    private val methodDownloadFile by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge) {
        methodDownloadFile.find(dexKit) {
            searchPackages("com.tencent.mm.vfs")
            matcher {
                declaredClass {
                    usingEqStrings("VFS.VFSStrategy", "Found wrong moving file: ")
                }

                paramTypes(String::class.java)
                returnType(String::class.java)
            }
        }
    }
}
