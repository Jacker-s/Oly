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
    val colors = LocalChatColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fullScreenPhotoUrl by remember { mutableStateOf<String?>(null) }
    
    var isVerified by remember { mutableStateOf(false) }
    var mutualGroups by remember { mutableIntStateOf(0) }
    val myPhotoUrl by viewModel.myPhotoUrl.collectAsStateWithLifecycle(null)
    val feedPosts by viewModel.feedPosts.collectAsStateWithLifecycle()
    val userPosts = remember(feedPosts, user.id) { feedPosts.filter { it.authorId == user.id } }

    LaunchedEffect(user.id) {
        FirebaseDatabase.getInstance().reference.child("verifiedUsers").child(user.id).get()
            .addOnSuccessListener { isVerified = it.getValue(Boolean::class.java) == true }
        
        // Mocking some groups/mutuals for UI beauty
        mutualGroups = (user.id.length % 4) + 1
    }

    val presenceText = remember(user.isOnline, user.isVisibleOnline, user.lastActive) {
        when {
            user.isOnline && user.isVisibleOnline -> user.presenceStatus
            user.showLastSeen && user.lastActive > 0L -> "visto por último ${formatLastSeen(user.lastActive)}"
            else -> "Offline"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.primaryBackground,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(colors.background)
            ) {
                // Immersive Header with Glassmorphism
                Box(modifier = Modifier.height(280.dp).fillMaxWidth()) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(if (user.photoUrl != null) 30.dp else 0.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, colors.background))
                    ))
                    
                    // TOP BAR ACTIONS (Block/Delete)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(0.3f),
                                modifier = Modifier.size(32.dp).clickable { onToggleBlock() }
                            ) {
                                Icon(
                                    if (isBlocked) Icons.Rounded.Block else Icons.Rounded.PersonOff,
                                    null,
                                    tint = if (isBlocked) iOSOrange else Color.White,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                            if (isContact) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(0.3f),
                                    modifier = Modifier.size(32.dp).clickable { onRemove(user) }
                                ) {
                                    Icon(
                                        Icons.Rounded.PersonRemove,
                                        null,
                                        tint = iOSRed,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                        }
                        
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(0.3f),
                            modifier = Modifier.size(32.dp).clickable { onDismiss() }
                        ) {
                            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }

                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(120.dp).shadow(20.dp, CircleShape),
                            border = BorderStroke(4.dp, Color.White),
                            color = colors.secondaryBackground
                        ) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize().clickable { fullScreenPhotoUrl = user.photoUrl },
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.displayName, fontWeight = FontWeight.Black, fontSize = 24.sp, color = colors.textPrimary)
                            if (isVerified) {
                                Icon(Icons.Rounded.Verified, null, tint = MessengerBlue, modifier = Modifier.size(20.dp).padding(start = 6.dp))
                            }
                        }
                        
                        Text(
                            text = "@${user.id.lowercase()}", 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = colors.textSecondary.copy(alpha = 0.8f)
                        )
                        
                        Spacer(Modifier.height(4.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (user.isOnline && user.isVisibleOnline) iOSGreen.copy(0.15f) else colors.separator.copy(0.1f)
                        ) {
                            Text(
                                presenceText, 
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = if (user.isOnline && user.isVisibleOnline) iOSGreen else colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Apple Style Action Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ModernActionButton("Mensagem", Icons.Rounded.ChatBubble, MessengerBlue) { onMessage(user) }
                    ModernActionButton("Ligação", Icons.Rounded.Call, MessengerBlue) { onAudioCall(user) }
                    ModernActionButton("Vídeo", Icons.Rounded.Videocam, MessengerBlue) { onVideoCall(user) }
                    ModernActionButton(if (isMuted) "Ativar" else "Silenciar", if (isMuted) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff, if (isMuted) iOSOrange else colors.textSecondary) { onToggleMute() }
                }

                Spacer(Modifier.height(16.dp))
                
                // BIO Card
                ProfileCard(colors) {
                    Text(
                        text = user.status.ifBlank { "Olá! Estou usando o Wappi Messenger." },
                        modifier = Modifier.padding(16.dp),
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        lineHeight = 22.sp
                    )
                }
                
                Spacer(Modifier.height(16.dp))

                // MEDIA/LINKS PREVIEW (Simulated or Real)
                SectionTitle("Mídias, Links e Docs")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    userPosts.take(5).forEach { post ->
                        AsyncImage(
                            model = post.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(colors.separator),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (userPosts.isEmpty()) {
                        repeat(3) {
                             Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(colors.secondaryBackground), contentAlignment = Alignment.Center) {
                                 Icon(Icons.Rounded.InsertPhoto, null, tint = colors.textSecondary.copy(0.3f))
                             }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                
                // SECURITY SECTIONS
                SectionTitle("Segurança e Configurações")
                ProfileCard(colors) {
                    SettingsRow(Icons.Rounded.Lock, "Criptografia de ponta a ponta", "Suas conversas são privadas", colors)
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), 0.5.dp, colors.separator.copy(0.2f))
                    SettingsRow(Icons.Rounded.History, "Mensagens Temporárias", "Desativadas", colors)
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), 0.5.dp, colors.separator.copy(0.2f))
                    SettingsRow(Icons.Rounded.Group, "$mutualGroups grupos em comum", "Vocês participam dos mesmos chats", colors)
                }

                Spacer(Modifier.height(24.dp))
                
                // SOCIAL FEED SECTION
                SectionTitle("Publicações")
                if (userPosts.isNotEmpty()) {
                    userPosts.forEach { post ->
                        FeedPostCard(
                            post = post,
                            isAuthorOnline = user.isOnline,
                            myUsername = myUsername,
                            myPhotoUrl = myPhotoUrl,
                            viewModel = viewModel,
                            onAuthorClick = { },
                            onImageClick = { fullScreenPhotoUrl = it }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                } else {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("Ainda não compartilhou nada no Feed", color = colors.textSecondary, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(80.dp))
            }

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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MessengerBlue
    )
}

@Composable
private fun ModernActionButton(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(56.dp).clickable(onClick = onClick),
            shape = CircleShape,
            color = Color.White.copy(0.1f),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
        }
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, colors: ChatColors) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MessengerBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
            Text(subtitle, fontSize = 13.sp, color = colors.textSecondary)
        }
    }
}

@Composable
private fun ProfileCard(colors: ChatColors, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = colors.secondaryBackground.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, colors: ChatColors, contentColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = contentColor.copy(0.8f), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontWeight = FontWeight.Bold, color = contentColor, fontSize = 16.sp)
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
