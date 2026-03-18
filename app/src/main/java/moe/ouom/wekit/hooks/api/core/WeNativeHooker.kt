package moe.ouom.wekit.hooks.api.core

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import dev.ujhhgtg.nameof.nameof
import moe.ouom.wekit.core.model.ClickableHookItem
import moe.ouom.wekit.hooks.utils.annotation.HookItem
import moe.ouom.wekit.utils.logging.WeLogger
import java.lang.reflect.Method

@HookItem(path = "调试/原生库钩子", desc = "提供 hook 宿主原生库的能力 (没写完)")
object WeNativeHooker : ClickableHookItem() {

    private val TAG = nameof(WeNativeHooker)

    private external fun hookNativeFunction(targetPtr: Long, replacePtr: Long)
    private external fun getSymbolOffset(soPath: String, symbol: String): Long
    private external fun getArtQuickCode(method: Method, entryPointOffset: Int): Long
    private external fun hookTestTarget(): Int

    override fun onClick(context: Context) {
        val entryPointOffset = when {
            Build.VERSION.SDK_INT >= 30 -> 0x30
            else -> 0x28
        }

        WeLogger.d(TAG, "getSymbolOffset 1: ${getSymbolOffset("libWCDB.so", "JNI_OnLoad")}")
        WeLogger.d(TAG, "getSymbolOffset 2: ${getSymbolOffset("libweapp_core.so", "JNI_OnLoad")}")
        WeLogger.d(TAG, "getSymbolOffset 3: ${getSymbolOffset("libwechataudiosilk.so", "Java_com_tencent_mm_audio_mix_jni_SilkResampleJni_initResample")}")

        val target = getSymbolOffset("libWCDB.so", "JNI_OnLoad")
        WeLogger.d(TAG, "getSymbolOffset 4: $target")
        if (target == 0L) {
            WeLogger.e(TAG, "hookTest: target symbol not found")
            return
        }

        val replacementMethod = WeNativeHooker::class.java
            .getDeclaredMethod("hookTestReplacement")
            .also { it.isAccessible = true }

        val replace = getArtQuickCode(replacementMethod, entryPointOffset)
        if (replace == 0L) {
            WeLogger.e(TAG, "hookTest: could not get replacement entry point")
            return
        }

        WeLogger.d(TAG, "hookTest: target=0x${target.toString(16)} replace=0x${replace.toString(16)}")
        hookNativeFunction(target, replace)

        // Verify
        val result = hookTestTarget()
        WeLogger.d(TAG, "hookTest: result=$result (expected 99 if hooked, 42 if not)")
    }

    @Keep
    @JvmStatic
    fun hookTestReplacement(): Int {
        WeLogger.d(TAG, "hookTestReplacement: hook is working!")
        return 99
    }
}
