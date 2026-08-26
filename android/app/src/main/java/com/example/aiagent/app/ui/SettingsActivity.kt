package com.example.aiagent.app.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.aiagent.app.R
import com.example.aiagent.app.config.ServerConfig

/**
 * 服务器地址设置页。
 *
 * 入口：首次启动引导、加载失败重试。保存后返回主界面并触发重新加载。
 */
class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val serverConfig = ServerConfig(this)
        val input = findViewById<EditText>(R.id.et_server_url)
        input.setText(serverConfig.serverUrl.orEmpty())

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val url = ServerConfig.normalize(input.text.toString())
            if (url == null) {
                Toast.makeText(this, R.string.settings_url_invalid, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            serverConfig.serverUrl = url
            setResult(RESULT_OK)
            finish()
        }
    }
}
