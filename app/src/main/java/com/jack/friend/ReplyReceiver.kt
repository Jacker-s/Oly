package com.jack.friend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput == null) {
            Log.e("ReplyReceiver", "RemoteInput nulo")
            return
        }

        val replyText = remoteInput.getCharSequence(FriendMessagingService.KEY_TEXT_REPLY)?.toString()
        val chatId = intent.getStringExtra("chatId") ?: return
        if (replyText.isNullOrBlank()) return

        val providedUsername = intent.getStringExtra("senderName").orEmpty()
        if (providedUsername.isNotBlank()) {
            sendReply(chatId, providedUsername, replyText, context)
            return
        }

        val pending = goAsync()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            pending.finish()
            return
        }

        FirebaseDatabase.getInstance().reference
            .child("uid_to_username")
            .child(uid)
            .get()
            .addOnSuccessListener { snap ->
                val resolvedUsername = snap.getValue(String::class.java).orEmpty()
                if (resolvedUsername.isNotBlank()) {
                    sendReply(chatId, resolvedUsername, replyText, context)
                } else {
                    Log.e("ReplyReceiver", "Username vazio ao responder por notificação")
                }
                pending.finish()
            }
            .addOnFailureListener {
                Log.e("ReplyReceiver", "Falha ao resolver username: ${it.message}")
                pending.finish()
            }
    }

    private fun sendReply(chatId: String, myUsername: String, replyText: String, context: Context) {
        val db = FirebaseDatabase.getInstance().reference
        val msgId = db.push().key ?: return

        val msg = Message(
            id = msgId,
            senderId = myUsername,
            receiverId = chatId,
            text = replyText,
            timestamp = System.currentTimeMillis()
        )

        val path = "messages/${chatPathFor(myUsername, chatId)}"
        db.child(path).child(msgId).setValue(msg).addOnSuccessListener {
            Log.d("ReplyReceiver", "Mensagem enviada via notificação")
            updateChatSummary(msg, myUsername)
        }.addOnFailureListener {
            Log.e("ReplyReceiver", "Erro ao enviar resposta: ${it.message}")
        }

        NotificationHelper.clearNotification(context, chatId)
    }

    private fun chatPathFor(u1: String, u2: String): String {
        val user1 = u1.uppercase().trim()
        val user2 = u2.uppercase().trim()
        return if (user1 < user2) "${user1}_$user2" else "${user2}_$user1"
    }

    private fun updateChatSummary(msg: Message, me: String) {
        val db = FirebaseDatabase.getInstance().reference
        val friend = msg.receiverId

        db.child("users").child(friend).get().addOnSuccessListener { snapshot ->
            val friendProf = snapshot.getValue(UserProfile::class.java)
            val summary = ChatSummary(
                friendId = friend,
                lastMessage = msg.text,
                timestamp = msg.timestamp,
                lastSenderId = me,
                friendName = friendProf?.name ?: friend,
                friendPhotoUrl = friendProf?.photoUrl,
                isOnline = friendProf?.isOnline ?: false,
                hasUnread = false,
                presenceStatus = friendProf?.presenceStatus ?: "Online"
            )
            db.child("chats").child(me).child(friend).setValue(summary)

            db.child("users").child(me).get().addOnSuccessListener { meSnapshot ->
                val meProf = meSnapshot.getValue(UserProfile::class.java)
                db.child("chats").child(friend).child(me).setValue(
                    summary.copy(
                        friendId = me,
                        friendName = meProf?.name ?: me,
                        friendPhotoUrl = meProf?.photoUrl,
                        hasUnread = true,
                        presenceStatus = meProf?.presenceStatus ?: "Online"
                    )
                )
            }
        }
    }
}
