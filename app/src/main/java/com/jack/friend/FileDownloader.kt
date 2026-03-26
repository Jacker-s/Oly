package com.jack.friend

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import java.io.File

object FileDownloader {
    private fun targetFile(context: Context, filename: String): File {
        val safeName = filename.ifBlank { "arquivo" }
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        return File(dir, safeName)
    }

    fun isDownloaded(context: Context, filename: String): Boolean {
        return targetFile(context, filename).exists()
    }

    fun openFile(context: Context, filename: String) {
        val file = targetFile(context, filename)
        if (!file.exists()) {
            Toast.makeText(context, "Arquivo não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        val extension = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(file), mime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum app disponível para abrir o arquivo", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadFile(context: Context, url: String, filename: String) {
        if (url.isBlank()) {
            Toast.makeText(context, "Arquivo indisponível", Toast.LENGTH_SHORT).show()
            return
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename.ifBlank { "arquivo" })
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(targetFile(context, filename)))

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download iniciado", Toast.LENGTH_SHORT).show()
    }
}
