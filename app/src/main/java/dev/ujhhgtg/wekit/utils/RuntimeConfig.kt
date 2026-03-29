package dev.ujhhgtg.wekit.utils

import android.content.SharedPreferences

object RuntimeConfig {

    private lateinit var mmPrefs: SharedPreferences

    fun setMmPrefs(sharedPreferences: SharedPreferences) {
        mmPrefs = sharedPreferences
    }

    val loggedInWxId: String
        get() = mmPrefs.getString("login_weixin_username", "") ?: ""
}
