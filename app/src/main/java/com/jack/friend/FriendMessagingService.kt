package com.jack.friend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URL

class FriendMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "MESSAGES_CHANNEL_V24"
        private const val CALL_CHANNEL_ID = "CALL_CHANNEL_V24"
        private const val TAG = "FriendMessagingService"
        const val KEY_TEXT_REPLY = "key_text_reply"
        private const val PREFS_NAME = "friend_prefs"
        private const val KEY_MY_USERNAME = "cached_username"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        createChannels()

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val messageJson = data["message"]
            val feedNotifJson = data["feedNotification"]

            // Notificações do Feed (reações, comentários, curtidas)
            if (data["type"] == "FEED_NOTIFICATION" && feedNotifJson != null) {
                try {
                    val notif = Gson().fromJson(feedNotifJson, FeedNotification::class.java)
                    FeedNotificationHelper.showFeedInteractionNotification(this, notif)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing feed notification: ${e.message}")
                }
                return
            }

            if (messageJson != null) {
                try {
                    val message = Gson().fromJson(messageJson, Message::class.java)

                    // IGNORAR ANÚNCIOS NAS NOTIFICAÇÕES E RESUMOS
                    if (message.isAd || message.senderId == "SYSTEM_AD") {
                        return
                    }

                    val isCall = data["type"] == "CALL" || (message.callType != null && message.callStatus == "STARTING")

                    if (isCall) {
                        handleIncomingCall(message)
                    } else {
                        val isChatOpen = FriendApplication.isAppInForeground && FriendApplication.currentOpenedChatId == message.senderId

                        // Atualiza o resumo, mas define 'hasUnread' como falso se a conversa já estiver aberta
                        // Para mensagens de sistema que esvaziam a conversa, deixamos false pois não tem conteúdo lido/não lido novo pra ver, a conversa só foi deletada.
                        val isSystemClear = message.text == "Conversa limpa" || message.text == "Conversa apagada"
                        updateChatSummaryOnMessage(message, if (isSystemClear) false else !isChatOpen)

                        // Se for uma limpeza de chat, paramos o processamento aqui, pois não queremos exibir uma notificação vibrando na tela da pessoa só pra avisar que foi limpo!
                        if (isChatOpen || isSystemClear) return

                        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val cachedUsername = prefs.getString(KEY_MY_USERNAME, null)

                        // Processar em thread separada para permitir downloads sem travar o serviço
                        Thread {
                            showNotification(message, cachedUsername)
                        }.start()

                        if (cachedUsername == null) {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid != null) {
                                FirebaseDatabase.getInstance().reference.child("uid_to_username").child(uid).get()
                                    .addOnSuccessListener { snapshot ->
                                        snapshot.getValue(String::class.java)?.let {
                                            prefs.edit().putString(KEY_MY_USERNAME, it).apply()
                                        }
                                    }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing FCM: ${e.message}")
                }
            }
        }
    }

    private fun updateChatSummaryOnMessage(msg: Message, setAsUnread: Boolean) {
        if (msg.isAd || msg.senderId == "SYSTEM_AD") return

        val db = FirebaseDatabase.getInstance().reference
        val sender = msg.senderId
        val receiver = msg.receiverId

        val lastMsgText = when {
            msg.audioUrl != null -> "🎤 Áudio"
            msg.imageUrl != null -> "📷 Imagem"
            msg.videoUrl != null -> "📹 Vídeo"
            msg.stickerUrl != null -> "Sticker"
            else -> msg.text
        }

        // Atualiza o resumo para o receptor (eu)
        db.child("users").child(sender).get().addOnSuccessListener { snapshot ->
            val senderProfile = snapshot.getValue(UserProfile::class.java)
            val summary = ChatSummary(
                friendId = sender,
                lastMessage = lastMsgText,
                timestamp = msg.timestamp,
                lastSenderId = sender,
                friendName = senderProfile?.name ?: sender,
                friendPhotoUrl = senderProfile?.photoUrl,
                isGroup = false,
                isOnline = senderProfile?.isOnline ?: false,
                hasUnread = setAsUnread,
                presenceStatus = senderProfile?.presenceStatus ?: "Online"
            )
            db.child("chats").child(receiver).child(sender).setValue(summary)
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
            BitmapFactory.decodeStream(connection.inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun handleIncomingCall(message: Message) {
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            putExtra("callMessage", message)
            putExtra("isVideo", message.callType == "VIDEO")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (Settings.canDrawOverlays(this) || FriendApplication.isAppInForeground) {
            try { startActivity(intent) } catch (e: Exception) {}
        }
        showCallNotification(message)
    }

    private fun showCallNotification(message: Message) {
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            putExtra("callMessage", message)
            putExtra("isVideo", message.callType == "VIDEO")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPI = PendingIntent.getActivity(this, 1002, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(message.senderName ?: "Chamada")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPI, true)
            .setOngoing(true).setSilent(true)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1002, builder.build())
    }

    private fun showNotification(message: Message, myUsername: String?) {
        val senderName = message.senderName ?: "Oly"
        var finalPhotoUrl = message.senderPhotoUrl

        if (finalPhotoUrl.isNullOrEmpty()) {
            try {
                val snapshot = com.google.android.gms.tasks.Tasks.await(
                    FirebaseDatabase.getInstance().reference.child("users").child(message.senderId).get()
                )
                val profile = snapshot.getValue(UserProfile::class.java)
                finalPhotoUrl = profile?.photoUrl
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao buscar foto: ${e.message}")
            }
        }

        NotificationHelper.showMessageNotification(
            context = this,
            message = message,
            myUsername = myUsername ?: "Eu",
            senderName = senderName,
            senderPhotoUrl = finalPhotoUrl
        )
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Mensagens", NotificationManager.IMPORTANCE_HIGH))
            }
            if (nm.getNotificationChannel(CALL_CHANNEL_ID) == null) {
                nm.createNotificationChannel(NotificationChannel(CALL_CHANNEL_ID, "Chamadas", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    enableVibration(false)
                })
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference.child("uid_to_username").child(uid).get().addOnSuccessListener {
            it.getValue(String::class.java)?.let { user ->
                FirebaseDatabase.getInstance().reference.child("fcmTokens").child(user).child("token").setValue(token)
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_MY_USERNAME, user).apply()
            }
        }
    }
}
