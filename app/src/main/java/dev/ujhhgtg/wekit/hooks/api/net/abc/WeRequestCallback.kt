package dev.ujhhgtg.wekit.hooks.api.net.abc

interface WeRequestCallback {
    fun onSuccess(json: String, bytes: ByteArray?)
    fun onFail(errType: Int, errCode: Int, errMsg: String)
}
