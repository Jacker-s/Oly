package com.jack.friend

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val callerName = intent.getStringExtra("callerName").orEmpty().ifBlank { "Contato" }
        val callerId = intent.getStringExtra("callerId").orEmpty()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "call_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Lembretes de chamada", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("chatId", callerId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            callerId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Lembrar de retornar")
            .setContentText(callerName)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        manager.notify(callerId.hashCode(), notification)
    }
}
