package dev.ujhhgtg.wekit.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.wekit.constants.PackageNames

/**
 * 模块资源加载器助手
 * 用于简化 Xposed 模块加载自身资源（布局、图片、字符串）的流程
 */
@SuppressLint("StaticFieldLeak")
object ModuleRes {

    private val TAG = This.Class.simpleName

    lateinit var context: Context
    lateinit var resources: Resources

    @SuppressLint("DiscouragedApi")
    fun init(hostContext: Context) {
        if (::context.isInitialized) return

        runCatching {
            context = hostContext.createPackageContext(
                PackageNames.THIS,
                Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
            )
            resources = context.resources
        }.onFailure { WeLogger.e(TAG, "failed to initialize module resources", it) }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getDrawable(resId: Int): Drawable? {
        return resources.getDrawable(resId, null)
    }
}
