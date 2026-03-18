package moe.ouom.wekit.utils.crash

import android.content.Context
import android.os.Process
import dev.ujhhgtg.nameof.nameof
import moe.ouom.wekit.utils.crash.CrashInfoCollector.collectCrashInfo
import moe.ouom.wekit.utils.getThreadId
import moe.ouom.wekit.utils.logging.WeLogger

class JavaCrashHandler(context: Context) : Thread.UncaughtExceptionHandler {

    companion object {
        private val TAG = nameof(JavaCrashHandler::class)
    }

    private val context: Context = context.applicationContext
    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    /**
     * 获取崩溃日志管理器
     * 
     * @return 崩溃日志管理器
     */
    val crashLogsManager: CrashLogsManager = CrashLogsManager()
    private var isHandling = false

    /**
     * 安装崩溃拦截器
     */
    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
        WeLogger.i(TAG, "Java crash handler installed")
    }

    /**
     * 卸载崩溃拦截器
     */
    fun uninstall() {
        if (defaultHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
            WeLogger.i(TAG, "Java crash handler uninstalled")
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 防止递归调用
        if (isHandling) {
            WeLogger.e(
                TAG,
                "Recursive crash detected, delegating to default handler"
            )
            defaultHandler?.uncaughtException(thread, throwable)
            return
        }

        isHandling = true

        try {
            WeLogger.e(TAG, "========================================")
            WeLogger.e(TAG, "Uncaught exception detected!")
            WeLogger.e(
                TAG,
                "Thread: " + thread.name + " (ID: " + thread.getThreadId() + ")"
            )
            WeLogger.e(TAG, "Exception: " + throwable.javaClass.name)
            WeLogger.e(TAG, "Message: " + throwable.message)
            WeLogger.e(TAG, "========================================")

            // 收集崩溃信息
            val crashInfo = collectCrashInfo(context, throwable, "JAVA")

            // 保存崩溃日志（标记为Java崩溃）
            val logPath = crashLogsManager.saveCrashLog(crashInfo, true)
            if (logPath != null) {
                WeLogger.i(TAG, "Java crash log saved to: $logPath")
            } else {
                WeLogger.e(TAG, "Failed to save Java crash log")
            }

            WeLogger.e(TAG, "Crash details", throwable)
        } catch (e: Throwable) {
            WeLogger.e(TAG, "Error while handling crash", e)
        } finally {
            isHandling = false

            // 调用默认处理器，让应用正常崩溃
            if (defaultHandler != null) {
                WeLogger.i(TAG, "Delegating to default handler")
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                // 如果没有默认处理器，手动终止进程
                WeLogger.e(TAG, "No default handler, killing process")
                Process.killProcess(Process.myPid())
            }
        }
    }
}
