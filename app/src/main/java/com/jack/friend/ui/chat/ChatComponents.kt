package com.jack.friend.ui.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jack.friend.ChatSummary
import com.jack.friend.UserProfile
import com.jack.friend.UserStatus
import com.jack.friend.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaStatusRow(
    statuses: List<UserStatus>,
    myPhotoUrl: String?,
    myUsername: String,
    contacts: List<UserProfile>,
    onAdd: () -> Unit,
    onViewUserStatuses: (List<UserStatus>) -> Unit
) {
    val contactIds = contacts.map { it.id }.toSet()

    // Filtra para mostrar apenas status próprios ou de contatos
    val filteredStatuses = statuses.filter { it.userId == myUsername || contactIds.contains(it.userId) }

    val grouped = filteredStatuses.groupBy { it.userId }
    val myStatuses = grouped[myUsername] ?: emptyList()
    val otherStatuses = grouped.filter { it.key != myUsername }

    val hasUnread = { list: List<UserStatus> -> list.any { !it.viewers.containsKey(myUsername) } }

    var showMyStatusOptions by remember { mutableStateOf(false) }

    if (showMyStatusOptions) {
        ModalBottomSheet(
            onDismissRequest = { showMyStatusOptions = false },
            containerColor = LocalChatColors.current.secondaryBackground,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(top = 12.dp).width(36.dp).height(4.dp),
                    color = LocalChatColors.current.textSecondary.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {}
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(
                    "Meu Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = LocalChatColors.current.textPrimary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Surface(
                    onClick = {
                        showMyStatusOptions = false
                        onViewUserStatuses(myStatuses)
                    },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = LocalChatColors.current.primary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                Icons.Rounded.Visibility,
                                null,
                                tint = LocalChatColors.current.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("Ver Status", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }

                Surface(
                    onClick = {
                        showMyStatusOptions = false
                        onAdd()
                    },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        ) {
                            Icon(
                                Icons.Rounded.AddCircleOutline,
                                null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("Adicionar Novo", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Meu Status
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(76.dp)
                    .clickable {
                        if (myStatuses.isNotEmpty()) showMyStatusOptions = true else onAdd()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val gradientBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF833AB4),
                            Color(0xFFFD1D1D),
                            Color(0xFFF56040),
                            Color(0xFFFFDC80)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .drawBehind {
                                val strokeWidth = 3.dp.toPx()
                                if (myStatuses.isNotEmpty()) {
                                    drawCircle(brush = gradientBrush, style = Stroke(strokeWidth))
                                }
                            }
                            .padding(6.dp)
                    ) {
                        AsyncImage(
                            model = myPhotoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape).background(LocalChatColors.current.separator),
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (myStatuses.isEmpty()) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).size(22.dp).offset(x = (-2).dp, y = (-2).dp),
                            shape = CircleShape,
                            color = LocalChatColors.current.primary,
                            border = BorderStroke(2.dp, Color.White)
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.padding(2.dp))
                        }
                    }
                }
                Text("Meu status", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
            }
        }

        // Status dos Contatos
        otherStatuses.forEach { (_, userList) ->
            val first = userList.first()
            val unread = hasUnread(userList)

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(76.dp).clickable { onViewUserStatuses(userList) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .drawBehind {
                                val strokeWidth = 3.dp.toPx()
                                val gap = 6f
                                val count = userList.size
                                val gradientBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF833AB4),
                                        Color(0xFFFD1D1D),
                                        Color(0xFFF56040),
                                        Color(0xFFFFDC80)
                                    )
                                )

                                if (count == 1) {
                                    val unread = !userList[0].viewers.containsKey(myUsername)
                                    if (unread) {
                                        drawCircle(brush = gradientBrush, style = Stroke(strokeWidth))
                                    } else {
                                        drawCircle(color = Color.LightGray.copy(alpha = 0.4f), style = Stroke(strokeWidth))
                                    }
                                } else {
                                    val sweep = (360f / count) - gap
                                    for (i in 0 until count) {
                                        val start = (i * (360f / count)) - 90f + (gap / 2)
                                        val unread = !userList[i].viewers.containsKey(myUsername)
                                        if (unread) {
                                            drawArc(
                                                brush = gradientBrush,
                                                startAngle = start,
                                                sweepAngle = sweep,
                                                useCenter = false,
                                                style = Stroke(strokeWidth)
                                            )
                                        } else {
                                            drawArc(
                                                color = Color.LightGray.copy(alpha = 0.4f),
                                                startAngle = start,
                                                sweepAngle = sweep,
                                                useCenter = false,
                                                style = Stroke(strokeWidth)
                                            )
                                        }
                                    }
                                }
                            }
                            .padding(6.dp)
                    ) {
                        AsyncImage(
                            model = first.userPhotoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape).background(LocalChatColors.current.separator),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(first.username, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetaChatItem(
    summary: ChatSummary,
    myId: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val chatColors = LocalChatColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = summary.friendPhotoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(chatColors.separator),
                contentScale = ContentScale.Crop
            )

            if (summary.isOnline && summary.presenceStatus != "Invisível") {
                val presenceColor = when (summary.presenceStatus) {
                    "Online" -> iOSGreen
                    "Ocupado" -> iOSRed
                    "Ausente" -> iOSOrange
                    else -> Color.Gray
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .background(chatColors.secondaryBackground, CircleShape)
                        .padding(3.dp)
                        .background(presenceColor, CircleShape)
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.friendName ?: summary.friendId,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (summary.hasUnread) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (summary.isMuted) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsOff,
                            contentDescription = null,
                            tint = MetaGray4,
                            modifier = Modifier.padding(start = 4.dp).size(14.dp)
                        )
                    }

                    if (summary.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = chatColors.primary,
                            modifier = Modifier.padding(start = 4.dp).size(14.dp)
                        )
                    }
                }

                Text(
                    text = formatChatTime(summary.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (summary.hasUnread) chatColors.primary else MetaGray4
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                val lastMessageText = if (summary.isTyping) {
                    "Digitando..."
                } else if (summary.lastMessage == "Conversa limpa" || summary.lastMessage == "Conversa apagada") {
                    summary.lastMessage
                } else {
                    val prefix = if (summary.lastSenderId == myId) "Você: " else ""
                    "$prefix${summary.lastMessage}"
                }

                val contentColor = if (summary.isTyping) {
                    chatColors.primary
                } else if (summary.hasUnread) {
                    chatColors.textPrimary
                } else {
                    MetaGray4
                }

                if (summary.isEphemeral && !summary.isTyping) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 4.dp).size(14.dp)
                    )
                }

                Text(
                    text = lastMessageText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (summary.hasUnread) FontWeight.Medium else FontWeight.Normal
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (summary.hasUnread) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(12.dp)
                            .background(chatColors.primary, CircleShape)
                    )
                }
            }
        }
    }
}

private fun formatChatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = Calendar.getInstance()
    val chatTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isSameDay = now.get(Calendar.YEAR) == chatTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == chatTime.get(Calendar.DAY_OF_YEAR)

    return when {
        isSameDay -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            val diffDays = (now.timeInMillis - chatTime.timeInMillis) / (24 * 60 * 60 * 1000)
            when {
                diffDays < 1 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                diffDays < 2 -> "Ontem"
                diffDays < 7 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
                else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}

@Composable
fun MetaUserItem(
    user: UserProfile,
    isContact: Boolean,
    onItemClick: () -> Unit,
    onChatClick: () -> Unit,
    onAddContactClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(model = user.photoUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape).background(LocalChatColors.current.separator), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("@${user.id.lowercase()}", style = MaterialTheme.typography.labelSmall, color = MetaGray4)
        }
        Row {
            if (!isContact) IconButton(onClick = onAddContactClick) { Icon(Icons.Rounded.PersonAdd, null, tint = LocalChatColors.current.primary) }
            IconButton(onClick = onChatClick) { Icon(Icons.Rounded.ChatBubble, null, tint = LocalChatColors.current.primary) }
        }
    }
}
