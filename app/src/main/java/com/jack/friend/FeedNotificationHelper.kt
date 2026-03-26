package com.jack.friend

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import java.net.HttpURLConnection
import java.net.URL

object FeedNotificationHelper {

    private const val FEED_CHANNEL_ID = "FEED_INTERACTIONS_V1"
    private const val CHATS_CHANNEL_ID = "CHATS_V1"
    private const val PREFS_NAME = "friend_prefs"
    private const val KEY_MY_USERNAME = "cached_username"

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
                    context.getString(R.string.notification_channel_feed_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_channel_feed_description)
                    enableLights(true)
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }
            if (nm.getNotificationChannel(CHATS_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHATS_CHANNEL_ID,
                    context.getString(R.string.notification_channel_chats_name),
                    NotificationManager.IMPORTANCE_HIGH // Mensagens devem ser alta prioridade
                ).apply {
                    description = context.getString(R.string.notification_channel_chats_description)
                    enableLights(true)
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            if (notification.type == "CHAT_MSG") {
                putExtra("targetId", notification.fromId)
            } else {
                putExtra("openFeed", true)
                putExtra("postId", notification.postId)
            }
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
            "LIKE" -> context.getString(R.string.notification_feed_liked_post)
            "COMMENT" -> context.getString(R.string.notification_feed_commented_preview, notification.postPreviewText)
            "REACTION" -> context.getString(R.string.notification_feed_reacted_post, notification.reactionEmoji ?: "")
            "CHAT_MSG" -> notification.postPreviewText.ifBlank { context.getString(R.string.notification_feed_new_chat_message) }
            "MENTION" -> context.getString(R.string.notification_feed_mentioned_post)
            else -> context.getString(R.string.notification_feed_interacted_post)
        }

        val channelToUse = if (notification.type == "CHAT_MSG") CHATS_CHANNEL_ID else FEED_CHANNEL_ID

        val builder = NotificationCompat.Builder(context, channelToUse)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (notification.type == "CHAT_MSG") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF007AFF.toInt())

        if (notification.type == "CHAT_MSG" && notification.fromId.isNotBlank()) {
            val myUsername = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_MY_USERNAME, "")
                .orEmpty()

            val remoteInput = RemoteInput.Builder(FriendMessagingService.KEY_TEXT_REPLY)
                .setLabel(context.getString(R.string.notification_reply_label))
                .build()
            val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
                putExtra("chatId", notification.fromId)
                putExtra("senderName", myUsername)
            }
            val replyPI = PendingIntent.getBroadcast(
                context,
                notification.id.hashCode(),
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_send,
                    context.getString(R.string.notification_action_reply),
                    replyPI
                ).addRemoteInput(remoteInput).build()
            )

            val readIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
                putExtra("chatId", notification.fromId)
            }
            val readPI = PendingIntent.getBroadcast(
                context,
                notification.id.hashCode() + 1,
                readIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    0,
                    context.getString(R.string.notification_action_mark_read),
                    readPI
                ).build()
            )
        }

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
            val bytes = conn.inputStream.readBytes()
            BitmapOrientationUtils.decodeWithExif(bytes)
        } catch (e: Exception) {
            null
        }
    }
}
