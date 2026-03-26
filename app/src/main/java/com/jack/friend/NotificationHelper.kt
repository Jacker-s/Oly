package com.jack.friend

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import java.net.HttpURLConnection
import java.net.URL

object NotificationHelper {
    const val CHANNEL_ID = "MESSAGES_CHANNEL_V24"
    const val GROUP_KEY = "com.jack.friend.MESSAGES_GROUP"

    fun showMessageNotification(
        context: Context,
        message: Message,
        myUsername: String,
        senderName: String,
        senderPhotoUrl: String?
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                        description = context.getString(R.string.notification_channel_description)
                        enableVibration(true)
                    }
                )
            }
        }

        // Action: Reply
        val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
            putExtra("chatId", message.senderId)
            putExtra("senderName", senderName)
        }
        val replyPI = PendingIntent.getBroadcast(context, message.senderId.hashCode() + 1, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val remoteInput = RemoteInput.Builder(FriendMessagingService.KEY_TEXT_REPLY).setLabel(context.getString(R.string.notification_reply_label)).build()
        val replyAction = NotificationCompat.Action.Builder(R.drawable.ic_reply, context.getString(R.string.notification_action_reply), replyPI).addRemoteInput(remoteInput).build()

        // Action: Open Chat
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("chatId", message.senderId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPI = PendingIntent.getActivity(context, message.senderId.hashCode(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Action: Mark as Read
        val readIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            putExtra("chatId", message.senderId)
        }
        val readPI = PendingIntent.getBroadcast(context, message.senderId.hashCode() + 2, readIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val readAction = NotificationCompat.Action.Builder(0, context.getString(R.string.notification_action_mark_read), readPI).build()

        Thread {
            val senderBitmap = downloadBitmap(senderPhotoUrl)
            
            val userPerson = Person.Builder().setName(senderName).setIcon(senderBitmap?.let { IconCompat.createWithBitmap(it) }).build()
            
            val msgText = when {
                message.audioUrl != null -> context.getString(R.string.attachment_audio)
                message.imageUrl != null -> context.getString(R.string.attachment_image)
                message.videoUrl != null -> context.getString(R.string.attachment_video)
                message.stickerUrl != null -> context.getString(R.string.attachment_sticker)
                else -> message.text
            }

            val style = NotificationCompat.MessagingStyle(Person.Builder().setName(myUsername).build()).addMessage(msgText, message.timestamp, userPerson).setGroupConversation(false)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(style)
                .setContentIntent(openPI)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setGroup(GROUP_KEY)
                .addAction(replyAction)
                .addAction(readAction)
                .setColor(0xFF007AFF.toInt())

            nm.notify(message.senderId.hashCode(), builder.build())
            showSummaryNotification(context, nm)
        }.start()
    }

    private fun showSummaryNotification(context: Context, nm: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setSubText(context.getString(R.string.notification_summary_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()
            nm.notify(999, summary)
        }
    }

    private fun downloadBitmap(url: String?): Bitmap? {
        if (url.isNullOrEmpty()) return null
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doInput = true
            connection.connect()
            val bytes = connection.inputStream.readBytes()
            var bitmap = BitmapOrientationUtils.decodeWithExif(bytes) ?: return null
            
            // Redimensionar para o ícone da notificação se necessário
            if (bitmap.width > 256 || bitmap.height > 256) {
                bitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun clearNotification(context: Context, chatId: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(chatId.hashCode())
    }
}
