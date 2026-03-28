package com.jack.friend.ui.chat

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.*
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.*
import com.jack.friend.ChatSummary
import com.jack.friend.ProfileActivity
import com.jack.friend.UserProfile
import com.jack.friend.ui.components.MetaSearchBar
import com.jack.friend.ui.theme.LocalChatColors
import com.jack.friend.ui.theme.MetaGray4
import com.jack.friend.ui.theme.iOSGreen
import com.jack.friend.ui.theme.iOSOrange
import com.jack.friend.ui.theme.iOSRed
import androidx.compose.ui.res.stringResource
import com.jack.friend.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    targetId: String,
    targetProfile: UserProfile?,
    activeChats: List<ChatSummary>,
    myPhotoUrl: String?,
    isTargetTyping: Boolean,
    showContacts: Boolean,
    isSearching: Boolean,
    searchInput: String,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onAddContact: () -> Unit,
    onChatHeaderClick: () -> Unit,
    // Opções do menu
    isMuted: Boolean = false,
    isPinned: Boolean = false,
    tempMessageDuration: Long = 0L,
    isBlocked: Boolean = false,
    onToggleMute: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onToggleTempMessages: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onBlockToggle: () -> Unit = {},
    onStarredMessages: () -> Unit = {},
) {
    val context = LocalContext.current
    val colors = LocalChatColors.current

    Surface(
        color = colors.secondaryBackground.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        modifier = Modifier.shadow(2.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(colors.secondaryBackground, colors.secondaryBackground.copy(0.9f))
                    )
                )
                .statusBarsPadding()
        ) {
            TopAppBar(
                title = {
                    if (targetId.isNotEmpty()) {
                        val currentChat = activeChats.find { it.friendId == targetId }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onChatHeaderClick() }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            ChatHeaderTitle(targetId, targetProfile, currentChat, isTargetTyping)
                        }
                    } else if (!isSearching) {
                        Text(
                            if (showContacts) stringResource(R.string.title_friends) else stringResource(R.string.title_chats),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                navigationIcon = {
                    if (targetId.isNotEmpty() || showContacts || isSearching) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                        }
                    } else {
                        IconButton(
                            onClick = {
                                context.startActivity(Intent(context, ProfileActivity::class.java))
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            AsyncImage(
                                model = myPhotoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, colors.primary.copy(0.3f), CircleShape)
                                    .background(colors.separator),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                },
                actions = {
                    if (targetId.isNotEmpty()) {
                        IconButton(onClick = onCallClick) {
                            Icon(Icons.Rounded.Phone, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                        }
                        IconButton(onClick = onVideoCallClick) {
                            Icon(Icons.Rounded.VideoCall, null, tint = colors.primary, modifier = Modifier.size(28.dp))
                        }
                        
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Rounded.MoreHoriz, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                            }
                            
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                offset = DpOffset(0.dp, 8.dp),
                                containerColor = colors.secondaryBackground,
                                shape = RoundedCornerShape(20.dp),
                                properties = PopupProperties(focusable = true)
                            ) {
                                ChatOptionsMenuSheet(
                                    isMuted = isMuted,
                                    isPinned = isPinned,
                                    tempMessageDuration = tempMessageDuration,
                                    isBlocked = isBlocked,
                                    onDismiss = { menuExpanded = false },
                                    onToggleMute = onToggleMute,
                                    onTogglePin = onTogglePin,
                                    onToggleTempMessages = onToggleTempMessages,
                                    onClearChat = onClearChat,
                                    onBlockToggle = onBlockToggle,
                                    onStarredMessages = onStarredMessages
                                )
                            }
                        }
                    } else if (showContacts) {
                        IconButton(onClick = { onAddContact() }) {
                            Icon(Icons.Rounded.PersonAdd, null, tint = colors.primary, modifier = Modifier.size(26.dp))
                        }
                    } else if (!isSearching) {
                        IconButton(onClick = { onSearchActiveChange(true) }) {
                            Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.hint_search), tint = colors.primary, modifier = Modifier.size(28.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            if (targetId.isEmpty() && !showContacts && isSearching) {
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    MetaSearchBar(
                        value = searchInput,
                        onValueChange = onSearchChange,
                        isSearching = isSearching,
                        onActiveChange = onSearchActiveChange
                    )
                }
            }
        }
    }
}

@Composable
fun ChatHeaderTitle(targetId: String, targetProfile: UserProfile?, currentChat: ChatSummary?, isTargetTyping: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = targetProfile?.photoUrl ?: currentChat?.friendPhotoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(1.dp, LocalChatColors.current.primary.copy(0.2f), CircleShape)
                .background(LocalChatColors.current.separator),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    targetProfile?.name?.takeIf { it.isNotBlank() } ?: currentChat?.friendName?.takeIf { it.isNotBlank() } ?: targetId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.width(6.dp))
                
                Text(
                    "@${targetId.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalChatColors.current.textSecondary.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
            if (isTargetTyping) {
                Text(
                    stringResource(R.string.status_typing),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalChatColors.current.primary,
                    fontWeight = FontWeight.Medium
                )
            } else {
                targetProfile?.let { PresenceIndicator(it) }
            }
        }
    }
}

@Composable
fun PresenceIndicator(profile: UserProfile) {
    val presenceColor = when (profile.presenceStatus) {
        "Online" -> iOSGreen
        "Ocupado" -> iOSRed
        "Ausente" -> iOSOrange
        else -> MetaGray4
    }
    val presenceLabel = when (profile.presenceStatus) {
        "Online" -> stringResource(R.string.status_online)
        "Ocupado" -> stringResource(R.string.status_busy)
        "Ausente" -> stringResource(R.string.status_away)
        "Invisível" -> stringResource(R.string.status_invisible)
        else -> profile.presenceStatus
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (profile.isOnline && profile.presenceStatus != "Invisível") {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(presenceColor)
                    .border(1.dp, Color.White.copy(0.5f), CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                presenceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = presenceColor,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                stringResource(R.string.status_offline),
                style = MaterialTheme.typography.labelSmall,
                color = MetaGray4,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
