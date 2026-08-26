package com.example.aiagent.app.web

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * WebView 权限请求处理。
 *
 * H5 录音（getUserMedia）触发 WebView onPermissionRequest 时，
 * 先申请 Android 运行时权限 RECORD_AUDIO，授权后放行 WebView 请求。
 */
class WebPermissionHandler(private val activity: Activity) {

    private var pendingRequest: PermissionRequest? = null

    /**
     * 处理 WebView 的 onPermissionRequest：仅放行音频采集，其余一律拒绝。
     */
    fun onPermissionRequest(request: PermissionRequest) {
        val resources = request.resources
        if (resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                request.grant(resources)
            } else {
                pendingRequest = request
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO
                )
            }
        } else {
            request.deny()
        }
    }

    /**
     * 系统运行时权限回调：授权成功则放行挂起的录音请求，失败则拒绝。
     */
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode != REQUEST_RECORD_AUDIO) return
        val request = pendingRequest ?: return
        pendingRequest = null
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            request.grant(request.resources)
        } else {
            request.deny()
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO = 1001
    }
}
