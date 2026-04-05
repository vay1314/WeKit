package dev.ujhhgtg.wekit.hooks.items.beautify

import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem

@HookItem(path = "界面美化/应用全局背景", description = "将微信背景替换为图片或视频 (没写完)")
object ApplyGlobalBackground : SwitchHookItem() {

    private val TAG = This.Class.simpleName

    override fun onEnable() {
//        LauncherUI::class.hookAfterOnCreate {
//            val activity = thisObject as Activity
//            val decorView = activity.window.decorView
//            decorView.background = Color.BLUE.toDrawable()
//        }

//        View::class.asResolver()
//            .firstMethod {
//                name = "setBackgroundDrawable"
//                parameters(Drawable::class)
//            }
//            .hookBefore {
//                args[0] = Color.TRANSPARENT.toDrawable()
//            }

//        "com.tencent.mm.plugin.setting.ui.setting.SettingsChattingBackgroundUI".toClass().asResolver()
//            .firstMethod { name = "onActivityResult" }.hookBefore {
//                val int1 = args[0] as Int
//                val int2 = args[1] as Int
//                WeLogger.d(TAG, "$int1, $int2, ${thisObject.asResolver().firstField { type = String::class }.get()!!}")
//            }
    }
}
