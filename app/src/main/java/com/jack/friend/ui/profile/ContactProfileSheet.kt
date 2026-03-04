package com.jack.friend.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.database.FirebaseDatabase
import com.jack.friend.ChatViewModel
import com.jack.friend.FeedPostCard
import com.jack.friend.UserProfile
import com.jack.friend.ui.chat.MediaViewerItem
import com.jack.friend.ui.chat.MediaViewerScreen
import com.jack.friend.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOS17ContactProfileSheet(
    viewModel: ChatViewModel,
    user: UserProfile,
    myUsername: String,
    isMuted: Boolean,
    isBlocked: Boolean,
    onDismiss: () -> Unit,
    onMessage: (UserProfile) -> Unit,
    onAudioCall: (UserProfile) -> Unit,
    onVideoCall: (UserProfile) -> Unit,
    onToggleMute: () -> Unit,
    onToggleBlock: () -> Unit,
    onRemove: (UserProfile) -> Unit,
    isContact: Boolean = true,
    onAddContact: (UserProfile) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val colors = LocalChatColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fullScreenPhotoUrl by remember { mutableStateOf<String?>(null) }

    var isVerified by remember { mutableStateOf(false) }
    var mutualGroups by remember { mutableIntStateOf(0) }

    LaunchedEffect(myUsername, user.id) {
        FirebaseDatabase.getInstance().reference.child("verifiedUsers").child(user.id).get()
            .addOnSuccessListener { isVerified = it.getValue(Boolean::class.java) == true }

        if (myUsername.isNotBlank() && user.id.isNotBlank()) {
            FirebaseDatabase.getInstance().reference.child("groups").get().addOnSuccessListener { snap ->
                var count = 0
                snap.children.forEach { g ->
                    if (g.child("members").hasChild(myUsername) && g.child("members").hasChild(user.id)) count++
                }
                mutualGroups = count
            }
        }
    }

    val presenceColor = when (user.presenceStatus) {
        "Online" -> iOSGreen
        "Ocupado" -> iOSRed
        "Ausente" -> iOSOrange
        else -> MetaGray4
    }

    val feedPosts by viewModel.feedPosts.collectAsStateWithLifecycle()
    val userPosts = feedPosts.filter { it.authorId == user.id }

    val presenceText = remember(user.isOnline, user.showLastSeen, user.lastActive, user.presenceStatus, user.isVisibleOnline) {
        when {
            user.isOnline && user.isVisibleOnline -> user.presenceStatus
            user.showLastSeen && user.lastActive > 0L -> "Visto ${formatLastSeen(user.lastActive)}"
            else -> "Offline"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.primaryBackground,
        dragHandle = null,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.id.lowercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colors.textPrimary
                        )
                        if (isVerified) {
                            Icon(
                                Icons.Rounded.Verified,
                                null,
                                tint = MessengerBlue,
                                modifier = Modifier.padding(start = 6.dp).size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = colors.textPrimary)
                    }
                }

                // Profile Image and Stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Image
                    Box {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier
                                .size(88.dp)
                                .clickable { fullScreenPhotoUrl = user.photoUrl },
                            color = colors.secondaryBackground
                        ) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (user.isOnline && user.isVisibleOnline) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(16.dp)
                                        .background(presenceColor, CircleShape)
                                        .border(2.dp, colors.primaryBackground, CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(24.dp))

                    // Stats
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${userPosts.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                            Text("Postagens", fontSize = 13.sp, color = colors.textPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$mutualGroups", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                            Text("Grupos", fontSize = 13.sp, color = colors.textPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Bio
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = user.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = user.status.ifBlank { "Olá! Estou usando o Wappi Messenger." },
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = presenceText,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isContact) {
                        Button(
                            onClick = { onMessage(user) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryBackground),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Mensagem", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Button(
                            onClick = { onAudioCall(user) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryBackground),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Ligar", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        Button(
                            onClick = { onAddContact(user) },
                            modifier = Modifier.weight(1.5f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MessengerBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Seguir+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Button(
                            onClick = { onMessage(user) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryBackground),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Mensagem", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // IG Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.GridView, null, tint = colors.textPrimary)
                        Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 12.dp).height(1.dp).fillMaxWidth().background(colors.textPrimary))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)
                            .clickable { onToggleMute() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (isMuted) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive, null, tint = colors.textSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)
                            .clickable { onToggleBlock() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (isBlocked) Icons.Rounded.Block else Icons.Rounded.PersonOff, null, tint = colors.textSecondary)
                    }
                    if (isContact) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp)
                                .clickable { onRemove(user) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PersonRemove, null, tint = iOSRed.copy(0.7f))
                        }
                    }
                }
                
                HorizontalDivider(color = colors.separator.copy(0.3f), thickness = 0.5.dp)

                if (userPosts.isNotEmpty()) {
                    userPosts.forEach { post ->
                        FeedPostCard(
                            post = post,
                            myUsername = myUsername,
                            viewModel = viewModel,
                            onImageClick = { fullScreenPhotoUrl = it }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = colors.primaryBackground,
                                border = BorderStroke(2.dp, colors.textPrimary),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Rounded.CameraAlt, null, tint = colors.textPrimary, modifier = Modifier.padding(16.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Nenhuma Publicação", 
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(60.dp))
            }

            // Full Screen Photo Viewer integration
            if (fullScreenPhotoUrl != null) {
                MediaViewerScreen(
                    mediaItem = MediaViewerItem.Image(fullScreenPhotoUrl!!),
                    onDismiss = { fullScreenPhotoUrl = null }
                )
            }
        }
    }
}

@Composable
private fun ModernActionButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = containerColor.copy(alpha = 0.12f),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = containerColor, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = containerColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileCard(colors: ChatColors, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.secondaryBackground,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun InfoItem(icon: ImageVector, label: String, value: String, colors: ChatColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MessengerBlue, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 17.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    colors: ChatColors,
    contentColor: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = contentColor ?: MessengerBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 17.sp,
            color = contentColor ?: colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatLastSeen(lastActive: Long): String {
    val now = Calendar.getInstance()
    val c = Calendar.getInstance().apply { timeInMillis = lastActive }
    val timeFmt = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(lastActive))
    val sameYear = now.get(Calendar.YEAR) == c.get(Calendar.YEAR)
    val dayNow = now.get(Calendar.DAY_OF_YEAR)
    val dayThen = c.get(Calendar.DAY_OF_YEAR)

    return when {
        sameYear && dayNow == dayThen -> "hoje às $timeFmt"
        sameYear && dayNow == dayThen + 1 -> "ontem às $timeFmt"
        else -> {
            val dateFmt = SimpleDateFormat("d MMM", Locale("pt", "BR")).format(Date(lastActive))
            "$dateFmt às $timeFmt"
        }
    }
}
