package moe.ouom.wekit.hooks.api.net.abc

interface IWePacketInterceptor {
    fun onRequest(uri: String, cgiId: Int, reqBytes: ByteArray): ByteArray? = null
    fun onResponse(uri: String, cgiId: Int, respBytes: ByteArray): ByteArray? = null
}
