package dev.ujhhgtg.wekit.utils.android

import android.os.Handler
import android.os.Looper

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

fun runOnUiThread(action: () -> Unit) {
    mainHandler.post {
        action()
    }
}
