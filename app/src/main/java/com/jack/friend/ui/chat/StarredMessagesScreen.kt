package com.jack.friend.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.jack.friend.ChatViewModel
import com.jack.friend.Message
import com.jack.friend.ui.theme.LocalChatColors
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.jack.friend.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredMessagesScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val chatColors = LocalChatColors.current
    val messages by viewModel.messages.collectAsStateWithLifecycle(emptyList())
    val myUsername by viewModel.myUsername.collectAsStateWithLifecycle("")

    // Collect all starred messages from firestore
    val starredMessages by remember(messages) {
        derivedStateOf { messages.filter { it.isStarred } }
    }

    // Also listen globally — for now we rely on the active chat's messages.
    // A future improvement would be to add a dedicated global starred messages query.

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFBF00),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.action_starred_messages),
                            fontWeight = FontWeight.Bold,
                            color = chatColors.textPrimary,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = chatColors.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = chatColors.primaryBackground
                )
            )
        },
        containerColor = chatColors.primaryBackground
    ) { padding ->
        if (starredMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFFBF00).copy(alpha = 0.2f),
                                        Color(0xFFFFBF00).copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.StarBorder,
                            contentDescription = null,
                            tint = Color(0xFFFFBF00).copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.starred_messages_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = chatColors.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.starred_messages_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = chatColors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(starredMessages, key = { it.id }) { message ->
                    StarredMessageItem(
                        message = message,
                        myUsername = myUsername,
                        onUnstar = {
                            val friendId = if (message.senderId == myUsername) message.receiverId else message.senderId
                            viewModel.toggleStarredMessage(message.id, friendId, true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StarredMessageItem(
    message: Message,
    myUsername: String,
    onUnstar: () -> Unit
) {
    val chatColors = LocalChatColors.current
    val isMe = message.senderId == myUsername
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("dd/MM/yy · HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = chatColors.secondaryBackground,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Accent left border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFBF00), Color(0xFFFF8C00))
                        )
                    )
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header: sender name + time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isMe) stringResource(R.string.label_you) else (message.senderName ?: message.senderId),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = chatColors.primary
                    )
                    Text(
                        text = timeStr,
                        fontSize = 11.sp,
                        color = chatColors.textSecondary
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Content
                when {
                    message.imageUrl != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.attachment_image),
                                color = chatColors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    message.audioUrl != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.MicNone,
                                null,
                                tint = chatColors.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.label_voice_message),
                                color = chatColors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    message.videoUrl != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Videocam,
                                null,
                                tint = chatColors.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.label_video),
                                color = chatColors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    message.latitude != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.LocationOn,
                                null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.label_location_shared),
                                color = chatColors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    message.text.isNotBlank() -> {
                        Text(
                            text = message.text,
                            color = chatColors.textPrimary,
                            fontSize = 14.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Unstar button
            IconButton(
                onClick = onUnstar,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = stringResource(R.string.action_remove_starred),
                    tint = Color(0xFFFFBF00),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
