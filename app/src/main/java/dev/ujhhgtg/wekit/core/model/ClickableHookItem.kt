package dev.ujhhgtg.wekit.core.model

import android.content.Context

abstract class ClickableHookItem : SwitchHookItem() {

    open val alwaysRun: Boolean = false

    open val noSwitchWidget = false

    abstract fun onClick(context: Context)
}
