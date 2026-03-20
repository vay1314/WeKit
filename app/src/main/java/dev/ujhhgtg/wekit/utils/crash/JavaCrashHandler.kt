package dev.ujhhgtg.wekit.utils.crash

import android.content.Context
import android.os.Process
import dev.ujhhgtg.nameof.nameof
import dev.ujhhgtg.wekit.utils.crash.CrashInfoCollector.collectCrashInfo
import dev.ujhhgtg.wekit.utils.polyfills.getThreadId
import dev.ujhhgtg.wekit.utils.logging.WeLogger

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
    }

    /**
     * 卸载崩溃拦截器
     */
    fun uninstall() {
        if (defaultHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 防止递归调用
        if (isHandling) {
            WeLogger.e(
                TAG,
                "recursive crash detected, delegating to default handler"
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
                WeLogger.i(TAG, "java crash log saved to: $logPath")
            } else {
                WeLogger.e(TAG, "failed to save Java crash log")
            }

            WeLogger.e(TAG, "crash details", throwable)
        } catch (e: Throwable) {
            WeLogger.e(TAG, "error while handling crash", e)
        } finally {
            isHandling = false

            // 调用默认处理器，让应用正常崩溃
            if (defaultHandler != null) {
                WeLogger.i(TAG, "delegating to default handler")
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                // 如果没有默认处理器，手动终止进程
                WeLogger.e(TAG, "no default handler, killing process")
                Process.killProcess(Process.myPid())
            }
        }
    }
}
