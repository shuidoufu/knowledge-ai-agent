package com.example.aiagent.app

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.example.aiagent.app.bridge.DownloadHelper
import com.example.aiagent.app.config.ServerConfig
import com.example.aiagent.app.ui.SettingsActivity
import com.example.aiagent.app.web.AppWebView
import com.example.aiagent.app.web.WebPermissionHandler

/**
 * APP 入口：WebView 容器。
 *
 * 职责：加载远程 H5、首启地址引导、加载失败重试入口、下载完成通知、返回键处理。
 */
class MainActivity : Activity() {

    private lateinit var appWebView: AppWebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: View
    private lateinit var serverConfig: ServerConfig
    private lateinit var permissionHandler: WebPermissionHandler
    private var downloadReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverConfig = ServerConfig(this)
        permissionHandler = WebPermissionHandler(this)

        progressBar = findViewById(R.id.progress_bar)
        errorView = findViewById(R.id.error_view)
        errorView.findViewById<Button>(R.id.btn_retry_settings)
            .setOnClickListener { openSettings() }

        appWebView = AppWebView(
            this,
            permissionHandler,
            onPageStarted = {
                progressBar.visibility = View.VISIBLE
                errorView.visibility = View.GONE
            },
            onPageFinished = { progressBar.visibility = View.GONE },
            onProgress = { progressBar.progress = it },
            onLoadError = { errorView.visibility = View.VISIBLE }
        )
        val container = findViewById<FrameLayout>(R.id.web_container)
        container.addView(
            appWebView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        DownloadHelper.ensureChannel(this)
        registerDownloadReceiver()
        loadServer()
    }

    /**
     * 加载服务器地址：未配置则先进入设置页。
     */
    private fun loadServer() {
        val url = serverConfig.serverUrl
        if (url.isNullOrBlank()) {
            openSettings()
        } else {
            appWebView.loadUrl(url)
        }
    }

    /**
     * 打开服务器地址设置页。
     */
    private fun openSettings() {
        startActivityForResult(Intent(this, SettingsActivity::class.java), REQ_SETTINGS)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SETTINGS && resultCode == RESULT_OK) {
            loadServer()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHandler.onRequestPermissionsResult(requestCode, grantResults)
    }

    /**
     * 返回键：WebView 有历史则回退，否则退到桌面（不销毁，保持会话）。
     */
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (appWebView.canGoBack()) {
            appWebView.goBack()
        } else {
            moveTaskToBack(true)
        }
    }

    /**
     * 注册下载完成广播，下载结束后发通知（打开/分享）。
     */
    private fun registerDownloadReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id != -1L) {
                        DownloadHelper.showCompleteNotification(context, id)
                    }
                }
            }
        }
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadReceiver = receiver
    }

    override fun onDestroy() {
        downloadReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }

    companion object {
        private const val REQ_SETTINGS = 100
    }
}
