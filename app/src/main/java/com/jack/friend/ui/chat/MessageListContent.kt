package com.jack.friend.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jack.friend.Message
import com.jack.friend.UserProfile
import com.jack.friend.ui.theme.MetaGray4
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@Composable
fun MessageListContent(
    messages: List<Message>,
    listState: LazyListState,
    myUsername: String,
    targetProfile: UserProfile?,
    showReadReceipts: Boolean = true,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onDelete: (Message) -> Unit,
    onReply: (Message) -> Unit,
    onReact: (Message, String) -> Unit,
    onEdit: (Message) -> Unit,
    onPin: (Message) -> Unit,
    onAudioPlayed: (Message) -> Unit
) {
    // Injetar anúncios aleatoriamente de forma estável e individual para cada pessoa
    val messagesWithAds = remember(messages, targetProfile?.id, myUsername) {
        if (messages.isEmpty()) return@remember messages
        
        // Seed baseada no hash do meu username e do ID do chat para ser individual por pessoa
        val seed = (targetProfile?.id?.hashCode() ?: 0).toLong() + myUsername.hashCode().toLong()
        val random = Random(seed)
        val result = mutableListOf<Message>()
        
        // Define o primeiro anúncio em uma posição aleatória entre 6 e 12
        var nextAdThreshold = 6 + random.nextInt(7) 
        var messageCount = 0

        messages.forEach { message ->
            result.add(message)
            messageCount++
            
            if (messageCount >= nextAdThreshold) {
                // Adiciona um marcador de anúncio que será renderizado via AdMob
                result.add(
                    Message(
                        id = "ad_item_${message.id}_$seed",
                        isAd = true,
                        timestamp = message.timestamp,
                        senderId = "SYSTEM_AD"
                    )
                )
                messageCount = 0
                // Próximo anúncio após mais 15 a 25 mensagens
                nextAdThreshold = 20 + random.nextInt(11)
            }
        }
        result
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(messagesWithAds, key = { _, m -> m.id }) { index, message ->
            if (message.isAd) {
                // Renderiza o banner retangular AdMob
                AdMobBubble()
            } else {
                val isMe = message.senderId == myUsername
                val prevMsg = findPreviousRealMessage(messagesWithAds, index)
                val nextMsg = findNextRealMessage(messagesWithAds, index)
                
                val isFirstInGroup = prevMsg == null || prevMsg.senderId != message.senderId || (message.timestamp - prevMsg.timestamp > 60000)
                val isLastInGroup = nextMsg == null || nextMsg.senderId != message.senderId || (nextMsg.timestamp - message.timestamp > 60000)
                
                if (isFirstInGroup) {
                    val dateText = formatDateHeader(message.timestamp)
                    if (dateText != (prevMsg?.let { formatDateHeader(it.timestamp) } ?: "")) {
                        DateHeader(dateText)
                    }
                }
                
                MetaMessageBubble(
                    message = message,
                    isMe = isMe,
                    targetPhotoUrl = targetProfile?.photoUrl,
                    isFirstInGroup = isFirstInGroup,
                    isLastInGroup = isLastInGroup,
                    showReadReceipts = showReadReceipts,
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onDelete = { onDelete(message) },
                    onReply = { onReply(message) },
                    onReact = { onReact(message, it) },
                    onEdit = { onEdit(message) },
                    onPin = { onPin(message) },
                    onAudioPlayed = { onAudioPlayed(message) }
                )
            }
        }
    }
}

private fun findPreviousRealMessage(list: List<Message>, currentIndex: Int): Message? {
    for (i in currentIndex - 1 downTo 0) {
        if (!list[i].isAd) return list[i]
    }
    return null
}

private fun findNextRealMessage(list: List<Message>, currentIndex: Int): Message? {
    for (i in currentIndex + 1 until list.size) {
        if (!list[i].isAd) return list[i]
    }
    return null
}

@Composable
fun DateHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MetaGray4,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        textAlign = TextAlign.Center
    )
}

fun formatDateHeader(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val now = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return when {
        calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> "HOJE"
        calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1 -> "ONTEM"
        else -> SimpleDateFormat("d 'DE' MMMM", Locale("pt", "BR")).format(Date(timestamp)).uppercase()
    }
}
