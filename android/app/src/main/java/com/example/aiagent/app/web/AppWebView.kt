package com.example.aiagent.app.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.aiagent.app.bridge.DownloadHelper

/**
 * WebView 封装：统一 JS/安全配置与 URL 分发。
 *
 * URL 分发规则：
 *  - 本站 /api/files 链接 → 原生下载（DownloadManager）
 *  - 本站 /api/image-proxy 且 download=1 链接 → 原生下载（移动端长按保存图片）
 *  - 外链（其他域名 / 其他 scheme）→ 系统浏览器打开
 *  - 其余站内导航 → WebView 内正常加载
 *
 * 前端 linkify 生成的下载链接是 target="_blank"，WebView 走 onCreateWindow，
 * 因此新窗口与普通导航两处都做 URL 分发；<a download> 则走 DownloadListener。
 */
@SuppressLint("SetJavaScriptEnabled")
class AppWebView(
    context: Context,
    private val permissionHandler: WebPermissionHandler,
    private val onPageStarted: () -> Unit = {},
    private val onPageFinished: () -> Unit = {},
    private val onProgress: (Int) -> Unit = {},
    private val onLoadError: () -> Unit = {},
) : WebView(context) {

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                return dispatchUrl(uri)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                onPageStarted()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                onPageFinished()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    onLoadError()
                }
            }
        }
        // <a download>/Blob 触发的下载不走 shouldOverrideUrlLoading，必须用 DownloadListener
        setDownloadListener { url, _, _, _, _ ->
            handleDownloadUrl(url)
        }
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgress(newProgress)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.let { permissionHandler.onPermissionRequest(it) }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                // 新窗口不实际打开，仅取出目标 URL 做分发后丢弃
                val target = WebView(view?.context ?: context)
                target.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        dispatchUrl(uri)
                        return true
                    }
                }
                transport.webView = target
                resultMsg.sendToTarget()
                return true
            }
        }
    }

    /**
     * URL 分发：下载 / 系统浏览器 / 站内放行。
     */
    private fun dispatchUrl(uri: Uri): Boolean {
        return when {
            isSameSite(uri) -> {
                if (uri.path?.startsWith("/api/files") == true ||
                    uri.path?.startsWith("/api/image-proxy") == true && uri.query.orEmpty().contains("download=1")
                ) {
                    DownloadHelper.startDownload(context, uri.toString())
                    true
                } else {
                    false
                }
            }
            uri.scheme == "http" || uri.scheme == "https" || uri.scheme == "mailto" ||
                uri.scheme == "tel" || uri.scheme == "intent" -> openInBrowser(uri)
            else -> false
        }
    }

    /**
     * 处理 a[download] 下载回调：仅接管同站 /api 链接（相对路径拼当前 origin，含端口）。
     */
    private fun handleDownloadUrl(url: String) {
        val base = this.url ?: return
        val baseUri = Uri.parse(base)
        val origin = "${baseUri.scheme}://${baseUri.authority}"
        val absolute = if (url.startsWith("http")) url else origin + url
        val uri = Uri.parse(absolute)
        val sameSite = uri.host == baseUri.host
        if (sameSite && uri.path?.startsWith("/api/") == true) {
            DownloadHelper.startDownload(context, absolute)
        }
    }

    /**
     * 判断链接是否属于当前站点的域名（与 WebView 当前加载地址同 host）。
     */
    private fun isSameSite(uri: Uri): Boolean {
        val currentHost = url?.let { Uri.parse(it).host }
        return currentHost != null && uri.host == currentHost
    }

    /**
     * 用系统浏览器打开外部链接。
     */
    private fun openInBrowser(uri: Uri): Boolean {
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (e: Exception) {
            false
        }
    }
}
