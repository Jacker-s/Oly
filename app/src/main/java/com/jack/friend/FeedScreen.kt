package com.jack.friend

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.Feed
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.WindowInsets
import com.jack.friend.ui.chat.MetaStatusRow
import com.jack.friend.ui.chat.StatusViewer
import com.jack.friend.ui.chat.StatusComposer
import com.jack.friend.ui.components.AdMobBanner
import com.jack.friend.ui.components.MediaAttachmentSheet
import com.jack.friend.ui.components.InAppCameraView
import com.jack.friend.ui.components.ModernGalleryPicker
import com.jack.friend.ui.components.ModernVideoPlayer
import com.jack.friend.ui.components.WappiLikeIcon
import com.jack.friend.ui.components.WappiCommentIcon
import com.jack.friend.ui.components.WappiShareIcon
import com.jack.friend.ui.theme.LocalChatColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: ChatViewModel,
    billingManager: BillingManager? = null,
    onAuthorClick: (UserProfile) -> Unit = {},
    onViewAllNotifications: () -> Unit = {},
    onStatusAdd: () -> Unit = {},
    onStatusView: (List<UserStatus>) -> Unit = {}
) {
    val colors = LocalChatColors.current
    val myName by viewModel.myName.collectAsStateWithLifecycle("")
    val myPhotoUrl by viewModel.myPhotoUrl.collectAsStateWithLifecycle(null)
    val feedPosts by viewModel.feedPosts.collectAsStateWithLifecycle(emptyList())
    val myUsername by viewModel.myUsername.collectAsStateWithLifecycle("")
    val notifications by viewModel.feedNotifications.collectAsStateWithLifecycle(emptyList())
    val statuses by viewModel.statuses.collectAsStateWithLifecycle(emptyList())
    val contacts by viewModel.contacts.collectAsStateWithLifecycle(emptyList())
    val isPremium by (billingManager?.isPremiumPurchased?.collectAsStateWithLifecycle(false) ?: remember { mutableStateOf(false) })

    val unreadCount = notifications.count { !it.isRead }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showNotifications by remember { mutableStateOf(false) }
    var showFullNotifications by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf<FeedPost?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val openPostId by viewModel.openPostId.collectAsStateWithLifecycle(null)

    LaunchedEffect(openPostId, feedPosts) {
        if (!openPostId.isNullOrEmpty() && feedPosts.isNotEmpty()) {
            val postIndex = feedPosts.indexOfFirst { it.id == openPostId }
            if (postIndex >= 0) {
                listState.animateScrollToItem(postIndex + 2)
            }
            viewModel.setOpenPostId(null)
        }
    }

    // Marca notificações como lidas ao abrir o menu
    LaunchedEffect(showNotifications) {
        if (showNotifications) {
            viewModel.markNotificationsAsRead()
        }
    }

    var postTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedEmoji by remember { mutableStateOf<String?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var showCustomGallery by remember { mutableStateOf(false) }
    var showMediaOptions by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(true) }
    var fullscreenImage by remember { mutableStateOf<String?>(null) }
    var sharingPost by remember { mutableStateOf<FeedPost?>(null) }
    var interactingPost by remember { mutableStateOf<FeedPost?>(null) }

    // Omit local status states as they are now handled by parent via callbacks

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showCamera = true
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showCustomGallery = true
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    val feedItems = remember(feedPosts, isPremium) {
        val items = mutableListOf<Any>()
        feedPosts.forEachIndexed { index, post ->
            items.add(post)
            // Inserir anúncio logo após o primeiro post se não for premium
            if (!isPremium && index == 0 && feedPosts.size > 1) {
                items.add("ADMOB_BANNER")
            } else if (!isPremium && (index + 1) % 10 == 0) { // Menos frequente depois
                items.add("ADMOB_BANNER")
            }
        }
        items
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Surface(
                    color = colors.topBar.copy(alpha = 0.95f),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.zIndex(5f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        // Logo/Title on the Left
                        Row(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ViewStream, contentDescription = null, tint = colors.primary, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.app_name).split(":")[0].trim(),
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = colors.titlePrimary,
                                letterSpacing = (-1.0).sp
                            )
                        }

                        // Actions on the Right (Notifications + Avatar)
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showNotifications = true }) {
                                Box {
                                    Icon(
                                        imageVector = if (unreadCount > 0) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                                        contentDescription = stringResource(R.string.feed_notifications_description),
                                        tint = if (unreadCount > 0) colors.primary else colors.textPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    if (unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .align(Alignment.TopEnd)
                                                .clip(CircleShape)
                                                .background(Color.Red)
                                                .border(1.dp, colors.topBar, CircleShape)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { onAuthorClick(UserProfile(id = myUsername, name = myName, photoUrl = myPhotoUrl)) }
                            ) {
                                AsyncImage(
                                    model = myPhotoUrl,
                                    contentDescription = stringResource(R.string.feed_my_profile_description),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }


                            DropdownMenu(
                                expanded = showNotifications,
                                onDismissRequest = { showNotifications = false },
                                modifier = Modifier
                                    .width(340.dp)
                                    .heightIn(max = 560.dp)
                                    .background(colors.secondaryBackground),
                                shape = RoundedCornerShape(24.dp),
                                offset = androidx.compose.ui.unit.DpOffset(0.dp, 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    // Header do Menu de Notificações
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 20.dp, vertical = 16.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Notifications, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.feed_interactions_title), fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.primary)
                                        }
                                        if (unreadCount > 0) {
                                            Surface(
                                                color = colors.primary.copy(alpha = 0.1f),
                                                shape = CircleShape,
                                                modifier = Modifier.clickable { viewModel.markNotificationsAsRead() }
                                            ) {
                                                Text(
                                                    stringResource(R.string.feed_action_read_all),
                                                    fontSize = 11.sp,
                                                    color = colors.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(thickness = 0.5.dp, color = colors.separator.copy(alpha = 0.1f))

                                    if (notifications.isEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().height(200.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Rounded.NotificationsNone, null, modifier = Modifier.size(48.dp), tint = colors.textSecondary.copy(0.3f))
                                            Spacer(Modifier.height(12.dp))
                                            Text(stringResource(R.string.feed_no_notifications), color = colors.textSecondary, fontSize = 14.sp)
                                        }
                                    } else {
                                        val context = LocalContext.current
                                        // Inteligência: Agrupar notificações no menu rápido também
                                        val aggregatedMenuNotifications = remember(notifications) {
                                            val result = mutableListOf<FeedNotification>()
                                            val processedKeys = mutableSetOf<String>()
                                            notifications.forEach { n ->
                                                if (n.type == "LIKE" || n.type == "REACTION") {
                                                    val key = "${n.postId}_${n.type}"
                                                    if (!processedKeys.contains(key)) {
                                                        val same = notifications.filter { it.postId == n.postId && it.type == n.type }
                                                        if (same.size > 1) {
                                                            result.add(n.copy(fromName = context.getString(R.string.notification_aggregated_format, n.fromName, same.size - 1)))
                                                        } else result.add(n)
                                                        processedKeys.add(key)
                                                    }
                                                } else result.add(n)
                                            }
                                            result
                                        }

                                        aggregatedMenuNotifications.take(5).forEach { notification ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        showNotifications = false
                                                        viewModel.markNotificationsAsRead()
                                                        val postIndex = feedPosts.indexOfFirst { it.id == notification.postId }
                                                        if (postIndex >= 0) {
                                                            scope.launch { listState.animateScrollToItem(postIndex + 2) }
                                                        }
                                                    }
                                                    .background(if (!notification.isRead) colors.primary.copy(alpha = 0.03f) else Color.Transparent)
                                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    NotificationDropdownItem(notification)
                                                }

                                                IconButton(
                                                    onClick = { viewModel.deleteNotification(notification.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Close, null, tint = colors.textSecondary.copy(0.5f), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            HorizontalDivider(thickness = 0.5.dp, color = colors.separator.copy(alpha = 0.05f))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showNotifications = false; showFullNotifications = true }
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                stringResource(R.string.feed_view_full_history),
                                                color = colors.primary,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                style = TextStyle(letterSpacing = 0.5.sp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = colors.background
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        delay(1500)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.padding(innerPadding)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 130.dp)
                ) {
                    // Stories / Status Row
                    item {
                        MetaStatusRow(
                            statuses = statuses,
                            myPhotoUrl = myPhotoUrl,
                            myUsername = myUsername,
                            contacts = contacts,
                            onAdd = onStatusAdd,
                            onViewUserStatuses = onStatusView
                        )
                        HorizontalDivider(color = colors.separator.copy(0.1f), thickness = 8.dp)
                    }

                    // New Post Box
                    item {
                        val suggestions by viewModel.mentionSuggestions.collectAsStateWithLifecycle()

                        Column {
                            MentionSuggestionsBox(
                                suggestions = suggestions,
                                onSuggestionClick = { user ->
                                    val text = postTextFieldValue.text
                                    val cursor = postTextFieldValue.selection.start
                                    val query = getMentionQuery(text, cursor) ?: ""
                                    val start = text.lastIndexOf(query, cursor - 1)
                                    val newText = text.replaceRange(start, cursor, "@${user.id} ")
                                    postTextFieldValue = TextFieldValue(newText, TextRange(newText.length))
                                    viewModel.updateMentionQuery("")
                                },
                                colors = colors
                            )

                            NewPostBox(
                                myPhotoUrl = myPhotoUrl,
                                postTextFieldValue = postTextFieldValue,
                                onTextFieldValueChange = { newValue ->
                                    postTextFieldValue = newValue
                                    val query = getMentionQuery(newValue.text, newValue.selection.start)
                                    viewModel.updateMentionQuery(query ?: "")
                                },
                                selectedImageUris = selectedImageUris,
                                onImageRemove = { uri -> selectedImageUris = selectedImageUris.filter { it != uri } },
                                selectedEmoji = selectedEmoji,
                                onEmojiToggle = { showEmojiPicker = !showEmojiPicker },
                                onEmojiRemove = { selectedEmoji = null },
                                onMediaOptionClick = { showMediaOptions = true },
                                isPublic = isPublic,
                                onPrivacyToggle = { isPublic = !isPublic },
                                onPost = {
                                    if (postTextFieldValue.text.isNotBlank() || selectedImageUris.isNotEmpty() || selectedEmoji != null) {
                                        viewModel.postToFeed(
                                            text = postTextFieldValue.text.trim(),
                                            imageUris = selectedImageUris,
                                            animatedEmoji = selectedEmoji,
                                            isPublic = isPublic
                                        )
                                        postTextFieldValue = TextFieldValue("")
                                        selectedImageUris = emptyList()
                                        selectedEmoji = null
                                        showEmojiPicker = false
                                        isPublic = true
                                    }
                                },
                                colors = colors
                            )
                        }

                        if (showEmojiPicker) {
                            MetaEmojiPickerPro(
                                onEmojiSelected = { emoji ->
                                    selectedEmoji = emoji
                                    showEmojiPicker = false
                                },
                                modifier = Modifier.fillMaxWidth().height(250.dp)
                            )
                        }
                        HorizontalDivider(color = colors.separator.copy(0.1f), thickness = 8.dp)
                    }

                    if (feedPosts.isEmpty()) {
                        item {
                            EmptyFeedState(colors)
                        }
                    } else {
                        itemsIndexed(feedItems, key = { index, item ->
                            when (item) {
                                is FeedPost -> item.id
                                is String -> "AD_$index"
                                else -> index
                            }
                        }) { index, item ->
                            when (item) {
                                is FeedPost -> {
                                    AnimatedPostItem(
                                        post = item,
                                        isAuthorOnline = contacts.any { it.id == item.authorId && it.isOnline } || (item.authorId == myUsername),
                                        myUsername = myUsername,
                                        myPhotoUrl = myPhotoUrl,
                                        viewModel = viewModel,
                                        onAuthorClick = onAuthorClick,
                                        onImageClick = { fullscreenImage = it },
                                        onShareClick = { sharingPost = item },
                                        onInteractionsClick = { interactingPost = it }
                                    )
                                }
                                 is String -> {
                                    if (item == "ADMOB_BANNER") {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            color = colors.secondaryBackground
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                // Sponsored Header (Looks like a post)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(colors.primary.copy(alpha = 0.1f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Rounded.Campaign, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                                                    }
                                                    Spacer(Modifier.width(12.dp))
                                                    Column {
                                                        Text("Patrocinado", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                                        Text("Sugestão para você", fontSize = 12.sp, color = colors.textSecondary)
                                                    }
                                                }
                                                
                                                Spacer(Modifier.height(12.dp))
                                                Text("Conheça as novidades e ofertas exclusivas dos nossos parceiros!", fontSize = 15.sp, color = colors.textPrimary)
                                                Spacer(Modifier.height(12.dp))
                                                
                                                AdMobBanner(
                                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                                    adSize = com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (index < feedItems.lastIndex) {
                                HorizontalDivider(color = colors.separator.copy(0.1f), thickness = 8.dp)
                            }
                        }
                    }
                }
            }

            // Overlays
            fullscreenImage?.let { imageUrl ->
                FullscreenImageViewer(url = imageUrl, onDismiss = { fullscreenImage = null })
            }

            if (sharingPost != null) {
                SharePostSheet(
                    post = sharingPost!!,
                    viewModel = viewModel,
                    onDismiss = { sharingPost = null }
                )
            }

            if (interactingPost != null) {
                InteractionsSheet(
                    post = interactingPost!!,
                    viewModel = viewModel,
                    onAuthorClick = onAuthorClick,
                    onDismiss = { interactingPost = null }
                )
            }

            // StatusViewer is now handled by the parent callback

            if (showCamera) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showCamera = false },
                    properties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        decorFitsSystemWindows = false
                    )
                ) {
                    InAppCameraView(
                        onDismiss = { showCamera = false },
                        onPhotoCaptured = { uri ->
                            selectedImageUris = selectedImageUris + uri
                            showCamera = false
                        },
                        onVideoCaptured = { uri ->
                            // Feed typically supports images, if it supports video, handle it here
                            selectedImageUris = selectedImageUris + uri
                            showCamera = false
                        }
                    )
                }
            }

            if (showCustomGallery) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showCustomGallery = false },
                    properties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        decorFitsSystemWindows = false
                    )
                ) {
                    ModernGalleryPicker(
                        viewModel = viewModel,
                        onDismiss = { showCustomGallery = false },
                        onSend = { uris ->
                            selectedImageUris = selectedImageUris + uris
                            showCustomGallery = false
                        }
                    )
                }
            }

            if (showMediaOptions) {
                MediaAttachmentSheet(
                    viewModel = viewModel,
                    isStatus = false,
                    isFeed = true,
                    onDismiss = { showMediaOptions = false },
                    onOpenCamera = { showCamera = true },
                    onOpenGallery = { showCustomGallery = true },
                    onOpenFile = { /* handled by launcher if needed */ },
                    onShareLocation = { /* handled if needed */ },
                    onMediaSelected = { uris ->
                        selectedImageUris = selectedImageUris + uris
                        showMediaOptions = false
                    }
                )
            }

            // Status Creation Overlays are now handled by the parent callback via onStatusAdd
            // Full Screen Notifications Overlay
            AnimatedVisibility(
                visible = showFullNotifications,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { width -> width }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { width -> width }),
                modifier = Modifier.fillMaxSize().zIndex(100f)
            ) {
                FeedNotificationsScreen(
                    viewModel = viewModel,
                    onNotificationClick = { postId ->
                        showFullNotifications = false
                        val postIndex = feedPosts.indexOfFirst { it.id == postId }
                        if (postIndex >= 0) {
                            scope.launch { listState.animateScrollToItem(postIndex + 2) }
                        }
                    },
                    onBack = { showFullNotifications = false }
                )
            }
        }
    }
}

@Composable
fun NotificationDropdownItem(notification: FeedNotification) {
    val colors = LocalChatColors.current
    Row(
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // PERFIL
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = notification.fromPhotoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colors.separator),
                contentScale = ContentScale.Crop
            )

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

            Surface(
                modifier = Modifier.size(19.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(11.dp))
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // CONTEÚDO
        Column(modifier = Modifier.weight(1f)) {
            val annotatedText = buildAnnotatedString {
                withStyle(style = SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
                    append(notification.fromName.ifBlank { stringResource(R.string.notification_someone) })
                }
                append(" ")
                val action = when (notification.type) {
                    "LIKE" -> stringResource(R.string.notification_action_liked)
                    "COMMENT" -> stringResource(R.string.notification_action_commented)
                    "REACTION" -> stringResource(R.string.notification_action_reacted)
                    "MENTION" -> stringResource(R.string.notification_action_mentioned)
                    else -> stringResource(R.string.notification_action_interacted)
                }
                withStyle(style = SpanStyle(color = colors.textPrimary.copy(0.8f), fontSize = 13.sp)) {
                    append(action)
                }
            }
            Text(text = annotatedText, maxLines = 2)
            Text(formatTimeSince(LocalContext.current, notification.timestamp), fontSize = 11.sp, color = colors.textSecondary)
        }

        // MINIATURA DO POST (NOVIDADE)
        if (!notification.postPhotoUrl.isNullOrEmpty()) {
            Spacer(Modifier.width(8.dp))
            AsyncImage(
                model = notification.postPhotoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.separator),
                contentScale = ContentScale.Crop
            )
        }

        // UNREAD DOT
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
            )
        }
    }
}

@Composable
fun EmptyFeedState(colors: com.jack.friend.ui.theme.ChatColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.Feed,
            null,
            modifier = Modifier.size(64.dp),
            tint = colors.textSecondary.copy(0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.feed_empty_state_title),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colors.textPrimary
        )
        Text(
            stringResource(R.string.feed_empty_state_subtitle),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun NewPostBox(
    myPhotoUrl: String?,
    postTextFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    selectedImageUris: List<Uri>,
    onImageRemove: (Uri) -> Unit,
    selectedEmoji: String?,
    onEmojiToggle: () -> Unit,
    onEmojiRemove: () -> Unit,
    onMediaOptionClick: () -> Unit,
    isPublic: Boolean,
    onPrivacyToggle: () -> Unit,
    onPost: () -> Unit,
    colors: com.jack.friend.ui.theme.ChatColors
) {
    Surface(
        color = colors.secondaryBackground,
        modifier = Modifier.fillMaxWidth().padding(bottom = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = myPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(colors.separator),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.background)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    if (postTextFieldValue.text.isEmpty()) {
                        Text(stringResource(R.string.post_hint), color = colors.textSecondary.copy(0.6f), fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = postTextFieldValue,
                        onValueChange = onTextFieldValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = colors.primary, fontSize = 14.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                        visualTransformation = MentionVisualTransformation(colors.primary)
                    )
                }
            }

            if (selectedImageUris.isNotEmpty() || selectedEmoji != null) {
                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(selectedImageUris) { uri ->
                        Box(modifier = Modifier.size(110.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onImageRemove(uri) },
                                modifier = Modifier.align(Alignment.TopEnd).size(26.dp).offset(x = (-4).dp, y = 4.dp).background(Color.Black.copy(0.6f), CircleShape)
                            ) {
                                Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    if (selectedEmoji != null) {
                        item {
                            Box(
                                modifier = Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(colors.background),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedEmoji(emoji = selectedEmoji, size = 64.dp)
                                IconButton(
                                    onClick = { onEmojiRemove() },
                                    modifier = Modifier.align(Alignment.TopEnd).size(26.dp).offset(x = (-4).dp, y = 4.dp).background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    PostActionIcon(modifier = Modifier.weight(1f), icon = Icons.Rounded.AddPhotoAlternate, color = colors.primary, label = stringResource(R.string.post_action_media), onClick = onMediaOptionClick)
                    PostActionIcon(modifier = Modifier.weight(1f), icon = Icons.Rounded.EmojiEmotions, color = Color(0xFFFFC107), label = stringResource(R.string.post_action_emoji), onClick = onEmojiToggle)
                    PostActionIcon(
                        modifier = Modifier.weight(1f),
                        icon = if (isPublic) Icons.Rounded.Public else Icons.Rounded.Lock,
                        color = if (isPublic) colors.primary else Color.Gray,
                        label = if (isPublic) stringResource(R.string.post_privacy_public) else stringResource(R.string.post_privacy_private),
                        onClick = onPrivacyToggle
                    )
                }

                Button(
                    onClick = onPost,
                    enabled = postTextFieldValue.text.isNotBlank() || selectedImageUris.isNotEmpty() || selectedEmoji != null,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(stringResource(R.string.post_button_post), fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun PostActionIcon(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = LocalChatColors.current.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AnimatedPostItem(
    post: FeedPost,
    isAuthorOnline: Boolean,
    myUsername: String,
    myPhotoUrl: String?,
    viewModel: ChatViewModel,
    onAuthorClick: (UserProfile) -> Unit,
    onImageClick: (String) -> Unit,
    onShareClick: () -> Unit,
    onInteractionsClick: (FeedPost) -> Unit = {}
) {
    val visibleState = remember { MutableTransitionState(false) }.apply { targetState = true }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(600, easing = EaseOutQuart)) + slideInVertically(initialOffsetY = { 80 }, animationSpec = tween(600, easing = EaseOutQuart))
    ) {
        FeedPostCard(
            post = post,
            isAuthorOnline = isAuthorOnline,
            myUsername = myUsername,
            myPhotoUrl = myPhotoUrl,
            viewModel = viewModel,
            onAuthorClick = onAuthorClick,
            onImageClick = { onImageClick(it) },
            onShareClick = onShareClick,
            onInteractionsClick = onInteractionsClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedPostCard(
    post: FeedPost,
    isAuthorOnline: Boolean,
    myUsername: String,
    myPhotoUrl: String?,
    viewModel: ChatViewModel,
    onAuthorClick: (UserProfile) -> Unit,
    onImageClick: (String) -> Unit,
    onShareClick: () -> Unit = {},
    onInteractionsClick: (FeedPost) -> Unit = {}
) {
    val colors = LocalChatColors.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val reactionEmojis = listOf("❤️", "😂", "😮", "😢", "😡", "👍")
    val hasLiked = post.likes.containsKey(myUsername)
    val likeCount = post.likes.size
    val reactions = post.reactions

    var showOptions by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var showReactionsPicker by remember { mutableStateOf(false) }
    var commentTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    var heartPulseScale by remember { mutableStateOf(1f) }
    val animatedHeartScale by animateFloatAsState(
        targetValue = heartPulseScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "HeartScale"
    )

    if (heartPulseScale == 1.6f) {
        LaunchedEffect(Unit) {
            delay(400)
            heartPulseScale = 1f
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(colors.bubbleCornerRadius),
        color = colors.secondaryBackground,
        shadowElevation = 0.5.dp,
        border = BorderStroke(0.5.dp, colors.separator.copy(0.1f))
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    AsyncImage(
                        model = post.authorPhotoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.dp, colors.separator.copy(0.1f), CircleShape)
                            .clickable {
                                onAuthorClick(UserProfile(id = post.authorId, name = post.authorName, photoUrl = post.authorPhotoUrl))
                            },
                        contentScale = ContentScale.Crop
                    )

                    if (isAuthorOnline) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(12.dp)
                                .offset(x = 2.dp, y = 2.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                                .border(2.dp, colors.secondaryBackground, CircleShape)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        post.authorName.ifBlank { post.authorId },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTimeSince(LocalContext.current, post.timestamp), fontSize = 12.sp, color = colors.textSecondary)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            if (post.isPublic) Icons.Rounded.Public else Icons.Rounded.Lock,
                            null,
                            modifier = Modifier.size(13.dp),
                            tint = colors.textSecondary.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(onClick = { showOptions = true }) {
                    Icon(Icons.Rounded.MoreHoriz, null, tint = colors.textSecondary)
                }

                if (showOptions) {
                    PostOptionsSheet(
                        post = post,
                        myUsername = myUsername,
                        viewModel = viewModel,
                        onDismiss = { showOptions = false },
                        onCopy = {
                            clipboard.setText(AnnotatedString(post.text))
                            showOptions = false
                        }
                    )
                }
            }

            // Text content
            if (post.text.isNotBlank()) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    var expanded by remember { mutableStateOf(false) }
                    val isLongText = post.text.length > 280

                    Column {
                        ClickableLinkText(
                            text = if (!expanded && isLongText) post.text.take(280) + "..." else post.text,
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            linkColor = colors.mention,
                            onMentionClick = { username ->
                                viewModel.fetchUserProfile(username) { profile ->
                                    if (profile != null) onAuthorClick(profile)
                                }
                            }
                        )
                        if (isLongText) {
                            Text(
                                if (expanded) stringResource(R.string.post_expand_less) else stringResource(R.string.post_expand_more),
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable { expanded = !expanded }.padding(top = 4.dp)
                            )
                        }
                    }
                }

                val urlMatcher = Patterns.WEB_URL.matcher(post.text)
                if (urlMatcher.find()) {
                    val foundUrl = urlMatcher.group()
                    if (foundUrl != null) {
                        val previewUrl = if (foundUrl.startsWith("http")) foundUrl else "https://$foundUrl"
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            FeedLinkPreviewCard(url = previewUrl, colors = colors)
                        }
                    }
                }
            }

            // Media content (Multiple Images)
            val images = if (post.photoUrls.isNotEmpty()) post.photoUrls else if (post.photoUrl != null) listOf(post.photoUrl!!) else emptyList()
            if (post.mediaType == "IMAGE_FEED" && images.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .heightIn(max = 520.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (images.size > 1) {
                        val pagerState = rememberPagerState(pageCount = { images.size })
                        Column {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth().height(420.dp)
                            ) { page ->
                                Box(
                                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (!hasLiked) {
                                                    viewModel.toggleFeedLike(post.id, true)
                                                    heartPulseScale = 1.6f
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                }
                                            },
                                            onTap = { onImageClick(images[page]) }
                                        )
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = images[page],
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            // Modern Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(images.size) { iteration ->
                                    val isSelected = pagerState.currentPage == iteration
                                    val indicatorWidth by animateDpAsState(if (isSelected) 18.dp else 6.dp, label = "width")
                                    Box(
                                        modifier = Modifier
                                            .padding(3.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) colors.primary else colors.separator.copy(0.4f))
                                            .height(6.dp)
                                            .width(indicatorWidth)
                                    )
                                }
                            }
                        }
                    } else {
                        AsyncImage(
                            model = images[0],
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (!hasLiked) {
                                                viewModel.toggleFeedLike(post.id, true)
                                                heartPulseScale = 1.6f
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            }
                                        },
                                        onTap = { onImageClick(images[0]) }
                                    )
                                },
                            contentScale = ContentScale.FillWidth
                        )
                    }

                    if (heartPulseScale > 1f) {
                        Box(modifier = Modifier.graphicsLayer(scaleX = animatedHeartScale, scaleY = animatedHeartScale)) {
                            WappiLikeIcon(filled = true, tint = Color.White.copy(alpha = 0.95f), size = 110.dp)
                        }
                    }
                }
            } else if (post.mediaType == "VIDEO_FEED" && images.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 12.dp, end = 12.dp)
                        .height(380.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    ModernVideoPlayer(
                        videoUrl = images[0],
                        modifier = Modifier.fillMaxSize(),
                        autoPlay = false,
                        loop = true,
                        showControls = true,
                        onDoubleTap = {
                            if (!hasLiked) {
                                viewModel.toggleFeedLike(post.id, true)
                                heartPulseScale = 1.6f
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }
                        }
                    )
                    
                    if (heartPulseScale > 1f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .graphicsLayer(scaleX = animatedHeartScale, scaleY = animatedHeartScale)
                        ) {
                            WappiLikeIcon(filled = true, tint = Color.White.copy(alpha = 0.95f), size = 110.dp)
                        }
                    }
                }
            }

            if (!post.animatedEmoji.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    AnimatedEmoji(emoji = post.animatedEmoji!!, size = 160.dp)
                }
            }

            // Reactions Summary
            if (likeCount > 0 || reactions.isNotEmpty() || post.comments.isNotEmpty() || post.shares.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (likeCount > 0 || reactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onInteractionsClick(post) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val combinedReactions = mutableListOf<String>()
                            if (hasLiked) combinedReactions.add("❤️")
                            reactions.values.distinct().take(3).forEach { combinedReactions.add(it) }

                            Box(modifier = Modifier.padding(end = 4.dp)) {
                                combinedReactions.distinct().take(3).forEachIndexed { i, emoji ->
                                    Text(
                                        text = emoji,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .offset(x = (i * 12).dp)
                                            .shadow(2.dp, CircleShape)
                                            .background(colors.background, CircleShape)
                                            .border(1.2.dp, colors.secondaryBackground, CircleShape)
                                            .padding(2.5.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width((combinedReactions.distinct().take(3).size * 10).dp))
                            Text(
                                text = "$likeCount",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    val commentCount = post.comments.size
                    val shareCount = post.shares.size

                    if (commentCount > 0 || shareCount > 0) {
                        Text(
                            text = buildString {
                                if (commentCount > 0) append(
                                    if (commentCount == 1) stringResource(R.string.post_comment_count_singular, commentCount)
                                    else stringResource(R.string.post_comment_count_plural, commentCount)
                                )
                                if (commentCount > 0 && shareCount > 0) append(" • ")
                                if (shareCount > 0) append(
                                    if (shareCount == 1) stringResource(R.string.post_share_count_singular, shareCount)
                                    else stringResource(R.string.post_share_count_plural, shareCount)
                                )
                            },
                            fontSize = 12.sp,
                            color = colors.textSecondary.copy(0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = colors.separator.copy(0.15f))

            // Action Bar (iOS Style)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Like / Reaction Button
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    viewModel.toggleFeedLike(post.id, !hasLiked)
                                    if (!hasLiked) heartPulseScale = 1.3f
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    showReactionsPicker = true
                                }
                            )
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Box(modifier = Modifier.graphicsLayer(
                                scaleX = if (hasLiked) animatedHeartScale else 1f,
                                scaleY = if (hasLiked) animatedHeartScale else 1f
                            )) {
                                WappiLikeIcon(
                                    filled = hasLiked,
                                    tint = if (hasLiked) Color(0xFFEF5350) else colors.textSecondary,
                                    size = 22.dp
                                )
                            }
                             Spacer(Modifier.width(6.dp))
                            val currentLikeCount = post.likes.filter { it.value }.size
                            Text(
                                if (currentLikeCount > 0) stringResource(R.string.post_action_like_count, currentLikeCount) else stringResource(R.string.post_action_like),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (hasLiked) Color(0xFFEF5350) else colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (showReactionsPicker) {
                        Popup(
                            alignment = Alignment.TopCenter,
                            offset = androidx.compose.ui.unit.IntOffset(0, -110),
                            onDismissRequest = { showReactionsPicker = false }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(32.dp),
                                color = colors.secondaryBackground,
                                shadowElevation = 12.dp,
                                border = BorderStroke(0.5.dp, colors.separator.copy(0.2f))
                            ) {
                                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    reactionEmojis.forEach { emoji ->
                                        Text(
                                            text = emoji,
                                            fontSize = 30.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.setFeedReaction(post.id, emoji)
                                                    showReactionsPicker = false
                                                }
                                                .padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { showComments = !showComments },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WappiCommentIcon(tint = colors.textSecondary, size = 22.dp)
                        Spacer(Modifier.width(6.dp))
                        val currentCommentCount = post.comments.size
                        Text(
                            if (currentCommentCount > 0) stringResource(R.string.post_action_comment_count, currentCommentCount) else stringResource(R.string.post_action_comment),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TextButton(
                    onClick = {
                        viewModel.trackFeedShare(post.id)
                        onShareClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WappiShareIcon(tint = colors.textSecondary, size = 22.dp)
                        Spacer(Modifier.width(6.dp))
                        val currentShareCount = post.shares.size
                        Text(
                            if (currentShareCount > 0) stringResource(R.string.post_action_send_count, currentShareCount) else stringResource(R.string.post_action_send),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Comments Section (Visual Threading)
            AnimatedVisibility(visible = showComments, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.background(colors.background.copy(alpha = 0.4f)).padding(bottom = 8.dp)) {
                    val commentList = post.comments.values.sortedBy { it.timestamp }
                    var replyingTo by remember { mutableStateOf<FeedComment?>(null) }

                    if (commentList.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Logic for grouping comments (1-level nesting for interlinking)
                            val topLevel = commentList.filter { it.replyToId == null }
                            val replies = commentList.filter { it.replyToId != null }.groupBy { it.replyToId }

                            topLevel.forEach { parentComment ->
                                CommentItem(
                                    postId = post.id,
                                    comment = parentComment,
                                    myUsername = myUsername,
                                    colors = colors,
                                    onAuthorClick = onAuthorClick,
                                    viewModel = viewModel,
                                    onReply = { replyingTo = parentComment }
                                )
                                
                                // Show replies visually interlinked
                                replies[parentComment.id]?.forEach { reply ->
                                    Box(modifier = Modifier.padding(start = 38.dp)) {
                                        CommentItem(
                                            postId = post.id,
                                            comment = reply,
                                            myUsername = myUsername,
                                            colors = colors,
                                            onAuthorClick = onAuthorClick,
                                            viewModel = viewModel,
                                            onReply = { replyingTo = parentComment },
                                            isReply = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column {
                        val suggestions by viewModel.mentionSuggestions.collectAsStateWithLifecycle()

                        MentionSuggestionsBox(
                            suggestions = suggestions,
                            onSuggestionClick = { user ->
                                val text = commentTextFieldValue.text
                                val cursor = commentTextFieldValue.selection.start
                                val query = getMentionQuery(text, cursor) ?: ""
                                val start = text.lastIndexOf(query, cursor - 1)
                                val newText = text.replaceRange(start, cursor, "@${user.id} ")
                                commentTextFieldValue = TextFieldValue(newText, TextRange(newText.length))
                                viewModel.updateMentionQuery("")
                            },
                            colors = colors
                        )

                        // Visual indicator for replyingTo
                        AnimatedVisibility(visible = replyingTo != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.primary.copy(alpha = 0.05f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Reply, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.post_replying_to, replyingTo?.authorName ?: ""),
                                        fontSize = 12.sp,
                                        color = colors.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Rounded.Close, null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = colors.background,
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, colors.separator.copy(0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = myPhotoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.padding(6.dp).size(32.dp).clip(CircleShape).background(colors.separator),
                                    contentScale = ContentScale.Crop
                                )

                                TextField(
                                    value = commentTextFieldValue,
                                    onValueChange = {
                                        commentTextFieldValue = it
                                        val query = getMentionQuery(it.text, it.selection.start)
                                        viewModel.updateMentionQuery(query ?: "")
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(stringResource(R.string.post_hint_comment), fontSize = 14.sp, color = colors.textSecondary.copy(0.6f)) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = colors.primary,
                                        unfocusedTextColor = colors.primary
                                    ),
                                    maxLines = 4,
                                    textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                                    visualTransformation = MentionVisualTransformation(colors.mention)
                                )

                                IconButton(
                                    onClick = {
                                        if (commentTextFieldValue.text.isNotBlank()) {
                                            viewModel.addFeedComment(
                                                post.id, 
                                                commentTextFieldValue.text.trim(),
                                                replyToId = replyingTo?.id,
                                                replyToName = replyingTo?.authorName
                                            )
                                            commentTextFieldValue = TextFieldValue("")
                                            replyingTo = null
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        }
                                    },
                                    enabled = commentTextFieldValue.text.isNotBlank(),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Send,
                                        null,
                                        tint = if (commentTextFieldValue.text.isNotBlank()) colors.primary else colors.textSecondary.copy(0.2f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentItem(
    postId: String,
    comment: FeedComment,
    myUsername: String,
    colors: com.jack.friend.ui.theme.ChatColors,
    onAuthorClick: (UserProfile) -> Unit,
    viewModel: ChatViewModel,
    onReply: (String) -> Unit = {},
    isReply: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val hasLiked = comment.likes.containsKey(myUsername)
    val likeCount = comment.likes.size
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    showMenu = true
                }
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            model = comment.authorPhotoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.separator.copy(0.2f))
                .clickable {
                    onAuthorClick(UserProfile(id = comment.authorId, name = comment.authorName, photoUrl = comment.authorPhotoUrl))
                },
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f)) {
            // Bubble
            Surface(
                color = colors.background.copy(alpha = 0.8f),
                shape = RoundedCornerShape(colors.bubbleCornerRadius),
                border = BorderStroke(0.5.dp, colors.separator.copy(0.1f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        comment.authorName,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = colors.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    ClickableLinkText(
                        text = comment.text,
                        color = colors.primary,
                        fontSize = 14.sp,
                        linkColor = colors.mention,
                        onMentionClick = { username ->
                            viewModel.fetchUserProfile(username) { profile ->
                                if (profile != null) onAuthorClick(profile)
                            }
                        }
                    )
                }
            }

            // Footer (Timestamp, Curtidas, Responder)
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = formatTimeSince(LocalContext.current, comment.timestamp),
                    fontSize = 11.sp,
                    color = colors.textSecondary.copy(0.7f),
                    fontWeight = FontWeight.Medium
                )

                if (likeCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Favorite, null, tint = Color.Red, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("$likeCount", fontSize = 11.sp, fontWeight = FontWeight.Black, color = colors.textSecondary)
                    }
                }

                Text(
                    stringResource(R.string.post_action_reply),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textSecondary.copy(0.8f),
                    modifier = Modifier.clickable { onReply(comment.authorId) }
                )
            }
        }

        // Like heart icon on the far right
        IconButton(
            onClick = { viewModel.toggleFeedCommentLike(postId, comment.id, !hasLiked) },
            modifier = Modifier.size(24.dp).align(Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = if (hasLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (hasLiked) Color.Red else colors.textSecondary.copy(0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(colors.secondaryBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.post_comment_menu_copy), fontWeight = FontWeight.Medium) },
                onClick = {
                    clipboard.setText(AnnotatedString(comment.text))
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(18.dp)) }
            )
            if (comment.authorId == myUsername) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.post_comment_menu_delete), fontWeight = FontWeight.Medium) },
                    onClick = {
                        viewModel.deleteFeedComment(postId, comment.id)
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = Color.Red, modifier = Modifier.size(18.dp)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsSheet(
    post: FeedPost,
    myUsername: String,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    val colors = LocalChatColors.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.separator.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Opções do Post",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            ActionOptionItem(
                text = "Copiar Texto",
                icon = Icons.Rounded.ContentCopy,
                iconBgColor = Color(0xFF007AFF).copy(alpha = 0.1f),
                iconColor = Color(0xFF007AFF),
                onClick = onCopy
            )

            ActionOptionItem(
                text = "Denunciar Post",
                icon = Icons.Rounded.Flag,
                iconBgColor = Color(0xFFFF9500).copy(alpha = 0.1f),
                iconColor = Color(0xFFFF9500),
                onClick = onDismiss
            )

            if (post.authorId == myUsername) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = colors.separator.copy(alpha = 0.05f), thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                ActionOptionItem(
                    text = "Apagar Post",
                    icon = Icons.Rounded.DeleteOutline,
                    iconBgColor = Color.Red.copy(alpha = 0.1f),
                    iconColor = Color.Red,
                    textColor = Color.Red,
                    onClick = { viewModel.deleteFeedPost(post.id); onDismiss() }
                )
            }
        }
    }
}

@Composable
fun ActionOptionItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    textColor: Color? = null,
    iconBgColor: Color? = null,
    iconColor: Color? = null
) {
    val colors = LocalChatColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBgColor ?: colors.separator.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor ?: colors.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = textColor ?: colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary.copy(alpha = 0.2f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun FullscreenImageViewer(url: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .background(Color.Black.copy(0.5f), CircleShape)
        ) {
            Icon(Icons.Rounded.Close, stringResource(R.string.action_close), tint = Color.White)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostSheet(
    post: FeedPost,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalChatColors.current
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Share, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.share_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary
                )
            }

            if (contacts.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.share_no_friends), color = colors.textSecondary)
                }
            } else {
                // Recent Contacts Row
                Text(
                    "Contatos Recentes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    items(contacts) { friend ->
                        Column(
                            modifier = Modifier
                                .width(70.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    viewModel.shareFeedPost(post, friend.id)
                                    android.widget.Toast.makeText(context, context.getString(R.string.share_toast_sent, friend.name), android.widget.Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = friend.photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(colors.separator.copy(0.2f)),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    color = colors.primary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(20.dp).border(2.dp, colors.background, CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                friend.name.split(" ").first(),
                                fontSize = 12.sp,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = colors.separator.copy(0.1f), thickness = 0.5.dp)
            Spacer(Modifier.height(24.dp))

            // External Share Option
            OutlinedButton(
                onClick = {
                    val shareText = context.getString(R.string.share_external_text, post.text, post.authorId)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_title)))
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.primary.copy(0.3f)),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Rounded.IosShare, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.share_external_action),
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }
        }
    }
}

@Composable
fun FeedLinkPreviewCard(url: String, colors: com.jack.friend.ui.theme.ChatColors) {
    data class PreviewData(val title: String, val description: String, val imageUrl: String?, val host: String)
    val uriHandler = LocalUriHandler.current
    var preview by remember(url) { mutableStateOf<PreviewData?>(null) }
    var loading by remember(url) { mutableStateOf(true) }

    LaunchedEffect(url) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val doc = org.jsoup.Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get()
                val title = doc.select("meta[property=og:title]").attr("content")
                    .ifBlank { doc.title() }
                val description = doc.select("meta[property=og:description]").attr("content")
                    .ifBlank { doc.select("meta[name=description]").attr("content") }
                val image = doc.select("meta[property=og:image]").attr("content")
                    .let { if (it.startsWith("http")) it else null }
                val host = java.net.URI(url).host?.removePrefix("www.") ?: ""
                preview = PreviewData(title, description, image, host)
            } catch (_: Exception) {}
            loading = false
        }
    }

    if (loading) return

    val p = preview ?: return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(colors.bubbleCornerRadius))
            .clickable { uriHandler.openUri(url) },
        color = colors.background,
        border = BorderStroke(0.5.dp, colors.separator.copy(alpha = 0.25f)),
        shadowElevation = 2.dp
    ) {
        Column {
            if (!p.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = p.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(p.host.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = colors.link, letterSpacing = 0.5.sp)
                if (p.title.isNotBlank()) {
                    Text(p.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (p.description.isNotBlank()) {
                    Text(p.description, fontSize = 14.sp, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun ClickableLinkText(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    linkColor: Color,
    onMentionClick: (String) -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val layoutResult = remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    val annotatedString = buildAnnotatedString {
        val combinedPattern = Pattern.compile("(@[a-zA-Z0-9_.-]+)|(${Patterns.WEB_URL.pattern()})")
        val matcher = combinedPattern.matcher(text)
        var lastIndex = 0
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val match = matcher.group()

            append(text.substring(lastIndex, start))

            if (match.startsWith("@")) {
                val username = match.substring(1).lowercase()
                pushStringAnnotation(tag = "MENTION", annotation = username)
                withStyle(style = SpanStyle(color = linkColor, fontWeight = FontWeight.Black)) {
                    append(match)
                }
                pop()
            } else {
                val url = if (match.startsWith("http")) match else "http://$match"
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)) {
                    append(match)
                }
                pop()
            }
            lastIndex = end
        }
        append(text.substring(lastIndex))
    }

    Text(
        text = annotatedString,
        color = color,
        fontSize = fontSize,
        onTextLayout = { layoutResult.value = it },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                layoutResult.value?.let { result ->
                    val position = result.getOffsetForPosition(offset)

                    annotatedString.getStringAnnotations(tag = "MENTION", start = position, end = position)
                        .firstOrNull()?.let { annotation ->
                            onMentionClick(annotation.item)
                            return@detectTapGestures
                        }

                    annotatedString.getStringAnnotations(tag = "URL", start = position, end = position)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                }
            }
        }
    )
}

fun formatTimeSince(context: android.content.Context, timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> context.getString(R.string.time_now)
        diff < 3600_000 -> context.getString(R.string.time_minutes, diff / 60_000)
        diff < 86400_000 -> context.getString(R.string.time_hours, diff / 3600_000)
        diff < 604800_000 -> context.getString(R.string.time_days, diff / 86400_000)
        else -> {
            val date = Date(timestamp)
            java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionsSheet(
    post: FeedPost,
    viewModel: ChatViewModel,
    onAuthorClick: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalChatColors.current
    val profiles = remember { mutableStateListOf<Pair<String, UserProfile?>>() }

    val allUsernames = (post.likes.keys + post.reactions.keys).distinct()

    LaunchedEffect(post) {
        allUsernames.forEach { username ->
            viewModel.fetchUserProfile(username) { profile ->
                if (profile != null) {
                    profiles.add(username to profile)
                } else {
                    profiles.add(username to UserProfile(id = username, name = username))
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 36.dp)) {
            Text(
                stringResource(R.string.feed_interactions_title),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = colors.primary
            )

            if (allUsernames.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.feed_no_interactions), color = colors.textSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                    items(allUsernames) { username ->
                        val pair = profiles.find { it.first == username }
                        val profile = pair?.second
                        val reaction = post.reactions[username]
                        val isLike = post.likes[username] == true

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (profile != null) {
                                        onDismiss()
                                        onAuthorClick(profile)
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = profile?.photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp).clip(CircleShape).background(colors.separator),
                                    contentScale = ContentScale.Crop
                                )
                                if (reaction != null || isLike) {
                                    Surface(
                                        modifier = Modifier.size(22.dp).offset(x = 6.dp, y = 6.dp),
                                        shape = CircleShape,
                                        color = Color.White,
                                        shadowElevation = 3.dp,
                                        border = BorderStroke(1.5.dp, colors.background)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                reaction ?: "❤️",
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(18.dp))
                            Text(
                                profile?.name ?: username,
                                fontWeight = FontWeight.Black,
                                color = colors.primary,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary.copy(0.4f), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

class MentionVisualTransformation(val linkColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val build = AnnotatedString.Builder()
        val str = text.text
        val pattern = Pattern.compile("@([a-zA-Z0-9_.-]+)")
        val matcher = pattern.matcher(str)

        var lastInx = 0
        while(matcher.find()) {
            build.append(str.substring(lastInx, matcher.start()))
            build.withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Bold)) {
                append(str.substring(matcher.start(), matcher.end()))
            }
            lastInx = matcher.end()
        }
        if (lastInx < str.length) {
            build.append(str.substring(lastInx))
        }

        return TransformedText(build.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
fun MentionSuggestionsBox(
    suggestions: List<UserProfile>,
    onSuggestionClick: (UserProfile) -> Unit,
    colors: com.jack.friend.ui.theme.ChatColors
) {
    if (suggestions.isEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp)),
        color = colors.secondaryBackground,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, colors.separator.copy(0.1f))
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            suggestions.forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(user) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(colors.separator),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primary)
                        Text("@${user.id}", fontSize = 12.sp, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

fun getMentionQuery(text: String, cursorPosition: Int): String? {
    if (cursorPosition <= 0 || text.isEmpty()) return null

    val sub = text.substring(0, cursorPosition)
    val lastAt = sub.lastIndexOf('@')

    if (lastAt == -1) return null

    // Check if there is a space between '@' and cursor
    val afterAt = sub.substring(lastAt + 1)
    if (afterAt.contains(' ')) return null

    return sub.substring(lastAt)
}
