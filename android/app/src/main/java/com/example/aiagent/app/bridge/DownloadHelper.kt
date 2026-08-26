package com.example.aiagent.app.bridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.aiagent.app.R
import java.io.File

/**
 * 文件下载与打开/分享。
 *
 * /api/files 链接经 DownloadManager 下载到系统 Downloads 目录（自动处理重名），
 * 完成后发通知：点击打开文件，带分享按钮。
 */
object DownloadHelper {

    private const val CHANNEL_ID = "downloads"
    private const val NOTIFICATION_ID = 2001

    /**
     * 创建下载通知渠道（Android 8+ 必须）。
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /**
     * 用系统 DownloadManager 下载指定 URL 到公共 Downloads 目录。
     */
    fun startDownload(context: Context, url: String) {
        val request = android.app.DownloadManager.Request(url.toUri())
            .setTitle(context.getString(R.string.download_title))
            .setDescription(url.substringAfterLast('/'))
            .setNotificationVisibility(
                android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
        val manager = context.getSystemService(android.app.DownloadManager::class.java)
        manager.enqueue(request)
    }

    /**
     * 下载完成后查询本地文件，发通知（点击打开 + 分享按钮）。
     */
    fun showCompleteNotification(context: Context, downloadId: Long) {
        val manager = context.getSystemService(android.app.DownloadManager::class.java)
        val fileUri = queryDownloadedFile(manager, downloadId) ?: return
        val file = File(fileUri.path ?: return)
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase())
            ?: "application/octet-stream"

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val sharePending = PendingIntent.getActivity(
            context, 1, Intent.createChooser(shareIntent, context.getString(R.string.share_title)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.download_done))
            .setContentText(context.getString(R.string.download_open_hint))
            .setContentIntent(openPending)
            .addAction(0, context.getString(R.string.share), sharePending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun queryDownloadedFile(
        manager: android.app.DownloadManager,
        downloadId: Long
    ): Uri? {
        val cursor: Cursor? = manager.query(
            android.app.DownloadManager.Query().setFilterById(downloadId)
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val status = it.getInt(
                    it.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS)
                )
                if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                    val uriString = it.getString(
                        it.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_LOCAL_URI)
                    )
                    return uriString.toUri()
                }
            }
        }
        return null
    }
}
