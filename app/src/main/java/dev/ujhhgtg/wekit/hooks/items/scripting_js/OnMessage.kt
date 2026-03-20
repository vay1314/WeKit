package dev.ujhhgtg.wekit.hooks.items.scripting_js

import dev.ujhhgtg.wekit.core.model.SwitchHookItem
import dev.ujhhgtg.wekit.hooks.utils.annotation.HookItem

@HookItem(path = "脚本/触发器：收到消息", desc = "收到消息时是否执行 onMessage()")
object OnMessage : SwitchHookItem()
