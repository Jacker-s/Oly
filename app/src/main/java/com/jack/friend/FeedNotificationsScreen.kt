package com.jack.friend

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.jack.friend.ui.theme.ChatColors
import com.jack.friend.ui.theme.LocalChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedNotificationsScreen(
    viewModel: ChatViewModel,
    onNotificationClick: (String) -> Unit, // postId
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = LocalChatColors.current
    val notifications by viewModel.feedNotifications.collectAsStateWithLifecycle(emptyList())
    var showClearConfirm by remember { mutableStateOf(false) }
    val initialFilter = stringResource(R.string.tab_all)
    var selectedFilter by remember(initialFilter) { mutableStateOf(initialFilter) }
    
    val filters = listOf(
        stringResource(R.string.tab_all), 
        stringResource(R.string.filter_likes), 
        stringResource(R.string.filter_comments), 
        stringResource(R.string.filter_mentions)
    )

    // Filtrar notificações antes de agrupar
    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            filters[1] -> notifications.filter { it.type == "LIKE" || it.type == "REACTION" }
            filters[2] -> notifications.filter { it.type == "COMMENT" }
            filters[3] -> notifications.filter { it.type == "MENTION" }
            else -> notifications
        }
    }

    // Inteligência: Agrupar notificações semelhantes (apenas para Curtidas/Reações no mesmo post)
    val smartNotifications = remember(filteredNotifications) {
        val result = mutableListOf<FeedNotification>()
        val processedKeys = mutableSetOf<String>()

        filteredNotifications.forEach { notification ->
            if (notification.type == "LIKE" || notification.type == "REACTION") {
                val key = "${notification.postId}_${notification.type}"
                if (!processedKeys.contains(key)) {
                    val sameGroup = filteredNotifications.filter { it.postId == notification.postId && it.type == notification.type }
                    if (sameGroup.size > 1) {
                        val main = sameGroup.first()
                        val aggregated = main.copy(
                            fromName = context.getString(R.string.feed_aggregated_notification, main.fromName, sameGroup.size - 1),
                            id = "GROUP_${main.id}"
                        )
                        result.add(aggregated)
                    } else {
                        result.add(notification)
                    }
                    processedKeys.add(key)
                }
            } else {
                result.add(notification)
            }
        }
        result
    }

    LaunchedEffect(Unit) {
        viewModel.markNotificationsAsRead()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(colors.topBar)) {
                TopAppBar(
                    title = { 
                        Text(stringResource(R.string.notifications_title), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = colors.textPrimary)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                        }
                    },
                    actions = {
                        val unreadCount = notifications.count { !it.isRead }
                        if (unreadCount > 0) {
                            IconButton(onClick = { viewModel.markNotificationsAsRead() }) {
                                Icon(Icons.Rounded.DoneAll, stringResource(R.string.action_mark_all_read), tint = colors.primary)
                            }
                        }
                        if (notifications.isNotEmpty()) {
                            IconButton(onClick = { showClearConfirm = true }) {
                                Icon(Icons.Rounded.DeleteSweep, stringResource(R.string.action_clear_all), tint = colors.textSecondary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                
                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            onClick = { selectedFilter = filter },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) colors.primary else colors.secondaryBackground,
                            border = if (!isSelected) BorderStroke(1.dp, colors.separator.copy(0.2f)) else null
                        ) {
                            Text(
                                text = filter,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else colors.textPrimary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        },
        containerColor = colors.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (smartNotifications.isEmpty()) {
                EmptyNotificationsState(colors, selectedFilter)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 140.dp) // Espaço para a floating bar
                ) {
                    items(smartNotifications, key = { it.id }) { notification ->
                        NotificationItemImproved(
                            notification = notification,
                            onClick = {
                                viewModel.markNotificationsAsRead()
                                onNotificationClick(notification.postId)
                            },
                            onDelete = { 
                                if (notification.id.startsWith("GROUP_")) {
                                    val originalPostId = notification.postId
                                    notifications.filter { it.postId == originalPostId && (it.type == "LIKE" || it.type == "REACTION") }
                                        .forEach { viewModel.deleteNotification(it.id) }
                                } else {
                                    viewModel.deleteNotification(notification.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text(stringResource(R.string.dialog_clear_notifications_title), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.dialog_clear_notifications_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            notifications.forEach { viewModel.deleteNotification(it.id) }
                            showClearConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text(stringResource(R.string.action_clear), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
                    }
                },
                containerColor = colors.secondaryBackground,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItemImproved(
    notification: FeedNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalChatColors.current
    
    val icon = when (notification.type) {
        "LIKE" -> Icons.Rounded.Favorite
        "COMMENT" -> Icons.AutoMirrored.Rounded.Comment
        "REACTION" -> Icons.Rounded.AddReaction
        "MENTION" -> Icons.Rounded.AlternateEmail
        else -> Icons.Rounded.Notifications
    }
    
    val iconColor = when (notification.type) {
        "LIKE" -> Color.Red
        "COMMENT" -> colors.primary
        "REACTION" -> Color(0xFFFF9800)
        "MENTION" -> Color(0xFF4CAF50)
        else -> colors.primary
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                    else -> Color.Transparent
                }, label = "dismissColor"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Rounded.Delete, stringResource(R.string.menu_remove), tint = Color.White)
            }
        }
    ) {
        Surface(
            onClick = onClick,
            color = if (notification.isRead) colors.background else colors.primary.copy(alpha = 0.05f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Photo & Action Icon
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = notification.fromPhotoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(colors.separator),
                        contentScale = ContentScale.Crop
                    )
                    
                    Surface(
                        modifier = Modifier.size(20.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val annotatedText = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 15.sp)) {
                            append(notification.fromName.ifBlank { context.getString(R.string.notification_someone) })
                        }
                        
                        append(" ")
                        
                        val actionText = when (notification.type) {
                            "LIKE" -> context.getString(R.string.feed_notification_action_liked)
                            "COMMENT" -> context.getString(R.string.feed_notification_action_commented)
                            "REACTION" -> context.getString(R.string.feed_notification_action_reacted, notification.reactionEmoji ?: "")
                            "MENTION" -> context.getString(R.string.feed_notification_action_mentioned)
                            else -> context.getString(R.string.feed_notification_action_interacted)
                        }
                        
                        withStyle(style = SpanStyle(color = colors.textPrimary.copy(alpha = 0.8f), fontSize = 14.sp)) {
                            append(actionText)
                        }
                        
                        if (notification.postPreviewText.isNotBlank() && (notification.type == "COMMENT" || notification.type == "MENTION")) {
                            withStyle(style = SpanStyle(color = colors.textSecondary, fontSize = 14.sp)) {
                                append("\"${notification.postPreviewText.take(45)}${if (notification.postPreviewText.length > 45) "..." else ""}\"")
                            }
                        }
                    }
                    
                    Text(
                        text = annotatedText,
                        lineHeight = 18.sp,
                        maxLines = 3
                    )
                    
                    Text(
                        text = formatTimeSince(context, notification.timestamp),
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Spacer(Modifier.width(8.dp))

                // Post Preview Thumbnail
                if (!notification.postPhotoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = notification.postPhotoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(45.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.separator),
                        contentScale = ContentScale.Crop
                    )
                } else if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationsState(colors: ChatColors, filter: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.NotificationsNone,
            null,
            modifier = Modifier.size(64.dp),
            tint = colors.textSecondary.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (filter == stringResource(R.string.tab_all)) stringResource(R.string.empty_notifications_title) else stringResource(R.string.empty_notifications_filter_no_results, filter),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colors.textPrimary
        )
        Text(
            stringResource(R.string.empty_notifications_subtitle),
            fontSize = 14.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
