package dev.ujhhgtg.wekit.hooks.items.system

import android.view.WindowManager
import com.android.internal.policy.PhoneWindow
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem

@HookItem(path = "系统与隐私/禁止屏幕高亮度", description = "禁止应用将屏幕亮度设置得过高")
object DisableHighBrightness : SwitchHookItem() {

    override fun onEnable() {
        PhoneWindow::class.asResolver()
            .firstMethod {
                name = "setAttributes"
                parameters(WindowManager.LayoutParams::class)
            }
            .hookBefore {
                val lp = args[0] as WindowManager.LayoutParams
                if (lp.screenBrightness >= 0.5f) {
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
    }
}
