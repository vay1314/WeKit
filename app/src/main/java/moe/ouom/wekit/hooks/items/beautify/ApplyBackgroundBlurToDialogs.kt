package moe.ouom.wekit.hooks.items.beautify

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import dev.ujhhgtg.nameof.nameof
import moe.ouom.wekit.core.dsl.dexClass
import moe.ouom.wekit.core.model.BaseClickableFunctionHookItem
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.ui.content.AlertDialogContent
import moe.ouom.wekit.ui.utils.showComposeDialog
import moe.ouom.wekit.utils.log.WeLogger
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "界面美化/对话框窗口级背景模糊", desc = "为宿主的对话框添加窗口级模糊处理 (不对模块生效, 模块模糊总是启用)")
object ApplyBackgroundBlurToDialogs : BaseClickableFunctionHookItem(), IDexFind {

    private val TAG = nameof(ApplyBackgroundBlurToDialogs)

    private val classMmAlertDialog by dexClass()
    private val classMmProgressDialog by dexClass()
    private val classMmQuickDialog by dexClass()

    override fun entry(classLoader: ClassLoader) {
        listOf(classMmAlertDialog, classMmProgressDialog, classMmQuickDialog).forEach {
            it.clazz.asResolver()
                .firstMethod {
                    name = "onCreate"
                }
                .hookBefore { param ->
                    val dialog = param.thisObject as Dialog
                    applyBlur(dialog)
                }
        }
    }

    private fun applyBlur(dialog: Dialog) {
        dialog.window.apply {
            if (this == null) {
                WeLogger.w(TAG, "dialog.window is null, skipping")
                return@apply
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                attributes.blurBehindRadius = 20
            } else {
                WeLogger.w(TAG, "sdk < 31, not applying blur behind dialog")
            }
        }
    }

    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        classMmAlertDialog.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.ui.widget.dialog")
            matcher {
                usingEqStrings("MicroMsg.MMAlertDialog", "dialog dismiss error!")
            }
        }

        classMmProgressDialog.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.ui.widget.dialog")
            matcher {
                usingEqStrings($$"com/tencent/mm/ui/widget/dialog/MMProgressDialog$Builder", "show")
            }
        }

        classMmQuickDialog.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.ui.widget.dialog")
            matcher {
                addFieldForType("android.widget.TextView")
                addFieldForType("com.tencent.mm.ui.widget.imageview.WeImageView")
                addFieldForType("android.widget.ProgressBar")
            }
        }

        return descriptors
    }

    override fun onClick(context: Context) {
        showComposeDialog(context) { onDismiss ->
            AlertDialogContent(title = { Text("对话框窗口级背景模糊") },
                text = { Text("如果本对话框背景没有模糊, 说明系统 Android 版本过低 (SDK < 31) 或未在开发者选项中启用") },
                dismissButton = { Button(onDismiss) { Text("关闭") } })
        }
    }
}