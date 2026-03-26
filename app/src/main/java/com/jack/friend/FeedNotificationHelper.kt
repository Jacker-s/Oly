package com.jack.friend

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import java.net.HttpURLConnection
import java.net.URL

object FeedNotificationHelper {

    private const val FEED_CHANNEL_ID = "FEED_INTERACTIONS_V1"
    private const val FEED_CHANNEL_NAME = "Postagens"

    fun showFeedInteractionNotification(
        context: Context,
        notification: FeedNotification
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Criar canal se necessário
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(FEED_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    FEED_CHANNEL_ID,
                    FEED_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Interações nas suas postagens"
                    enableLights(true)
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("openFeed", true)
            putExtra("postId", notification.postId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = notification.fromName.ifBlank { "Alguém" }
        val body = when (notification.type) {
            "LIKE" -> "curtiu sua postagem"
            "COMMENT" -> "comentou: \"${notification.postPreviewText}\""
            "REACTION" -> "reagiu ${notification.reactionEmoji} à sua postagem"
            else -> "interagiu com sua postagem"
        }

        val builder = NotificationCompat.Builder(context, FEED_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF007AFF.toInt())

        // Tentar baixar avatar do remetente numa thread separada
        Thread {
            val bitmap = downloadBitmap(notification.fromPhotoUrl)
            if (bitmap != null) builder.setLargeIcon(bitmap)
            nm.notify(notification.id.hashCode(), builder.build())
        }.start()
    }

    private fun downloadBitmap(url: String?): Bitmap? {
        if (url.isNullOrEmpty()) return null
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doInput = true
            conn.connect()
            BitmapFactory.decodeStream(conn.inputStream)
        } catch (e: Exception) {
            null
        }
    }
}
