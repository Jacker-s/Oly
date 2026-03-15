package com.jack.friend.ui.chat

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import com.jack.friend.FeedScreen
import com.jack.friend.ProfileActivity
import com.jack.friend.UserProfile
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.jack.friend.*
import com.jack.friend.ui.components.*
import com.jack.friend.ui.profile.IOS17ContactProfileSheet
import com.jack.friend.ui.screens.ChatListScreenIOS
import com.jack.friend.ui.screens.SearchScreen
import com.jack.friend.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Main screen for the chat functionality, handling both the conversation list and individual chat sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel, 
    billingManager: BillingManager? = null,
    activity: Activity? = null
) {
    val myUsername by viewModel.myUsername.collectAsStateWithLifecycle("")
    val myPhotoUrl by viewModel.myPhotoUrl.collectAsStateWithLifecycle(null)
    val targetId by viewModel.targetId.collectAsStateWithLifecycle("")
    val targetProfileState by viewModel.targetProfile.collectAsStateWithLifecycle(null)
    val messages by viewModel.messages.collectAsStateWithLifecycle(emptyList())
    val activeChats by viewModel.activeChats.collectAsStateWithLifecycle(emptyList())
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle(emptyList())
    val statuses by viewModel.statuses.collectAsStateWithLifecycle(emptyList())
    val isTargetTyping by viewModel.isTargetTyping.collectAsStateWithLifecycle(false)
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle(emptyList())
    val contacts by viewModel.contacts.collectAsStateWithLifecycle(emptyList())
    val recordingDuration by viewModel.recordingDuration.collectAsStateWithLifecycle(0L)
    val pinnedMessage by viewModel.pinnedMessage.collectAsStateWithLifecycle(null)
    val showReadReceipts by viewModel.showReadReceipts.collectAsStateWithLifecycle(true)

    // External sharing
    val pendingSharedMedia by viewModel.pendingSharedMedia.collectAsStateWithLifecycle()
    val pendingSharedText by viewModel.pendingSharedText.collectAsStateWithLifecycle()
    val hasPendingShare = pendingSharedMedia.isNotEmpty() || !pendingSharedText.isNullOrEmpty()
    var showShareConfirmDialog by remember { mutableStateOf<String?>(null) } // friendId

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var textState by remember { mutableStateOf("") }
    var searchInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var mediaViewerItem by remember { mutableStateOf<MediaViewerItem?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showInAppCamera by remember { mutableStateOf(false) }
    var showModernGallery by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var tempMessageDuration by remember { mutableLongStateOf(0L) }
    var showTempMessageSelector by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showChatInfo by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var viewingStatuses by remember { mutableStateOf<List<UserStatus>?>(null) }
    var selectedFilter by remember { mutableStateOf("Tudo") }
    var selectedChatForOptions by remember { mutableStateOf<ChatSummary?>(null) }
    val bottomScreens = remember { listOf(BottomBarScreen.Home, BottomBarScreen.Feed, BottomBarScreen.Contacts, BottomBarScreen.Calls, BottomBarScreen.Settings) }
    var currentBottomRoute by remember { mutableStateOf(BottomBarScreen.Home.route) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { bottomScreens.size })

    val openFeed by viewModel.openFeed.collectAsStateWithLifecycle(false)
    val openPostId by viewModel.openPostId.collectAsStateWithLifecycle(null)

    LaunchedEffect(openFeed, openPostId) {
        if (openFeed) {
            currentBottomRoute = BottomBarScreen.Feed.route
            val targetIndex = bottomScreens.indexOf(BottomBarScreen.Feed)
            if (targetIndex != -1) pagerState.animateScrollToPage(targetIndex)
            // After navigating, reset openFeed state so it doesn't get stuck
            viewModel.setOpenFeed(false)
        }
    }

    // State to open profile from search
    var searchingUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    var activePopupNotification by remember { mutableStateOf<FeedNotification?>(null) }

    LaunchedEffect(Unit) {
        viewModel.latestNotification.collect { notification ->
            activePopupNotification = notification
        }
    }

    LaunchedEffect(activePopupNotification) {
        if (activePopupNotification != null) {
            delay(5000)
            activePopupNotification = null
        }
    }

    // Status Composer State
    var statusItemsToCompose by remember { mutableStateOf<SnapshotStateList<StatusDraft>?>(null) }
    var showStatusAttachmentMenu by remember { mutableStateOf(false) }
    var showInAppStatusCamera by remember { mutableStateOf(false) }
    var showInAppStatusGallery by remember { mutableStateOf(false) }

    val filteredChats by remember(activeChats, selectedFilter) {
        derivedStateOf { if (selectedFilter == "Não Lidas") activeChats.filter { it.hasUnread } else activeChats }
    }

    // Ensure screenshots are always allowed
    LaunchedEffect(Unit) {
        val currentActivity = context as? Activity ?: return@LaunchedEffect
        currentActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    LaunchedEffect(currentBottomRoute) {
        if (currentBottomRoute != BottomBarScreen.Search.route) {
            val index = bottomScreens.indexOfFirst { it.route == currentBottomRoute }
            if (index != -1 && pagerState.currentPage != index) {
                pagerState.animateScrollToPage(index)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val page = bottomScreens[pagerState.currentPage]
            if (currentBottomRoute != page.route && currentBottomRoute != BottomBarScreen.Search.route) {
                currentBottomRoute = page.route
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            if (targetId.isNotEmpty()) {
                viewModel.markAsRead()
            }
        }
    }

    LaunchedEffect(targetId) {
        if (targetId.isNotEmpty()) {
            viewModel.markAsRead()
            FriendApplication.currentOpenedChatId = targetId
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(targetId.hashCode())
        } else {
            FriendApplication.currentOpenedChatId = "LISTA_CONVERSAS"
        }
    }

    LaunchedEffect(targetId, activeChats) {
        if (targetId.isNotEmpty()) {
            val currentChat = activeChats.find { it.friendId == targetId }
            tempMessageDuration = currentChat?.tempDuration ?: 0L
        }
    }

    LaunchedEffect(hasPendingShare, targetId) {
        if (hasPendingShare && targetId.isNotEmpty()) {
            showShareConfirmDialog = targetId
        }
    }

    val callLogic = { isVideo: Boolean ->
        val uniqueRoomId = "Call_${UUID.randomUUID().toString().take(8)}"
        viewModel.startCall(isVideo = isVideo, customRoomId = uniqueRoomId)
        val currentChat = activeChats.find { it.friendId == targetId }
        context.startActivity(Intent(context, CallActivity::class.java).apply {
            putExtra("roomId", uniqueRoomId)
            putExtra("targetId", targetId)
            putExtra("targetPhotoUrl", targetProfileState?.photoUrl ?: currentChat?.friendPhotoUrl)
            putExtra("isOutgoing", true)
            putExtra("isVideo", isVideo)
        })
    }

    BackHandler(enabled = targetId.isNotEmpty() || isSearching || mediaViewerItem != null || currentBottomRoute != BottomBarScreen.Home.route || viewingStatuses != null || showEmojiPicker || showStickerPicker || showChatInfo || showInAppCamera || showModernGallery || showAttachmentMenu || searchingUserProfile != null || showTempMessageSelector || statusItemsToCompose != null || showInAppStatusCamera || showInAppStatusGallery) {
        when {
            statusItemsToCompose != null -> statusItemsToCompose = null
            showInAppStatusCamera -> showInAppStatusCamera = false
            showInAppStatusGallery -> showInAppStatusGallery = false
            showTempMessageSelector -> showTempMessageSelector = false
            searchingUserProfile != null -> searchingUserProfile = null
            showInAppCamera -> showInAppCamera = false
            showModernGallery -> showModernGallery = false
            showAttachmentMenu -> showAttachmentMenu = false
            showEmojiPicker -> showEmojiPicker = false
            showStickerPicker -> showStickerPicker = false
            showChatInfo -> showChatInfo = false
            viewingStatuses != null -> viewingStatuses = null
            mediaViewerItem != null -> mediaViewerItem = null
            targetId.isNotEmpty() -> viewModel.setTargetId("")
            isSearching -> {
                isSearching = false
                searchInput = ""
                viewModel.searchUsers("")
                currentBottomRoute = BottomBarScreen.Home.route
            }
            currentBottomRoute != BottomBarScreen.Home.route -> {
                currentBottomRoute = BottomBarScreen.Home.route
                selectedFilter = "Tudo"
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (viewingStatuses == null && !showChatInfo && !showInAppCamera && !showModernGallery && mediaViewerItem == null && searchingUserProfile == null && statusItemsToCompose == null && !showInAppStatusCamera && !showInAppStatusGallery) {
                if (targetId.isNotEmpty() || currentBottomRoute == BottomBarScreen.Home.route || currentBottomRoute == BottomBarScreen.Search.route) {
                    ChatTopBar(
                        targetId = targetId, targetProfile = targetProfileState, activeChats = activeChats, myPhotoUrl = myPhotoUrl,
                        isTargetTyping = isTargetTyping, showContacts = false, isSearching = isSearching, searchInput = searchInput,
                        onBack = { if (targetId.isNotEmpty()) viewModel.setTargetId("") else { isSearching = false; currentBottomRoute = BottomBarScreen.Home.route } },
                        onSearchChange = { searchInput = it; viewModel.searchUsers(it) },
                        onSearchActiveChange = { isSearching = it; if (!it) currentBottomRoute = BottomBarScreen.Home.route },
                        onCallClick = { callLogic(false) }, onVideoCallClick = { callLogic(true) },
                        onOptionClick = { showOptionsMenu = true },
                        onAddContact = { showAddContactDialog = true },
                        onChatHeaderClick = { showChatInfo = true }
                    )
                }
            }
        },
        bottomBar = {
            if (viewingStatuses == null && !showChatInfo && !showInAppCamera && !showModernGallery && mediaViewerItem == null && searchingUserProfile == null && statusItemsToCompose == null && !showInAppStatusCamera && !showInAppStatusGallery) {
                if (targetId.isNotBlank()) {
                    Column {
                        ChatInputSection(
                            textState = textState, onTextChange = { textState = it; viewModel.setTyping(it.isNotEmpty()) },
                            replyingTo = replyingTo, editingMessage = editingMessage, pinnedMessage = pinnedMessage, recordingDuration = recordingDuration,
                            onSend = {
                                if (textState.isNotBlank()) {
                                    val currentEditingMessage = editingMessage
                                    if (currentEditingMessage != null) viewModel.editMessage(currentEditingMessage, textState)
                                    else viewModel.sendMessage(textState, tempMessageDuration, replyingTo)
                                    textState = ""
                                    replyingTo = null
                                    editingMessage = null
                                    showEmojiPicker = false
                                    showStickerPicker = false
                                }
                            },
                            onAddClick = { showAttachmentMenu = true }, onCameraClick = { showInAppCamera = true },
                            onAudioStart = { viewModel.startRecording(context.cacheDir) },
                            onAudioStop = { cancel -> viewModel.stopRecording(tempMessageDuration, cancel) },
                            onEmojiClick = {
                                if (showEmojiPicker) {
                                    keyboardController?.show()
                                    showEmojiPicker = false
                                } else {
                                    keyboardController?.hide()
                                    showEmojiPicker = true
                                    showStickerPicker = false
                                }
                            },
                            onStickerClick = {
                                if (showStickerPicker) {
                                    keyboardController?.show()
                                    showStickerPicker = false
                                } else {
                                    keyboardController?.hide()
                                    showStickerPicker = true
                                    showEmojiPicker = false
                                }
                            },
                            onCancelReply = { replyingTo = null; editingMessage = null; if (editingMessage != null) textState = "" },
                            onUnpin = { viewModel.unpinMessage() },
                            onPinnedClick = { msg ->
                                val index = messages.indexOfFirst { it.id == msg.id }
                                if (index != -1) {
                                    scope.launch { listState.animateScrollToItem(index) }
                                }
                            }
                        )
                        AnimatedVisibility(visible = showEmojiPicker) { MetaEmojiPickerPro(onEmojiSelected = { textState += it }, heightDp = 290) }
                        AnimatedVisibility(visible = showStickerPicker) {
                            StickerPicker(
                                onStickerSelected = {
                                    viewModel.sendSticker(it, replyingTo)
                                    replyingTo = null
                                    showStickerPicker = false
                                },
                                heightDp = 290
                            )
                        }
                    }
                } else {
                    ResponsiveFloatingDock(
                        currentRoute = currentBottomRoute,
                        pagerOffset = if (!isSearching) (pagerState.currentPage + pagerState.currentPageOffsetFraction) else null,
                        onNavigate = { screen ->
                            currentBottomRoute = screen.route
                            isSearching = screen == BottomBarScreen.Search
                            if (screen == BottomBarScreen.Home) selectedFilter = "Tudo"
                        },
                        onFabClick = { }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                targetId.isNotEmpty() -> {
                    MessageListContent(
                        messages = messages, listState = listState, myUsername = myUsername, targetProfile = targetProfileState,
                        showReadReceipts = showReadReceipts,
                        onImageClick = { url -> mediaViewerItem = MediaViewerItem.Image(url) },
                        onVideoClick = { url -> mediaViewerItem = MediaViewerItem.Video(url) },
                        onDelete = { m -> viewModel.deleteMessage(m.id, if (m.senderId == myUsername) m.receiverId else m.senderId) },
                        onReply = { m -> replyingTo = m }, onReact = { m, e -> viewModel.addReaction(m, e) },
                        onEdit = { m -> editingMessage = m; textState = m.text }, onPin = { m -> viewModel.pinMessage(m) },
                        onAudioPlayed = { m -> viewModel.markAudioAsPlayed(m.id) }
                    )

                    val targetProfile = targetProfileState
                    if (showChatInfo && targetProfile != null) {
                        val currentChat = activeChats.find { it.friendId == targetId }
                        IOS17ContactProfileSheet(
                            viewModel = viewModel,
                            user = targetProfile,
                            myUsername = myUsername,
                            isMuted = currentChat?.isMuted ?: false,
                            isBlocked = blockedUsers.contains(targetId),
                            onDismiss = { showChatInfo = false },
                            onMessage = {
                                showChatInfo = false
                                viewModel.setTargetId(it.id)
                            },
                            onAudioCall = { callLogic(false) },
                            onVideoCall = { callLogic(true) },
                            onToggleMute = { viewModel.toggleMuteChat(targetProfile.id, currentChat?.isMuted ?: false) },
                            onToggleBlock = { if (blockedUsers.contains(targetId)) viewModel.unblockUser(targetId) else viewModel.blockUser(targetId) },
                            onRemove = { profile -> viewModel.deleteContact(profile.id) { s, _ -> if (s) { showChatInfo = false; viewModel.setTargetId("") } } }
                        )
                    }

                    if (showOptionsMenu) {
                        val currentChat = activeChats.find { it.friendId == targetId }
                        ModalBottomSheet(
                            onDismissRequest = { showOptionsMenu = false },
                            containerColor = LocalChatColors.current.secondaryBackground,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        ) {
                            ChatOptionsMenuSheet(
                                isMuted = currentChat?.isMuted ?: false,
                                isPinned = currentChat?.isPinned ?: false,
                                tempMessageDuration = tempMessageDuration,
                                isBlocked = blockedUsers.contains(targetId),
                                onDismiss = { showOptionsMenu = false },
                                onViewInfo = { showChatInfo = true },
                                onToggleMute = { viewModel.toggleMuteChat(targetId, currentChat?.isMuted ?: false) },
                                onTogglePin = { viewModel.togglePinChat(targetId, currentChat?.isPinned ?: false) },
                                onToggleTempMessages = { showTempMessageSelector = true },
                                onClearChat = { showClearChatDialog = true },
                                onBlockToggle = { if (blockedUsers.contains(targetId)) viewModel.unblockUser(targetId) else viewModel.blockUser(targetId) },
                                onSendTestAd = { viewModel.sendTestAd() }
                            )
                        }
                    }

                    if (showTempMessageSelector) {
                        ModalBottomSheet(
                            onDismissRequest = { showTempMessageSelector = false },
                            containerColor = LocalChatColors.current.secondaryBackground,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        ) {
                            TempMessageSelectorSheet(
                                currentDuration = tempMessageDuration,
                                onSelect = {
                                    viewModel.setTempMessageDuration(targetId, it)
                                    tempMessageDuration = it
                                    showTempMessageSelector = false
                                }
                            )
                        }
                    }
                }
                else -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !isSearching // Disable swipe when searching
                    ) { page ->
                        when(bottomScreens[page]) {
                            BottomBarScreen.Home -> {
                                if (isSearching) {
                                    SearchScreen(
                                        viewModel = viewModel,
                                        searchInput = searchInput,
                                        onUserClick = { user -> searchingUserProfile = user },
                                        onChatClick = { user ->
                                            isSearching = false
                                            searchInput = ""
                                            viewModel.searchUsers("")
                                            viewModel.setTargetId(user.id)
                                        },
                                        onAddContact = { id -> viewModel.addContact(id) { _, _ -> } }
                                    )
                                } else {
                                    Column {
                                        if (hasPendingShare) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(12.dp),
                                                tonalElevation = 4.dp
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Rounded.Share, null, tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(Modifier.width(12.dp))
                                                    Text("Conteúdo pronto para compartilhar. Selecione um amigo.", Modifier.weight(1f), fontSize = 14.sp)
                                                    TextButton(onClick = { viewModel.clearPendingShare() }) {
                                                        Text("Cancelar", color = iOSRed)
                                                    }
                                                }
                                            }
                                        }
                                        ChatListScreenIOS(
                                            isSearching = isSearching && (currentBottomRoute == BottomBarScreen.Home.route || currentBottomRoute == BottomBarScreen.Search.route),
                                            searchInput = searchInput, searchResults = searchResults, filteredChats = filteredChats, statuses = statuses,
                                            myPhotoUrl = myPhotoUrl, myUsername = myUsername, contacts = contacts,
                                            onStatusAdd = { showStatusAttachmentMenu = true },
                                            onStatusView = { userStatuses -> viewModel.markStatusAsViewed(userStatuses.first().id); viewingStatuses = userStatuses },
                                            onChatClick = { summary ->
                                                if (hasPendingShare) showShareConfirmDialog = summary.friendId
                                                else viewModel.setTargetId(summary.friendId)
                                            },
                                            onChatLongClick = { summary -> selectedChatForOptions = summary },
                                            onUserSearchClick = { user ->
                                                searchingUserProfile = user
                                            },
                                            onAddContactSearch = { id -> viewModel.addContact(id) { _, _ -> } },
                                            onUserChatClick = { user ->
                                                isSearching = false
                                                searchInput = ""
                                                viewModel.searchUsers("")
                                                viewModel.setTargetId(user.id)
                                            }
                                        )
                                    }
                                }
                            }
                            BottomBarScreen.Feed -> {
                                FeedScreen(
                                    viewModel = viewModel,
                                    billingManager = billingManager,
                                    onAuthorClick = { user ->
                                        if (user.id == myUsername) {
                                            context.startActivity(Intent(context, ProfileActivity::class.java))
                                        } else {
                                            // Show partially filled profile while loading the full one
                                            searchingUserProfile = user
                                            // Update with full profile from DB
                                            viewModel.fetchUserProfile(user.id) { fullProfile ->
                                                if (fullProfile != null) {
                                                    searchingUserProfile = fullProfile
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            BottomBarScreen.Contacts -> {
                                ContactsScreenIOS17(
                                    viewModel = viewModel,
                                    onBack = { currentBottomRoute = BottomBarScreen.Home.route },
                                    onOpenChat = { user -> viewModel.setTargetId(user.id) },
                                    onStartCall = { user, isVideo ->
                                        viewModel.setTargetId(user.id)
                                        callLogic(isVideo)
                                    }
                                )
                            }
                            BottomBarScreen.Calls -> {
                                CallsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentBottomRoute = BottomBarScreen.Home.route },
                                    onOpenCall = { roomId, targetIdCall, targetPhotoUrl, isOutgoing, isVideo ->
                                        context.startActivity(Intent(context, CallActivity::class.java).apply {
                                            putExtra("roomId", roomId)
                                            putExtra("targetId", targetIdCall)
                                            putExtra("targetPhotoUrl", targetPhotoUrl)
                                            putExtra("isOutgoing", isOutgoing)
                                            putExtra("isVideo", isVideo)
                                        })
                                    }
                                )
                            }
                            BottomBarScreen.Settings -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    billingManager = billingManager,
                                    onBack = { currentBottomRoute = BottomBarScreen.Home.route },
                                    activity = activity
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            if (showAttachmentMenu) MediaAttachmentSheet(viewModel = viewModel, onDismiss = { showAttachmentMenu = false }, onOpenCamera = { showInAppCamera = true }, onOpenGallery = { showModernGallery = true }, onMediaSelected = { uris -> uris.forEach { viewModel.uploadImage(it, tempMessageDuration) } }, onOpenFile = { })
            if (showInAppCamera) Box(modifier = Modifier.fillMaxSize().zIndex(10f)) { InAppCameraView(onDismiss = { showInAppCamera = false }, onPhotoCaptured = { uri -> viewModel.uploadImage(uri, tempMessageDuration) }, onVideoCaptured = { uri -> viewModel.uploadVideo(uri, tempMessageDuration) }) }
            if (showModernGallery) Box(modifier = Modifier.fillMaxSize().zIndex(10f)) { ModernGalleryPicker(viewModel = viewModel, onDismiss = { showModernGallery = false }, onSend = { uris -> uris.forEach { u -> viewModel.uploadImage(u, tempMessageDuration) } }) }

            if (showStatusAttachmentMenu) MediaAttachmentSheet(viewModel = viewModel, onDismiss = { showStatusAttachmentMenu = false }, onOpenCamera = { showInAppStatusCamera = true }, onOpenGallery = { showInAppStatusGallery = true }, onMediaSelected = { uris -> statusItemsToCompose = mutableStateListOf<StatusDraft>().apply { uris.forEach { add(StatusDraft(it)) } }; showStatusAttachmentMenu = false }, onOpenFile = { })
            if (showInAppStatusCamera) Box(modifier = Modifier.fillMaxSize().zIndex(11f)) { InAppCameraView(onDismiss = { showInAppStatusCamera = false }, onPhotoCaptured = { statusItemsToCompose = mutableStateListOf(StatusDraft(it)); showInAppStatusCamera = false }, onVideoCaptured = { statusItemsToCompose = mutableStateListOf(StatusDraft(it)); showInAppStatusCamera = false }) }
            if (showInAppStatusGallery) Box(modifier = Modifier.fillMaxSize().zIndex(11f)) { ModernGalleryPicker(viewModel = viewModel, onDismiss = { showInAppStatusGallery = false }, onSend = { uris -> statusItemsToCompose = mutableStateListOf<StatusDraft>().apply { uris.forEach { add(StatusDraft(it)) } }; showInAppStatusGallery = false }) }

            if (statusItemsToCompose != null) {
                StatusComposer(
                    statusItems = statusItemsToCompose!!,
                    onDismiss = { statusItemsToCompose = null },
                    onPost = { finalStatusList ->
                        finalStatusList.forEach { statusDraft ->
                            viewModel.uploadStatus(statusDraft.uri, statusDraft.caption, statusDraft.overlays.toList())
                        }
                        statusItemsToCompose = null
                    }
                )
            }

            MediaViewerScreen(mediaItem = mediaViewerItem, onDismiss = { mediaViewerItem = null })

            if (showClearChatDialog) AlertDialog(onDismissRequest = { showClearChatDialog = false }, title = { Text("Limpar Conversa") }, text = { Text("Isso apagará todas as mensagens para ambos.") }, confirmButton = { TextButton(onClick = { viewModel.clearChat(targetId); showClearChatDialog = false }) { Text("Limpar", color = iOSRed) } }, dismissButton = { TextButton(onClick = { showClearChatDialog = false }) { Text("Cancelar") } })
            if (showAddContactDialog) AddContactDialog(icon = Icons.Default.Person, onDismiss = { showAddContactDialog = false }, onAdd = { u -> viewModel.addContact(u) { s, e -> if (s) showAddContactDialog = false else Toast.makeText(context, e ?: "Erro", Toast.LENGTH_SHORT).show() } })

            val currentSelectedChat = selectedChatForOptions
            if (currentSelectedChat != null) {
                ChatPopUpMenu(
                    summary = currentSelectedChat,
                    isBlocked = blockedUsers.contains(currentSelectedChat.friendId),
                    onDismiss = { selectedChatForOptions = null },
                    onOpen = { summary -> viewModel.setTargetId(summary.friendId) },
                    onClear = { summary -> viewModel.clearChat(summary.friendId) },
                    onDelete = { summary -> viewModel.deleteChat(summary.friendId) },
                    onBlockToggle = { friendId -> if (blockedUsers.contains(friendId)) viewModel.unblockUser(friendId) else viewModel.blockUser(friendId) },
                    onTogglePin = { id, pinned -> viewModel.togglePinChat(id, pinned) },
                    onToggleMute = { id, muted -> viewModel.toggleMuteChat(id, muted) }
                )
            }

            val currentViewingStatuses = viewingStatuses
            if (currentViewingStatuses != null) {
                StatusViewer(userStatuses = currentViewingStatuses, myUsername = myUsername, viewModel = viewModel, onClose = { viewingStatuses = null }, onDelete = { id -> viewModel.deleteStatus(id) })
            }

            val shareTargetId = showShareConfirmDialog
            if (shareTargetId != null) {
                val shareTargetName = (activeChats.find { it.friendId == shareTargetId }?.friendName
                    ?: contacts.find { it.id == shareTargetId }?.name
                    ?: shareTargetId)

                AlertDialog(
                    onDismissRequest = { showShareConfirmDialog = null },
                    title = { Text("Compartilhar com $shareTargetName?") },
                    text = { Text("Deseja enviar o conteúdo selecionado para este amigo?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.sendPendingShare(shareTargetId, tempMessageDuration)
                            showShareConfirmDialog = null
                            if (targetId.isEmpty()) viewModel.setTargetId(shareTargetId)
                        }) { Text("Enviar", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showShareConfirmDialog = null }) { Text("Cancelar") }
                    }
                )
            }

            if (searchingUserProfile != null) {
                val user = searchingUserProfile!!
                val isContact = contacts.any { it.id == user.id }
                val chat = activeChats.firstOrNull { !it.isGroup && it.friendId == user.id }

                IOS17ContactProfileSheet(
                    viewModel = viewModel,
                    user = user,
                    myUsername = myUsername,
                    isMuted = chat?.isMuted ?: false,
                    isBlocked = blockedUsers.contains(user.id),
                    onDismiss = { searchingUserProfile = null },
                    onMessage = {
                        searchingUserProfile = null
                        isSearching = false
                        viewModel.setTargetId(it.id)
                    },
                    onAudioCall = {
                        if (blockedUsers.contains(it.id)) {
                            Toast.makeText(context, "Desbloqueie para ligar", Toast.LENGTH_SHORT).show()
                        } else {
                            searchingUserProfile = null
                            isSearching = false
                            viewModel.setTargetId(it.id)
                            callLogic(false)
                        }
                    },
                    onVideoCall = {
                        if (blockedUsers.contains(it.id)) {
                            Toast.makeText(context, "Desbloqueie para ligar", Toast.LENGTH_SHORT).show()
                        } else {
                            searchingUserProfile = null
                            isSearching = false
                            viewModel.setTargetId(it.id)
                            callLogic(true)
                        }
                    },
                    onToggleMute = { viewModel.toggleMuteChat(user.id, chat?.isMuted ?: false) },
                    onToggleBlock = {
                        if (blockedUsers.contains(user.id)) viewModel.unblockUser(user.id)
                        else viewModel.blockUser(user.id)
                    },
                    onRemove = { profile ->
                        viewModel.deleteContact(profile.id) { s, _ ->
                            if (s) searchingUserProfile = null
                        }
                    },
                    isContact = isContact,
                    onAddContact = { profile ->
                        viewModel.addContact(profile.id) { s, e ->
                            if (!s) Toast.makeText(context, e ?: "Erro", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp, start = 8.dp, end = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = activePopupNotification != null,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    activePopupNotification?.let { notification ->
                        NotificationPopup(
                            notification = notification,
                            onDismiss = { activePopupNotification = null },
                            onClick = {
                                activePopupNotification = null
                                currentBottomRoute = BottomBarScreen.Feed.route
                                scope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationPopup(
    notification: FeedNotification,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    val colors = LocalChatColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = colors.secondaryBackground.copy(alpha = 0.98f),
        tonalElevation = 12.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, colors.separator.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = notification.fromPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.separator),
                    contentScale = ContentScale.Crop
                )

                // Pequeno badge do tipo de notificação
                val icon = when (notification.type) {
                    "LIKE" -> Icons.Rounded.Favorite
                    "COMMENT" -> Icons.Rounded.Comment
                    "REACTION" -> Icons.Rounded.AddReaction
                    else -> Icons.Rounded.Notifications
                }
                val iconColor = when (notification.type) {
                    "LIKE" -> Color.Red
                    "COMMENT" -> MessengerBlue
                    "REACTION" -> Color(0xFFFF9800)
                    else -> colors.primary
                }

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(12.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append(notification.fromName.ifBlank { "Alguém" })
                        when (notification.type) {
                            "LIKE" -> append(" curtiu sua postagem")
                            "COMMENT" -> append(" comentou na sua postagem")
                            "REACTION" -> append(" reagiu ${notification.reactionEmoji} à sua postagem")
                            else -> append(" interagiu com você")
                        }
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    maxLines = 1
                )
                if (notification.postPreviewText.isNotBlank()) {
                    Text(
                        notification.postPreviewText,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = colors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * A full-screen composer for user statuses (similar to stories), with native-like caption input and floating text overlays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusComposer(
    statusItems: SnapshotStateList<StatusDraft>,
    onDismiss: () -> Unit,
    onPost: (List<StatusDraft>) -> Unit
) {
    var currentStatusIndex by remember { mutableIntStateOf(0) }
    val currentStatusDraft = statusItems[currentStatusIndex]

    var draftCaption by remember(currentStatusIndex) { mutableStateOf(currentStatusDraft.caption) }

    var showTextEditor by remember { mutableStateOf(false) }
    var currentOverlayText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var selectedFont by remember { mutableStateOf("Default") }

    var showEmojiStickerPicker by remember { mutableStateOf(false) }

    val fonts = listOf("Default", "Serif", "Monospace", "Cursive")
    val colors = listOf(Color.White, Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan)

    val context = LocalContext.current
    val isVideo = context.contentResolver.getType(currentStatusDraft.uri)?.startsWith("video") == true
    var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .zIndex(20f)
            .onGloballyPositioned { containerSize = it.size }
    ) {
        if (isVideo) {
            VideoStatusPlayer(url = currentStatusDraft.uri.toString(), onComplete = {}, isPaused = showTextEditor)
        } else {
            // Premium Blurred Background Base Layer
            AsyncImage(
                model = currentStatusDraft.uri,
                contentDescription = "Blurred Background",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.6f)
                    .blur(60.dp),
                contentScale = ContentScale.Crop
            )
            // Foreground Focused Image Layer
            AsyncImage(
                model = currentStatusDraft.uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(0.85f))))
        )

        currentStatusDraft.overlays.forEachIndexed { index, overlay ->
            var offsetX by remember(currentStatusIndex, index) { mutableFloatStateOf(overlay.x * containerSize.width) }
            var offsetY by remember(currentStatusIndex, index) { mutableFloatStateOf(overlay.y * containerSize.height) }

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(currentStatusIndex, index) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            currentStatusDraft.overlays[index] = overlay.copy(
                                x = if (containerSize.width > 0) offsetX / containerSize.width else 0.5f,
                                y = if (containerSize.height > 0) offsetY / containerSize.height else 0.5f
                            )
                        }
                    }
                    .padding(8.dp)
            ) {
                if (overlay.isAnimated && overlay.stickerUrl != null) {
                    AnimatedEmoji(emoji = overlay.stickerUrl!!, modifier = Modifier.size(100.dp))
                } else {
                    Text(
                        text = overlay.text,
                        color = Color(overlay.color),
                        fontSize = overlay.fontSize.sp,
                        fontFamily = when(overlay.fontStyle) {
                            "Serif" -> FontFamily.Serif
                            "Monospace" -> FontFamily.Monospace
                            "Cursive" -> FontFamily.Cursive
                            else -> FontFamily.Default
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.background(Color.Black.copy(0.3f), RoundedCornerShape(4.dp)).padding(4.dp)
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White)
                }
            }

            Row(
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showEmojiStickerPicker = true },
                    modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Face, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        currentOverlayText = ""
                        showTextEditor = true
                    },
                    modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)
                ) {
                    Icon(Icons.Rounded.TextFields, null, tint = Color.White)
                }
            }

            if (statusItems.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 4.dp, start = 8.dp, end = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statusItems.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(if (index == currentStatusIndex) Color.White else Color.White.copy(alpha = 0.4f))
                                .clickable { currentStatusIndex = index }
                        )
                    }
                }
            }

            if (statusItems.size > 1) {
                IconButton(
                    onClick = { if (currentStatusIndex > 0) currentStatusIndex-- },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = Color.White)
                }
                IconButton(
                    onClick = { if (currentStatusIndex < statusItems.size - 1) currentStatusIndex++ },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = draftCaption,
                            onValueChange = {
                                draftCaption = it
                                currentStatusDraft.caption = it
                            },
                            placeholder = { Text("Adicione uma legenda...", color = Color.White.copy(0.8f)) },
                            modifier = Modifier.fillMaxWidth().clickable { }, // Ensures clickable area focus
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MessengerBlue,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 5,
                            textStyle = TextStyle(fontSize = 16.sp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MessengerBlue)
                            .clickable { onPost(statusItems.toList()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Color.White)
                    }
                }
            }
        }

        if (showTextEditor) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .zIndex(30f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                        items(fonts) { font ->
                            Text(
                                text = font,
                                color = if (selectedFont == font) MessengerBlue else Color.White,
                                modifier = Modifier
                                    .clickable { selectedFont = font }
                                    .padding(8.dp),
                                fontWeight = if (selectedFont == font) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                        items(colors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = color }
                                    .then(if (selectedColor == color) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                            )
                        }
                    }

                    BasicTextField(
                        value = currentOverlayText,
                        onValueChange = { currentOverlayText = it },
                        textStyle = TextStyle(
                            color = selectedColor,
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = when(selectedFont) {
                                "Serif" -> FontFamily.Serif
                                "Monospace" -> FontFamily.Monospace
                                "Cursive" -> FontFamily.Cursive
                                else -> FontFamily.Default
                            },
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MessengerBlue)
                    )

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (currentOverlayText.isNotBlank()) {
                                currentStatusDraft.overlays.add(StatusOverlay(
                                    text = currentOverlayText,
                                    x = 0.5f,
                                    y = 0.5f,
                                    color = selectedColor.toArgb().toLong(),
                                    fontStyle = selectedFont
                                ))
                            }
                            currentOverlayText = ""
                            showTextEditor = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MessengerBlue)
                    ) {
                        Text("Concluído")
                    }
                    TextButton(onClick = {
                        showTextEditor = false
                        currentOverlayText = ""
                    }) {
                        Text("Cancelar", color = Color.White)
                    }
                }
            }
        }

        AnimatedVisibility(visible = showEmojiStickerPicker) {
            MetaEmojiPickerPro(
                onEmojiSelected = { emoji ->
                    currentStatusDraft.overlays.add(
                        StatusOverlay(
                            stickerUrl = emoji,
                            isAnimated = true,
                            x = 0.5f,
                            y = 0.5f
                        )
                    )
                    showEmojiStickerPicker = false
                },
                heightDp = 350
            )
        }
    }
}

/**
 * A full-screen viewer for user statuses (similar to stories), supporting progress indicators and navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusViewer(
    userStatuses: List<UserStatus>,
    myUsername: String,
    viewModel: ChatViewModel,
    onClose: () -> Unit,
    onDelete: (String) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentStatus = userStatuses[currentIndex]
    var progress by remember { mutableFloatStateOf(0f) }
    var showViewers by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var isPaused by remember { mutableStateOf(false) }

    var replyText by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Pausar o status se o usuário estiver focando no teclado
    val effectiveIsPaused = isPaused || isReplying

    LaunchedEffect(currentIndex, effectiveIsPaused) {
        progress = 0f
        if (showViewers || currentStatus.isVideo) return@LaunchedEffect

        val duration = 5000L
        val updateInterval = 50L
        var accumulatedTime = 0L

        while(accumulatedTime < duration) {
            kotlinx.coroutines.delay(updateInterval)
            if (!effectiveIsPaused && !showViewers) {
                accumulatedTime += updateInterval
                progress = accumulatedTime.toFloat() / duration
            }
        }
        if (currentIndex < userStatuses.size - 1) currentIndex++ else onClose()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .onGloballyPositioned { containerSize = it.size }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    val wasReplying = isReplying
                    if (!wasReplying) isPaused = true // Só pausa por toque longo se não estiver digitando
                    tryAwaitRelease()
                    if (!wasReplying) isPaused = false
                },
                onTap = { offset ->
                    if (isReplying) {
                        isReplying = false // Tocar fora do teclado cancela a resposta
                        replyText = ""
                        focusManager.clearFocus()
                        return@detectTapGestures
                    }
                    if (offset.x < size.width * 0.3f) {
                        if (currentIndex > 0) {
                            currentIndex--
                            progress = 0f
                        } else onClose() // Changed from onPreviousUser()
                    } else {
                        if (currentIndex < userStatuses.size - 1) {
                            currentIndex++
                            progress = 0f
                        } else onClose() // Changed from onNextUser()
                    }
                }
            )
        }
    ) {
        if (currentStatus.isVideo && currentStatus.videoUrl != null) {
            VideoStatusPlayer(
                url = currentStatus.videoUrl!!,
                onComplete = {
                    if (currentIndex < userStatuses.size - 1) currentIndex++ else onClose()
                },
                isPaused = showViewers || effectiveIsPaused,
                onProgress = { progress = it }
            )
        } else {
            AsyncImage(
                model = currentStatus.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        currentStatus.overlays.forEach { overlay ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (overlay.x * containerSize.width).roundToInt(),
                            (overlay.y * containerSize.height).roundToInt()
                        )
                    }
                    .padding(8.dp)
            ) {
                if (overlay.isAnimated && overlay.stickerUrl != null) {
                    AnimatedEmoji(emoji = overlay.stickerUrl!!, modifier = Modifier.size(100.dp))
                } else {
                    Text(
                        text = overlay.text,
                        color = Color(overlay.color),
                        fontSize = overlay.fontSize.sp,
                        fontFamily = when(overlay.fontStyle) {
                            "Serif" -> FontFamily.Serif
                            "Monospace" -> FontFamily.Monospace
                            "Cursive" -> FontFamily.Cursive
                            else -> FontFamily.Default
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.background(Color.Black.copy(0.3f), RoundedCornerShape(4.dp)).padding(4.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(0.7f), Color.Transparent)))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(0.8f))))
        ) {
            if (currentStatus.caption.isNotBlank()) {
                Text(
                    text = currentStatus.caption,
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = if (currentStatus.userId == myUsername) 40.dp else 100.dp, top = 24.dp)
                )
            }
        }

        val currentContext = LocalContext.current
        if (currentStatus.userId != myUsername) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Animated Reactions Row
                val reactionEmojis = listOf("😂", "😍", "🔥", "😢", "👏", "🤯")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    reactionEmojis.forEach { emoji ->
                        AnimatedEmoji(
                            emoji = emoji,
                            size = 48.dp,
                            onClick = {
                                viewModel.setTargetId(currentStatus.userId)
                                val mockStatusReply = Message(
                                    id = currentStatus.id,
                                    senderId = currentStatus.userId,
                                    senderName = currentStatus.username,
                                    text = if (currentStatus.isVideo) "📹 Vídeo" else "📷 Imagem",
                                    imageUrl = if (!currentStatus.isVideo) currentStatus.imageUrl else null,
                                    videoThumbnailUrl = if (currentStatus.isVideo) currentStatus.videoUrl else null
                                )
                                viewModel.sendMessage(emoji, replyingTo = mockStatusReply)
                                android.widget.Toast.makeText(currentContext, "Reação Enviada", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // Input/Action Row
                val mockStatusReply = remember(currentStatus) {
                    Message(
                        id = currentStatus.id,
                        senderId = currentStatus.userId,
                        senderName = currentStatus.username,
                        text = if (currentStatus.isVideo) "📹 Vídeo" else "📷 Imagem",
                        imageUrl = if (!currentStatus.isVideo) currentStatus.imageUrl else null,
                        videoThumbnailUrl = if (currentStatus.isVideo) currentStatus.videoUrl else null
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isReplying) {
                        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }

                        TextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(24.dp))
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(0.6f),
                                unfocusedContainerColor = Color.Black.copy(0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            placeholder = { Text("Mensagem...", color = Color.White.copy(0.5f)) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    if (replyText.isNotBlank()) {
                                        viewModel.setTargetId(currentStatus.userId)
                                        viewModel.sendMessage(replyText.trim(), replyingTo = mockStatusReply)
                                        android.widget.Toast.makeText(currentContext, "Respondido", android.widget.Toast.LENGTH_SHORT).show()
                                        replyText = ""
                                        isReplying = false
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            trailingIcon = {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = replyText.isNotBlank(),
                                    enter = androidx.compose.animation.scaleIn(),
                                    exit = androidx.compose.animation.scaleOut()
                                ) {
                                    IconButton(
                                        onClick = {
                                            viewModel.setTargetId(currentStatus.userId)
                                            viewModel.sendMessage(replyText.trim(), replyingTo = mockStatusReply)
                                            android.widget.Toast.makeText(currentContext, "Respondido", android.widget.Toast.LENGTH_SHORT).show()
                                            replyText = ""
                                            isReplying = false
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier.background(LocalChatColors.current.primary, CircleShape).size(36.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(24.dp))
                                .background(Color.Black.copy(0.4f))
                                .clickable { isReplying = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text("Responder...", color = Color.White.copy(0.8f))
                        }
                    }

                    if (!isReplying) {
                        Spacer(Modifier.width(16.dp))
                        IconButton(onClick = { onClose(); viewModel.setTargetId(currentStatus.userId) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Color.White)
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                userStatuses.forEachIndexed { index, _ ->
                    LinearProgressIndicator(
                        progress = { if (index < currentIndex) 1f else if (index == currentIndex) progress else 0f },
                        modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(1.5.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = currentStatus.userPhotoUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(currentStatus.username, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentStatus.timestamp)), color = Color.White.copy(0.7f), fontSize = 12.sp)
                }
                if (currentStatus.userId == myUsername) {
                    IconButton(onClick = { showViewers = true }) {
                        Icon(Icons.Rounded.Visibility, null, tint = Color.White)
                    }
                    IconButton(onClick = { onDelete(currentStatus.id) }) { Icon(Icons.Rounded.Delete, null, tint = Color.White) }
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, null, tint = Color.White) }
            }
        }

        if (showViewers && currentStatus.userId == myUsername) {
            ModalBottomSheet(
                onDismissRequest = { showViewers = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Visto por", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                    val viewers = currentStatus.viewers.keys.toList()
                    if (viewers.isEmpty()) {
                        Text("Nenhuma visualização ainda.", color = Color.Gray, modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(viewers) { viewerId ->
                                var viewerProfile by remember { mutableStateOf<UserProfile?>(null) }
                                LaunchedEffect(viewerId) {
                                    com.google.firebase.database.FirebaseDatabase.getInstance().reference
                                        .child("users").child(viewerId).get().addOnSuccessListener {
                                            viewerProfile = it.getValue(UserProfile::class.java)
                                        }
                                }

                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = viewerProfile?.photoUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray), contentScale = ContentScale.Crop)
                                    Spacer(Modifier.width(12.dp))
                                    Text(viewerProfile?.name ?: viewerId, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * A specialized player for video statuses using ExoPlayer.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoStatusPlayer(url: String, onComplete: () -> Unit, isPaused: Boolean, onProgress: (Float) -> Unit = {}) {
    val currentContext = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(currentContext).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            kotlinx.coroutines.delay(50)
            if (exoPlayer.duration > 0) {
                onProgress(exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat())
            }
        }
    }

    LaunchedEffect(isPaused) {
        if (isPaused) exoPlayer.pause() else exoPlayer.play()
    }

    DisposableEffect(Unit) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) onComplete()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(currentContext).apply {
                player = exoPlayer
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * A pop-up dialog menu providing quick actions for a specific chat from the chat list.
 */
@Composable
fun ChatPopUpMenu(
    summary: ChatSummary,
    isBlocked: Boolean,
    onDismiss: () -> Unit,
    onOpen: (ChatSummary) -> Unit,
    onClear: (ChatSummary) -> Unit,
    onDelete: (ChatSummary) -> Unit,
    onBlockToggle: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onToggleMute: (String, Boolean) -> Unit
) {
    val chatColors = LocalChatColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(chatColors.secondaryBackground)
                    .clickable(enabled = false) {}
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = summary.friendPhotoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(chatColors.separator),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = summary.friendName ?: summary.friendId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = chatColors.textPrimary
                        )
                        Text(
                            text = if (summary.isOnline) "Online" else "Visto por último recentemente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (summary.isOnline) iOSGreen else chatColors.textSecondary
                        )
                    }
                }


                ChatPopOptionItem(
                    text = "Abrir Conversa",
                    icon = Icons.AutoMirrored.Rounded.Chat,
                    onClick = { onOpen(summary); onDismiss() }
                )

                ChatPopOptionItem(
                    text = if (summary.isPinned) "Desafixar" else "Fixar Conversa",
                    icon = Icons.Rounded.PushPin,
                    iconColor = if (summary.isPinned) MessengerBlue else null,
                    onClick = { onTogglePin(summary.friendId, summary.isPinned); onDismiss() }
                )

                ChatPopOptionItem(
                    text = if (summary.isMuted) "Ativar Sons" else "Silenciar",
                    icon = if (summary.isMuted) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                    onClick = { onToggleMute(summary.friendId, summary.isMuted); onDismiss() }
                )

                ChatPopOptionItem(
                    text = "Limpar Histórico",
                    icon = Icons.Rounded.DeleteSweep,
                    textColor = iOSRed,
                    iconColor = iOSRed,
                    onClick = { onClear(summary); onDismiss() }
                )

                ChatPopOptionItem(
                    text = "Excluir Chat",
                    icon = Icons.Rounded.DeleteOutline,
                    textColor = iOSRed,
                    iconColor = iOSRed,
                    onClick = { onDelete(summary); onDismiss() }
                )

                ChatPopOptionItem(
                    text = if (isBlocked) "Desbloquear" else "Bloquear",
                    icon = if (isBlocked) Icons.Rounded.LockOpen else Icons.Rounded.Block,
                    textColor = if (isBlocked) null else iOSRed,
                    iconColor = if (isBlocked) null else iOSRed,
                    onClick = { onBlockToggle(summary.friendId); onDismiss() }
                )
            }
        }
    }
}

/**
 * A single menu item within the [ChatPopUpMenu].
 */
@Composable
fun ChatPopOptionItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    textColor: Color? = null,
    iconColor: Color? = null
) {
    val chatColors = LocalChatColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor ?: chatColors.textPrimary.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            color = textColor ?: chatColors.textPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )
    }
}

@Composable
fun TempMessageSelectorSheet(
    currentDuration: Long,
    onSelect: (Long) -> Unit
) {
    val chatColors = LocalChatColors.current
    val options = listOf(
        0L to "Desativado",
        15000L to "15 segundos",
        30000L to "30 segundos",
        60000L to "60 segundos",
        300000L to "5 minutos",
        600000L to "10 minutos",
        1800000L to "30 minutos",
        86400000L to "24 horas"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Mensagens Temporárias",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = chatColors.textPrimary
        )

        options.forEach { (duration, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(duration) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = chatColors.textPrimary,
                    fontSize = 16.sp
                )
                if (currentDuration == duration) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selecionado",
                        tint = MessengerBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
