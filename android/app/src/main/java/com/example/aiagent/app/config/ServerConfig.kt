package com.example.aiagent.app.config

import android.content.Context
import android.content.SharedPreferences

/**
 * 服务器地址配置。
 *
 * 地址持久化在 SharedPreferences，APP 首次启动无地址时引导用户输入，
 * 后续可在设置页修改（隧道域名变化时无需重新打包 APK）。
 */
class ServerConfig(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 当前配置的服务器地址（未配置时为 null） */
    var serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)
        set(value) {
            prefs.edit().putString(KEY_SERVER_URL, normalize(value)).apply()
        }

    companion object {
        private const val PREFS_NAME = "app_config"
        private const val KEY_SERVER_URL = "server_url"

        /**
         * 规范化服务器地址：去除首尾空白与末尾斜杠，缺少协议前缀时补 https://。
         */
        fun normalize(url: String?): String? {
            val trimmed = url?.trim()?.trimEnd('/') ?: return null
            if (trimmed.isEmpty()) return null
            return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }
        }
    }
}
