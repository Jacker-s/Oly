package com.jack.friend

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.firebase.database.*
import com.jack.friend.ui.theme.FriendTheme
import com.jack.friend.ui.theme.LocalChatColors
import com.jack.friend.ui.theme.MetaGray4
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.rounded.*
import com.jack.friend.R
import androidx.compose.ui.res.stringResource

class CallsActivity : androidx.fragment.app.FragmentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FriendTheme {
                CallsScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onOpenCall = { roomId, targetId, targetPhotoUrl, isOutgoing, isVideo ->
                        startActivity(
                            Intent(this, CallActivity::class.java).apply {
                                putExtra("roomId", roomId)
                                putExtra("targetId", targetId)
                                putExtra("targetPhotoUrl", targetPhotoUrl)
                                putExtra("isOutgoing", isOutgoing)
                                putExtra("isVideo", isVideo)
                            }
                        )
                    }
                )
            }
        }
    }
}

/** Item pronto para UI */
data class CallItemUi(
    val roomId: String,
    val otherId: String,
    val otherName: String,
    val otherPhotoUrl: String?,
    val isOutgoing: Boolean,
    val isVideo: Boolean,
    val status: String,
    val timeMs: Long,
    val durationSec: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CallsScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenCall: (roomId: String, targetId: String, targetPhotoUrl: String?, isOutgoing: Boolean, isVideo: Boolean) -> Unit
) {
    val myUsername by viewModel.myUsername.collectAsState("")
    val contacts by viewModel.contacts.collectAsState(emptyList())
    val activeChats by viewModel.activeChats.collectAsState(emptyList())
    val chatColors = LocalChatColors.current

    var calls by remember { mutableStateOf<List<CallItemUi>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Todas, 1: Perdidas
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<CallItemUi?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    DisposableEffect(myUsername, contacts, activeChats) {
        if (myUsername.isBlank()) {
            calls = emptyList()
            return@DisposableEffect onDispose { }
        }

        val me = myUsername.uppercase().trim()
        val db = FirebaseDatabase.getInstance().reference
        val ref = db.child("calls").limitToLast(100)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CallItemUi>()
                snapshot.children.forEach { callSnap ->
                    val roomId = callSnap.key ?: return@forEach
                    val caller = callSnap.child("callerId").getValue(String::class.java).orEmpty().uppercase().trim()
                    val receiver = callSnap.child("receiverId").getValue(String::class.java).orEmpty().uppercase().trim()
                    val status = callSnap.child("status").getValue(String::class.java).orEmpty().ifBlank { "RINGING" }
                    val isVideo = callSnap.child("isVideo").getValue(Boolean::class.java) ?: false
                    val timeMs = (callSnap.child("timestamp").value as? Long) ?: 0L
                    val durationSec = callSnap.child("durationSec").getValue(Long::class.java)

                    if (caller == me || receiver == me) {
                        val isOutgoing = caller == me
                        val otherId = if (isOutgoing) receiver else caller
                        val otherProfile = contacts.firstOrNull { it.id.equals(otherId, true) }
                        val otherChat = activeChats.firstOrNull { it.friendId.equals(otherId, true) }
                        val otherName = otherProfile?.name ?: otherChat?.friendName ?: otherId
                        val otherPhotoUrl = otherProfile?.photoUrl ?: otherChat?.friendPhotoUrl

                        list.add(CallItemUi(roomId, otherId, otherName, otherPhotoUrl, isOutgoing, isVideo, status, timeMs, durationSec))
                    }
                }
                calls = list.sortedByDescending { it.timeMs }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("CallsActivity", "Firebase error: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    val filteredLogs = remember(calls, searchQuery, selectedTab) {
        calls.filter { log ->
            val matchesSearch = log.otherName.contains(searchQuery, ignoreCase = true) ||
                    log.otherId.contains(searchQuery, ignoreCase = true)
            val matchesTab = if (selectedTab == 1) log.status == "MISSED" || log.status == "REJECTED" else true
            matchesSearch && matchesTab
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(chatColors.background)) {
                CenterAlignedTopAppBar(
                    title = {
                        SegmentedControl(
                            items = listOf(stringResource(R.string.calls_tab_all), stringResource(R.string.calls_tab_missed)),
                            selectedIndex = selectedTab,
                            onItemSelection = { selectedTab = it },
                            modifier = Modifier.width(180.dp)
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = { showDeleteAllDialog = true }) {
                            Text(stringResource(R.string.calls_action_clear), color = chatColors.primary, fontWeight = FontWeight.Medium)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showContactPicker = true }) {
                            Icon(Icons.Rounded.Call, stringResource(R.string.calls_new_call), tint = chatColors.primary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                Text(
                    stringResource(R.string.calls_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = chatColors.textPrimary
                )

                IOS17SearchPill(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = stringResource(R.string.hint_search),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        containerColor = chatColors.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (filteredLogs.isEmpty()) {
                EmptyCallsState(isSearch = searchQuery.isNotBlank() || selectedTab == 1)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredLogs, key = { it.roomId }) { log ->
                        CallRowItem(
                            log = log,
                            onDelete = { logToDelete = log },
                            onClick = {
                                onOpenCall(log.roomId, log.otherId, log.otherPhotoUrl, log.isOutgoing, log.isVideo)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.calls_dialog_clear_title)) },
            text = { Text(stringResource(R.string.calls_dialog_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val db = FirebaseDatabase.getInstance().reference
                    calls.forEach { db.child("calls").child(it.roomId).removeValue() }
                    showDeleteAllDialog = false
                }) { Text(stringResource(R.string.calls_dialog_clear_all), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    logToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text(stringResource(R.string.calls_dialog_delete_title)) },
            text = { Text(stringResource(R.string.calls_dialog_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseDatabase.getInstance().reference.child("calls").child(log.roomId).removeValue()
                    logToDelete = null
                }) { Text(stringResource(R.string.action_delete), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showContactPicker) {
        ModalBottomSheet(
            onDismissRequest = { showContactPicker = false },
            containerColor = chatColors.secondaryBackground,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ContactPickerForCall(
                contacts = contacts,
                onSelect = { user, isVideo ->
                    showContactPicker = false
                    val uniqueRoomId = "Call_${UUID.randomUUID().toString().take(8)}"
                    viewModel.setTargetId(user.id)
                    viewModel.startCall(isVideo = isVideo, customRoomId = uniqueRoomId)
                    onOpenCall(uniqueRoomId, user.id, user.photoUrl, true, isVideo)
                }
            )
        }
    }
}

@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelection: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    Surface(
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = chatColors.tertiaryBackground.copy(alpha = 0.5f)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { onItemSelection(index) },
                    shape = RoundedCornerShape(7.dp),
                    color = if (isSelected) Color.White else Color.Transparent,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else chatColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCallsState(isSearch: Boolean) {
    val chatColors = LocalChatColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                Icons.Rounded.History, null,
                modifier = Modifier.size(64.dp),
                tint = chatColors.textSecondary.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (isSearch) stringResource(R.string.contacts_search_empty_title) else stringResource(R.string.calls_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = chatColors.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallRowItem(
    log: CallItemUi,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val chatColors = LocalChatColors.current
    val isMissed = (log.status == "MISSED" || log.status == "REJECTED") && !log.isOutgoing

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onDelete)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = log.otherPhotoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(chatColors.separator),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.otherName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isMissed) Color.Red else chatColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (log.isVideo) Icons.Rounded.Videocam else Icons.Rounded.Call,
                        contentDescription = null,
                        tint = chatColors.textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (log.isOutgoing) stringResource(R.string.calls_status_outgoing) else stringResource(R.string.calls_status_incoming_log),
                        style = MaterialTheme.typography.bodySmall,
                        color = chatColors.textSecondary
                    )
                    log.durationSec?.let {
                        if (it > 0) {
                            Text(" • ${formatDuration(LocalContext.current, it)}", style = MaterialTheme.typography.bodySmall, color = chatColors.textSecondary)
                        }
                    }
                }
            }

            Text(
                text = formatCallTime(LocalContext.current, log.timeMs),
                style = MaterialTheme.typography.bodySmall,
                color = chatColors.textSecondary
            )

            Spacer(Modifier.width(12.dp))

            Icon(
                Icons.Rounded.Info, null,
                tint = chatColors.primary,
                modifier = Modifier.size(22.dp).clickable { onDelete() } // Emula o botão de info, mas aqui deleta ou abre info se tivesse
            )
        }
        HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = chatColors.separator.copy(alpha = 0.2f), thickness = 0.5.dp)
    }
}

@Composable
fun ContactPickerForCall(
    contacts: List<UserProfile>,
    onSelect: (UserProfile, Boolean) -> Unit
) {
    val chatColors = LocalChatColors.current
    Column(modifier = Modifier.fillMaxHeight(0.8f).padding(16.dp)) {
        Text(stringResource(R.string.calls_picker_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(contacts) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(chatColors.tertiaryBackground.copy(alpha = 0.4f))
                        .clickable { onSelect(user, false) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(user.displayName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    
                    IconButton(onClick = { onSelect(user, false) }) {
                        Icon(Icons.Rounded.Call, null, tint = chatColors.primary)
                    }
                    IconButton(onClick = { onSelect(user, true) }) {
                        Icon(Icons.Rounded.Videocam, null, tint = chatColors.primary)
                    }
                }
            }
        }
    }
}

private fun formatCallTime(context: android.content.Context, timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val dayMs = 86400000L

    return when {
        diff < dayMs -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 2 * dayMs -> context.getString(R.string.date_yesterday)
        diff < 7 * dayMs -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatDuration(context: android.content.Context, seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) {
        context.getString(R.string.duration_format_min_sec, mins, secs)
    } else {
        context.getString(R.string.duration_format_sec, secs)
    }
}
