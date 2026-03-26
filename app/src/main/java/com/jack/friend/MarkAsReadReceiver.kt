package com.jack.friend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MarkAsReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra("chatId") ?: return
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Update username first to find the chat summary
        FirebaseDatabase.getInstance().reference.child("uid_to_username").child(myUid).get()
            .addOnSuccessListener { snapshot ->
                val myUsername = snapshot.getValue(String::class.java) ?: return@addOnSuccessListener
                
                // Set hasUnread to false
                FirebaseDatabase.getInstance().reference
                    .child("chats")
                    .child(myUsername)
                    .child(chatId)
                    .child("hasUnread")
                    .setValue(false)
                    .addOnSuccessListener {
                        Log.d("MarkAsReadReceiver", "Chat $chatId marcado como lido conforme notificação")
                        NotificationHelper.clearNotification(context, chatId)
                    }
            }
    }
}
