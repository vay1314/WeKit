package dev.ujhhgtg.wekit.hooks.api.core

import android.app.Activity
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import dev.ujhhgtg.wekit.hooks.core.ApiHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import java.lang.ref.WeakReference

@HookItem(path = "API/当前活动跟踪服务", description = "跟踪当前处于屏幕上的活动")
object WeCurrentActivityApi : ApiHookItem() {

    @Volatile
    var activity: WeakReference<Activity>? = null
        private set

    override fun onEnable() {
        Activity::class.asResolver().apply {
            firstMethod { name = "onResume" }.hookBefore {
                activity = WeakReference(thisObject as Activity)
            }
        }
    }
}
