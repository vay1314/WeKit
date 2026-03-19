package moe.ouom.wekit.hooks.items.chat

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.condition.type.Modifiers
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.nameof.nameof
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.ouom.wekit.core.dsl.dexClass
import moe.ouom.wekit.core.model.SwitchHookItem
import moe.ouom.wekit.dexkit.abc.IResolvesDex
import moe.ouom.wekit.hooks.api.core.model.MessageType
import moe.ouom.wekit.hooks.api.ui.WeChatMessageContextMenuApi
import moe.ouom.wekit.hooks.utils.annotation.HookItem
import moe.ouom.wekit.utils.KnownPaths
import moe.ouom.wekit.utils.ModuleRes
import moe.ouom.wekit.utils.ToastUtils
import moe.ouom.wekit.utils.logging.WeLogger
import org.luckypray.dexkit.DexKitBridge
import kotlin.io.path.div
import kotlin.io.path.outputStream

@HookItem(path = "聊天/贴纸保存到本地", desc = "在贴纸消息菜单添加保存按钮, 允许将图片保存到本地")
object SaveStickersToLocalStorage : SwitchHookItem(), IResolvesDex,
    WeChatMessageContextMenuApi.IMenuItemsProvider {

    private val TAG = nameof(SaveStickersToLocalStorage)

    private val classEmojiFileEncryptMgr by dexClass()

    override fun onEnable() {
        WeChatMessageContextMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeChatMessageContextMenuApi.removeProvider(this)
    }

    override fun resolveDex(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        classEmojiFileEncryptMgr.find(dexKit, descriptors) {
            matcher {
                methods {
                    add {
                        usingEqStrings(
                            "MicroMsg.emoji.EmojiFileEncryptMgr",
                            "decode emoji file failed. path is no exist :%s "
                        )
                    }
                }
            }
        }

        return descriptors
    }

    override fun getMenuItems(): List<WeChatMessageContextMenuApi.MenuItem> {
        return listOf(
            @Suppress("UNCHECKED_CAST")
            WeChatMessageContextMenuApi.MenuItem(
                777001,
                "存本地",
                lazy { ModuleRes.getDrawable("download_24px") },
                { msgInfo -> msgInfo.isType(MessageType.STICKER) }
            ) { _, _, msgInfo ->
                val md5 = msgInfo.imagePath
                val emojiInfo = StickersSync.getEmojiInfoByMd5(md5)
                val emojiFileEncryptMgr = classEmojiFileEncryptMgr.asResolver()
                    .firstMethod {
                        modifiers(Modifiers.STATIC)
                        parameterCount = 0
                    }
                    .invoke()!!
                var bytes = emojiFileEncryptMgr.asResolver()
                    .firstMethod {
                        parameters("com.tencent.mm.api.IEmojiInfo")
                        returnType = ByteArray::class
                    }
                    .invoke(emojiInfo) as ByteArray
                bytes = "com.tencent.mm.plugin.gif.MMWXGFJNI".toClass().asResolver()
                    .firstMethod {
                        name = "nativeWxamToGif"
                    }
                    .invoke(bytes) as ByteArray

                val fileName = "sticker_${System.currentTimeMillis()}.gif"
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        (KnownPaths.downloads / fileName).outputStream().use { out ->
                            out.write(bytes)
                        }
                    }.onSuccess {
                        withContext(Dispatchers.Main) {
                            ToastUtils.showToast("已将贴纸保存到 /sdcard/Download/WeKit/$fileName")
                        }
                    }.onFailure { e ->
                        WeLogger.e(TAG, "failed to save sticker $fileName", e)
                        withContext(Dispatchers.Main) {
                            ToastUtils.showToast("贴纸保存失败! 查看日志以了解错误详情")
                        }
                    }
                }
            }
        )
    }
}
