package moe.ouom.wekit.hooks.api.net.abc

import moe.ouom.wekit.hooks.api.net.model.SignResult
import org.json.JSONObject

interface ISigner {
    fun match(cgiId: Int): Boolean
    fun sign(loader: ClassLoader, json: JSONObject): SignResult
}