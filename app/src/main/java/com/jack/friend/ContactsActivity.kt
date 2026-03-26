package com.jack.friend

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.jack.friend.ui.profile.IOS17ContactProfileSheet
import com.jack.friend.ui.theme.*
import java.util.UUID
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class ContactsActivity : AppCompatActivity() {
    private val viewModel: ChatViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            checkAndFetchLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkLocationPermissions()

        setContent {
            FriendTheme {
                ContactsScreenIOS17(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onOpenChat = { contact ->
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("targetId", contact.id)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        finish()
                    },
                    onStartCall = { contact, isVideo ->
                        viewModel.setTargetId(contact.id)
                        val uniqueRoomId = "Call_${UUID.randomUUID().toString().take(8)}"
                        viewModel.startCall(isVideo = isVideo, customRoomId = uniqueRoomId)

                        startActivity(
                            Intent(this, CallActivity::class.java).apply {
                                Log.d("ContactsActivity", "Starting call: $uniqueRoomId")
                                putExtra("roomId", uniqueRoomId)
                                putExtra("targetId", contact.id)
                                putExtra("targetPhotoUrl", contact.photoUrl)
                                putExtra("isOutgoing", true)
                                putExtra("isVideo", isVideo)
                            }
                        )
                    }
                )
            }
        }
    }

    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                checkAndFetchLocation()
            }
            else -> {
                requestPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }

    private fun checkAndFetchLocation() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    location?.let {
                        viewModel.updateUserLocation(it.latitude, it.longitude)
                    }
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreenIOS17(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenChat: (UserProfile) -> Unit,
    onStartCall: (UserProfile, Boolean) -> Unit
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsStateWithLifecycle(emptyList())
    val myUsername by viewModel.myUsername.collectAsStateWithLifecycle("")
    val activeChats by viewModel.activeChats.collectAsStateWithLifecycle(emptyList())
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle(emptyList())
    
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<UserProfile?>(null) }
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var longPressContact by remember { mutableStateOf<UserProfile?>(null) }
    var showLongPressMenu by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    var showQRScanner by remember { mutableStateOf(false) }
    var showMyQRSheet by remember { mutableStateOf(false) }
    var qrResultId by remember { mutableStateOf<String?>(null) }
    val myPhotoUrl by viewModel.myPhotoUrl.collectAsStateWithLifecycle(null)
    val myDisplayName by viewModel.myName.collectAsStateWithLifecycle("")

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Friends, 1: Discover

    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle(emptyList())
    val nearbyUsers by viewModel.nearbyUsers.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(query, selectedTab) {
        if (selectedTab == 1) {
            if (query.isNotBlank()) {
                viewModel.searchUsers(query)
            } else {
                viewModel.fetchNearbyUsers()
            }
        }
    }

    val filteredContacts by remember(contacts, query) {
        derivedStateOf {
            val q = query.trim()
            if (q.isEmpty()) contacts
            else contacts.filter {
                it.name.contains(q, ignoreCase = true) ||
                        it.id.contains(q, ignoreCase = true) ||
                        (it.status).contains(q, ignoreCase = true)
            }
        }
    }

    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.contacts_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (contacts.isNotEmpty()) {
                            Text(
                                text = if (query.isBlank()) stringResource(R.string.contacts_count, contacts.size) else stringResource(R.string.contacts_search_results_count, filteredContacts.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MetaGray4
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAddContactDialog = true }) {
                        Icon(Icons.Rounded.PersonAdd, stringResource(R.string.contacts_action_add_description), tint = LocalChatColors.current.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionIcon(Icons.Rounded.QrCodeScanner, stringResource(R.string.contacts_quick_scan), colors = LocalChatColors.current) {
                    showQRScanner = true
                }
                QuickActionIcon(Icons.Rounded.QrCode, stringResource(R.string.contacts_quick_my_qr), colors = LocalChatColors.current) {
                    showMyQRSheet = true
                }
                QuickActionIcon(Icons.Rounded.PersonAddAlt1, stringResource(R.string.contacts_quick_add_new), colors = LocalChatColors.current) {
                    showAddContactDialog = true
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = LocalChatColors.current.primary,
                divider = { HorizontalDivider(thickness = 0.5.dp, color = LocalChatColors.current.separator.copy(0.2f)) },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = LocalChatColors.current.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.contacts_tab_mine), fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.contacts_tab_discover), fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            IOS17SearchPill(
                value = query,
                onValueChange = { query = it },
                placeholder = if (selectedTab == 0) stringResource(R.string.contacts_search_hint_mine) else stringResource(R.string.contacts_search_hint_discover),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 130.dp)
            ) {
                if (selectedTab == 0) {
                    if (contacts.isEmpty()) {
                        item {
                            EmptyContactsState(onAddClick = { showAddContactDialog = true })
                        }
                    } else if (filteredContacts.isEmpty()) {
                        item {
                            EmptySearchState(query = query)
                        }
                    } else {
                        items(
                            items = filteredContacts,
                            key = { it.id }
                        ) { contact ->
                            FriendRow(
                                contact = contact,
                                isBlocked = blockedUsers.contains(contact.id),
                                onClick = { selectedProfile = contact },
                                onLongClick = {
                                    longPressContact = contact
                                    showLongPressMenu = true
                                },
                                onMessageClick = { onOpenChat(contact) }
                            )
                        }
                    }
                } else {
                    // Discover Tab
                    if (query.isBlank()) {
                        if (nearbyUsers.isEmpty()) {
                            item { DiscoverEmptyState() }
                        } else {
                            item {
                                Text(
                                    stringResource(R.string.contacts_section_nearby),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MetaGray4,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            items(
                                items = nearbyUsers.filter { it.id != myUsername },
                                key = { it.id }
                            ) { user ->
                                val isContact = contacts.any { it.id == user.id }
                                DiscoverFriendRow(
                                    user = user,
                                    isContact = isContact,
                                    onClick = { selectedProfile = user },
                                    onAddClick = { 
                                        viewModel.addContact(user.id) { success, _ -> 
                                            if (success) Toast.makeText(context, context.getString(R.string.contacts_toast_added), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        items(
                            items = searchResults.filter { it.id != myUsername },
                            key = { it.id }
                        ) { user ->
                            val isContact = contacts.any { it.id == user.id }
                            DiscoverFriendRow(
                                user = user,
                                isContact = isContact,
                                onClick = { selectedProfile = user },
                                onAddClick = { 
                                    viewModel.addContact(user.id) { success, _ -> 
                                        if (success) Toast.makeText(context, context.getString(R.string.contacts_toast_added), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showLongPressMenu && longPressContact != null) {
            val contact = longPressContact!!
            ContactActionSheet(
                contact = contact,
                isBlocked = blockedUsers.contains(contact.id),
                onDismiss = { showLongPressMenu = false },
                onOpenChat = {
                    showLongPressMenu = false
                    onOpenChat(contact)
                },
                onCall = { isVideo ->
                    showLongPressMenu = false
                    onStartCall(contact, isVideo)
                },
                onBlock = {
                    showLongPressMenu = false
                    if (blockedUsers.contains(contact.id)) viewModel.unblockUser(contact.id)
                    else viewModel.blockUser(contact.id)
                },
                onDelete = {
                    showLongPressMenu = false
                    showDeleteDialog = contact
                }
            )
        }

        if (selectedProfile != null) {
            val user = selectedProfile!!
            val chat = activeChats.firstOrNull { it.friendId == user.id }
            IOS17ContactProfileSheet(
                viewModel = viewModel,
                user = user,
                myUsername = myUsername,
                isMuted = chat?.isMuted ?: false,
                isBlocked = blockedUsers.contains(user.id),
                onDismiss = { selectedProfile = null },
                onMessage = {
                    selectedProfile = null
                    onOpenChat(it)
                },
                onAudioCall = {
                    selectedProfile = null
                    onStartCall(it, false)
                },
                onVideoCall = {
                    selectedProfile = null
                    onStartCall(it, true)
                },
                onToggleMute = { viewModel.toggleMuteChat(user.id, chat?.isMuted ?: false) },
                onToggleBlock = {
                    if (blockedUsers.contains(user.id)) viewModel.unblockUser(user.id)
                    else viewModel.blockUser(user.id)
                },
                onRemove = {
                    selectedProfile = null
                    showDeleteDialog = it
                }
            )
        }

        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text(stringResource(R.string.contacts_dialog_unfriend_title)) },
                text = { Text(stringResource(R.string.contacts_dialog_unfriend_message, showDeleteDialog?.name ?: "")) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog?.let { viewModel.deleteContact(it.id) { _, _ -> } }
                        showDeleteDialog = null
                    }) { Text(stringResource(R.string.contacts_dialog_unfriend_confirm), color = iOSRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text(stringResource(R.string.action_cancel)) }
                }
            )
        }

        if (showAddContactDialog) {
            AddContactDialog(
                icon = Icons.Rounded.PersonAdd,
                searchResults = searchResults,
                onSearch = { viewModel.searchUsers(it) },
                onDismiss = { showAddContactDialog = false },
                onAdd = { username ->
                    viewModel.addContact(username) { success, err ->
                        if (success) showAddContactDialog = false
                        else Toast.makeText(context, err ?: context.getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (showMyQRSheet) {
            MyQRSheet(
                userId = myUsername,
                displayName = myDisplayName ?: myUsername,
                photoUrl = myPhotoUrl,
                onDismiss = { showMyQRSheet = false }
            )
        }

    }

    if (showQRScanner) {
        Box(Modifier.fillMaxSize().zIndex(100f)) {
            QRCodeScannerScreen(
                onDismiss = { showQRScanner = false },
                onResult = { id ->
                    showQRScanner = false
                    Toast.makeText(context, context.getString(R.string.contacts_toast_id_found, id), Toast.LENGTH_SHORT).show()
                    
                    // Fetch and show profile
                    viewModel.fetchUserProfile(id) { profile ->
                        if (profile != null) {
                            selectedProfile = profile
                        }
                    }

                    // Also add as contact
                    viewModel.addContact(id) { s, e ->
                        if (!s && e != null) Toast.makeText(context, e, Toast.LENGTH_SHORT).show()
                        else if (s) Toast.makeText(context, context.getString(R.string.contacts_toast_added), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FriendRow(
    contact: UserProfile,
    isBlocked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    val chatColors = LocalChatColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = chatColors.secondaryBackground,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = contact.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(chatColors.separator),
                    contentScale = ContentScale.Crop
                )
                if (contact.isOnline && contact.isVisibleOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                            .background(iOSGreen, CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = chatColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.status.takeIf { it.isNotBlank() } ?: stringResource(R.string.contacts_status_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isBlocked) iOSRed else chatColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.id.length % 3 == 0) { // Simulated mutual friends for UI beauty
                     Text(
                        text = stringResource(R.string.contacts_mutual_friends_count, 2 + (contact.id.length % 5)),
                        style = MaterialTheme.typography.labelSmall,
                        color = chatColors.primary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            IconButton(
                onClick = onMessageClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(chatColors.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Rounded.ChatBubble, 
                    contentDescription = stringResource(R.string.contacts_action_message_description), 
                    tint = chatColors.primary, 
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DiscoverFriendRow(
    user: UserProfile,
    isContact: Boolean,
    onClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val chatColors = LocalChatColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = chatColors.secondaryBackground,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(chatColors.separator),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = chatColors.textPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.profile_username_format, user.id.lowercase()),
                        style = MaterialTheme.typography.bodySmall,
                        color = chatColors.textSecondary
                    )
                    if (user.status.contains("km")) {
                         Spacer(Modifier.width(8.dp))
                         Text(
                            text = "• ${user.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = chatColors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!isContact) {
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = chatColors.primary)
                ) {
                    Text(stringResource(R.string.contacts_action_add_description), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    color = iOSGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        stringResource(R.string.contacts_status_is_friend), 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = iOSGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionIcon(icon: ImageVector, label: String, colors: ChatColors, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = colors.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = colors.primary, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DiscoverEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Public, null, modifier = Modifier.size(64.dp), tint = MetaGray4.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.contacts_discover_empty_title), 
            fontWeight = FontWeight.Bold, 
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.contacts_discover_empty_subtitle), 
            color = MetaGray4,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactActionSheet(
    contact: UserProfile,
    isBlocked: Boolean,
    onDismiss: () -> Unit,
    onOpenChat: () -> Unit,
    onCall: (Boolean) -> Unit,
    onBlock: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = contact.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(contact.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("@${contact.id.lowercase()}", color = MetaGray4, fontSize = 14.sp)
                }
            }
            
            HorizontalDivider(color = LocalChatColors.current.separator, thickness = 0.5.dp)
            
            ActionItem(stringResource(R.string.contacts_sheet_message), Icons.Rounded.ChatBubble, LocalChatColors.current.primary, onOpenChat)
            ActionItem(stringResource(R.string.contacts_sheet_audio_call), Icons.Rounded.Call, LocalChatColors.current.primary) { onCall(false) }
            ActionItem(stringResource(R.string.contacts_sheet_video_call), Icons.Rounded.Videocam, LocalChatColors.current.primary) { onCall(true) }
            ActionItem(if (isBlocked) stringResource(R.string.contacts_sheet_unblock) else stringResource(R.string.contacts_sheet_block), Icons.Rounded.Block, iOSRed, onBlock)
            ActionItem(stringResource(R.string.contacts_dialog_unfriend_title), Icons.Rounded.PersonRemove, iOSRed, onDelete)
        }
    }
}

@Composable
private fun ActionItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = color) },
        leadingContent = { Icon(icon, null, tint = color) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun EmptyContactsState(onAddClick: () -> Unit) {
    val chatColors = LocalChatColors.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = chatColors.secondaryBackground,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.People, 
                    null, 
                    modifier = Modifier.size(56.dp), 
                    tint = chatColors.primary.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.contacts_empty_list_title), 
            fontWeight = FontWeight.ExtraBold, 
            fontSize = 20.sp,
            color = chatColors.textPrimary
        )
        Text(
            stringResource(R.string.contacts_empty_list_subtitle), 
            color = chatColors.textSecondary, 
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAddClick,
            modifier = Modifier.height(48.dp).fillMaxWidth(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = chatColors.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Rounded.PersonAdd, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.contacts_empty_list_button), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.SearchOff, null, modifier = Modifier.size(80.dp), tint = MetaGray4.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.contacts_search_empty_title), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(stringResource(R.string.contacts_search_empty_subtitle, query), color = MetaGray4)
    }
}

@Composable
fun IOS17SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = LocalChatColors.current.secondaryBackground.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = MetaGray4, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (value.isBlank()) Text(placeholder, color = MetaGray4, fontSize = 16.sp)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Cancel, null, tint = MetaGray4)
                }
            }
        }
    }
}
