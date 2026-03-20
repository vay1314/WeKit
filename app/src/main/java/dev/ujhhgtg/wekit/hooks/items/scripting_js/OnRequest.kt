package dev.ujhhgtg.wekit.hooks.items.scripting_js

import dev.ujhhgtg.wekit.core.model.SwitchHookItem
import dev.ujhhgtg.wekit.hooks.utils.annotation.HookItem

@HookItem(path = "脚本/触发器：发起请求", desc = "发起请求时是否执行 onRequest()")
object OnRequest : SwitchHookItem()
