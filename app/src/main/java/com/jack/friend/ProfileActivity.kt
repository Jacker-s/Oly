package com.jack.friend

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jack.friend.FeedPostCard
import com.jack.friend.ui.chat.MediaViewerItem
import com.jack.friend.ui.chat.MediaViewerScreen
import com.jack.friend.ui.profile.PrivacyPolicyScreen
import com.jack.friend.ui.theme.*

class ProfileActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val uiPrefs = remember { context.getSharedPreferences("ui_prefs", MODE_PRIVATE) }
            val isDarkMode = uiPrefs.getBoolean("dark_mode", false)
            val followSystem = uiPrefs.getBoolean("follow_system", true)

            FriendTheme(isDarkModeOverride = if (followSystem) null else isDarkMode) {
                val viewModel: ChatViewModel = viewModel()
                val colors = LocalChatColors.current
                
                val myName by viewModel.myName.collectAsStateWithLifecycle("")
                val myUsername by viewModel.myUsername.collectAsStateWithLifecycle("")
                val myPhotoUrl by viewModel.myPhotoUrl.collectAsStateWithLifecycle(null)
                val myStatus by viewModel.myStatus.collectAsStateWithLifecycle("")
                val myPresenceStatus by viewModel.myPresenceStatus.collectAsStateWithLifecycle("Online")
                val isHiddenFromSearch by viewModel.isHiddenFromSearch.collectAsStateWithLifecycle(false)
                
                val myContacts by viewModel.contacts.collectAsStateWithLifecycle(emptyList())
                val feedPosts by viewModel.feedPosts.collectAsStateWithLifecycle(emptyList())
                val myPosts = feedPosts.filter { it.authorId == myUsername }

                var nameInput by remember { mutableStateOf("") }
                var statusInput by remember { mutableStateOf("") }
                var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
                var selectedPresence by remember { mutableStateOf(myPresenceStatus) }
                var hideFromSearch by remember { mutableStateOf(isHiddenFromSearch) }
                var isSaving by remember { mutableStateOf(false) }
                var showPresenceMenu by remember { mutableStateOf(false) }
                var showPrivacyPolicy by remember { mutableStateOf(false) }
                var fullScreenPhotoUrl by remember { mutableStateOf<String?>(null) }
                var showSettingsMenu by remember { mutableStateOf(false) }
                var selectedTab by remember { mutableIntStateOf(0) } // 0: Grid, 1: List
                
                var dataLoaded by remember { mutableStateOf(false) }

                LaunchedEffect(myName, myStatus, isHiddenFromSearch) {
                    if (!dataLoaded && (myName.isNotEmpty() || myStatus.isNotEmpty())) {
                        nameInput = myName
                        statusInput = myStatus
                        hideFromSearch = isHiddenFromSearch
                        dataLoaded = true
                    }
                }

                LaunchedEffect(myPresenceStatus) { selectedPresence = myPresenceStatus }

                val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    selectedImageUri = uri
                }

                BackHandler(enabled = showPrivacyPolicy || fullScreenPhotoUrl != null || showSettingsMenu) {
                    if (fullScreenPhotoUrl != null) fullScreenPhotoUrl = null
                    else if (showSettingsMenu) showSettingsMenu = false
                    else if (showPrivacyPolicy) showPrivacyPolicy = false
                }

                if (showPrivacyPolicy) {
                    PrivacyPolicyScreen(onBack = { showPrivacyPolicy = false })
                } else {
                    Scaffold(
                        topBar = {
                            Surface(
                                color = colors.background.copy(alpha = 0.95f),
                                modifier = Modifier.statusBarsPadding()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = { finish() }) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
                                    }
                                    
                                    Text(
                                        text = "@${myUsername.lowercase()}", 
                                        style = MaterialTheme.typography.titleMedium, 
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    
                                    Spacer(Modifier.width(48.dp)) // Spacer to keep title centered
                                }
                            }
                        },
                        containerColor = colors.background
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Profile Info Section
                                item {
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Profile Photo
                                        Box(contentAlignment = Alignment.BottomEnd) {
                                            Surface(
                                                shape = CircleShape,
                                                border = BorderStroke(2.dp, colors.separator.copy(alpha = 0.5f)),
                                                modifier = Modifier.size(90.dp).clickable { fullScreenPhotoUrl = (selectedImageUri ?: myPhotoUrl)?.toString() }
                                            ) {
                                                AsyncImage(
                                                    model = selectedImageUri ?: myPhotoUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }

                                        // Stats Row
                                        Row(
                                            modifier = Modifier.weight(1f).padding(start = 24.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            StatItem(label = "Posts", count = "${myPosts.size}", colors = colors)
                                            StatItem(label = "Amigos", count = "${myContacts.size}", colors = colors)
                                        }
                                    }

                                    // Bio / Status
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = nameInput.ifBlank { "Wappi User" },
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = statusInput.ifBlank { "Sem biografia" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textPrimary
                                        )
                                    }

                                    // Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { showSettingsMenu = true },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryBackground),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Editar Perfil", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Button(
                                            onClick = {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "Converse comigo no Wappi Messenger! Meu usuário é @$myUsername")
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar perfil"))
                                            },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryBackground),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Compartilhar", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }

                                    Spacer(Modifier.height(24.dp))
                                    HorizontalDivider(thickness = 0.5.dp, color = colors.separator.copy(0.3f))
                                }

                                // Tabs for Posts
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedTab = 0 }
                                                .padding(vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Rounded.GridView, 
                                                null, 
                                                tint = if (selectedTab == 0) colors.textPrimary else colors.textSecondary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            if (selectedTab == 0) {
                                                Spacer(Modifier.height(4.dp))
                                                Box(Modifier.height(2.dp).width(40.dp).background(colors.textPrimary))
                                            }
                                        }
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedTab = 1 }
                                                .padding(vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Rounded.ViewDay, 
                                                null, 
                                                tint = if (selectedTab == 1) colors.textPrimary else colors.textSecondary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            if (selectedTab == 1) {
                                                Spacer(Modifier.height(4.dp))
                                                Box(Modifier.height(2.dp).width(40.dp).background(colors.textPrimary))
                                            }
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = colors.separator.copy(0.2f))
                                }

                                // The Focus: My Posts
                                if (myPosts.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                border = BorderStroke(1.dp, colors.textPrimary.copy(alpha = 0.5f)),
                                                color = Color.Transparent,
                                                modifier = Modifier.size(64.dp)
                                            ) {
                                                Icon(Icons.Rounded.CameraAlt, null, tint = colors.textPrimary, modifier = Modifier.padding(16.dp))
                                            }
                                            Spacer(Modifier.height(16.dp))
                                            Text("Ainda não há publicações", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                        }
                                    }
                                } else {
                                    if (selectedTab == 0) {
                                        // Grid View
                                        val rows = myPosts.chunked(3)
                                        items(rows.size) { rowIndex ->
                                            val rowPosts = rows[rowIndex]
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                rowPosts.forEachIndexed { colIndex, post ->
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .padding(1.dp)
                                                            .clickable { selectedTab = 1 } // Go to list view on click? or open viewer?
                                                    ) {
                                                        if (!post.photoUrl.isNullOrEmpty()) {
                                                            AsyncImage(
                                                                model = post.photoUrl,
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                            if (post.mediaType == "VIDEO_FEED") {
                                                                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp))
                                                            }
                                                        } else if (!post.animatedEmoji.isNullOrEmpty()) {
                                                            Box(Modifier.fillMaxSize().background(colors.tertiaryBackground), contentAlignment = Alignment.Center) {
                                                                Text("✨", fontSize = 24.sp)
                                                            }
                                                        } else {
                                                            Box(
                                                                Modifier
                                                                    .fillMaxSize()
                                                                    .background(colors.tertiaryBackground)
                                                                    .padding(8.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(post.text, maxLines = 3, fontSize = 10.sp, color = colors.textPrimary, textAlign = TextAlign.Center)
                                                            }
                                                        }
                                                    }
                                                }
                                                // Fill empty slots in the row
                                                repeat(3 - rowPosts.size) {
                                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                                }
                                            }
                                        }
                                    } else {
                                        // List View
                                        items(myPosts.size) { index ->
                                            FeedPostCard(
                                                post = myPosts[index],
                                                isAuthorOnline = true,
                                                myUsername = myUsername,
                                                myPhotoUrl = myPhotoUrl,
                                                viewModel = viewModel,
                                                onAuthorClick = { }, // Já está no seu próprio perfil
                                                onImageClick = { fullScreenPhotoUrl = it }
                                            )
                                        }
                                    }
                                }

                                 item { Spacer(Modifier.height(100.dp)) }
                            }

                            // Settings Bottom Sheet
                            if (showSettingsMenu) {
                                ModalBottomSheet(
                                    onDismissRequest = { showSettingsMenu = false },
                                    containerColor = colors.background,
                                    tonalElevation = 8.dp,
                                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 48.dp)
                                            .verticalScroll(rememberScrollState()),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { showSettingsMenu = false }) {
                                                Text("Cancelar", color = colors.textSecondary)
                                            }
                                            Text(
                                                "Editar Perfil",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            TextButton(
                                                onClick = {
                                                    isSaving = true
                                                    viewModel.updateProfile(
                                                        name = nameInput,
                                                        imageUri = selectedImageUri,
                                                        status = statusInput,
                                                        presenceStatus = selectedPresence,
                                                        privacySettings = mapOf("isHiddenFromSearch" to hideFromSearch)
                                                    ) { success ->
                                                        isSaving = false
                                                        if (success) {
                                                            Toast.makeText(context, "Perfil atualizado!", Toast.LENGTH_SHORT).show()
                                                            showSettingsMenu = false
                                                        }
                                                    }
                                                },
                                                enabled = !isSaving
                                            ) {
                                                if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MessengerBlue)
                                                else Text("Concluir", color = MessengerBlue, fontWeight = FontWeight.ExtraBold)
                                            }
                                        }

                                        Spacer(Modifier.height(16.dp))

                                        // Edit Photo Section
                                        Box(
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clickable { photoLauncher.launch("image/*") },
                                            contentAlignment = Alignment.BottomEnd
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                border = BorderStroke(2.dp, colors.separator.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                AsyncImage(
                                                    model = selectedImageUri ?: myPhotoUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Surface(
                                                modifier = Modifier.size(28.dp),
                                                shape = CircleShape,
                                                color = MessengerBlue
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        
                                        TextButton(onClick = { photoLauncher.launch("image/*") }) {
                                            Text("Alterar foto", color = MessengerBlue, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(Modifier.height(16.dp))

                                        MetaSettingsGroup(title = "Minha Conta", colors = colors) {
                                            ProfileEditRow(label = "Nome", value = nameInput, onValueChange = { nameInput = it }, icon = Icons.Rounded.Person, colors = colors)
                                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = colors.separator.copy(0.4f))
                                            ProfileEditRow(label = "Bio", value = statusInput, onValueChange = { statusInput = it }, icon = Icons.Rounded.ChatBubbleOutline, colors = colors)
                                        }

                                        Spacer(Modifier.height(24.dp))

                                        MetaSettingsGroup(title = "Presença & Privacidade", colors = colors) {
                                            PresenceSelectorRow(selectedPresence, onClick = { showPresenceMenu = true }, colors = colors)
                                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = colors.separator.copy(0.4f))
                                            MetaSettingsSwitchItem(
                                                icon = Icons.Rounded.VisibilityOff,
                                                iconColor = Color.Gray,
                                                title = "Modo Fantasma",
                                                checked = hideFromSearch,
                                                onCheckedChange = { hideFromSearch = it }
                                            )
                                        }

                                        Spacer(Modifier.height(32.dp))

                                        TextButton(
                                            onClick = { 
                                                viewModel.logout()
                                                val intent = Intent(context, MainActivity::class.java).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                }
                                                context.startActivity(intent)
                                                finish()
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                                        ) {
                                            Text("Sair da Conta", color = iOSRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                            // Photo Viewer
                            if (fullScreenPhotoUrl != null) {
                                MediaViewerScreen(
                                    mediaItem = MediaViewerItem.Image(fullScreenPhotoUrl!!),
                                    onDismiss = { fullScreenPhotoUrl = null }
                                )
                            }
                            // Presence Selection Bottom Sheet
                            if (showPresenceMenu) {
                                ModalBottomSheet(
                                    onDismissRequest = { showPresenceMenu = false },
                                    containerColor = colors.background,
                                    tonalElevation = 8.dp
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                                    ) {
                                        Text(
                                            "Status de Presença",
                                            modifier = Modifier.padding(16.dp),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        
                                        listOf("Online", "Ocupado", "Ausente", "Invisível").forEach { status ->
                                            val color = when(status) {
                                                "Online" -> iOSGreen
                                                "Ocupado" -> iOSRed
                                                "Ausente" -> iOSOrange
                                                else -> Color.Gray
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { 
                                                        selectedPresence = status
                                                        showPresenceMenu = false
                                                    }
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                                                Spacer(Modifier.width(16.dp))
                                                Text(status, color = colors.textPrimary, fontWeight = if (selectedPresence == status) FontWeight.Bold else FontWeight.Normal)
                                                Spacer(Modifier.weight(1f))
                                                if (selectedPresence == status) {
                                                    Icon(Icons.Rounded.Check, null, tint = MessengerBlue)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } // End of Box
                    } // End of Scaffold content
                } // End of else
            } // End of FriendTheme
        } // End of setContent
    } // End of onCreate
} // End of ProfileActivity

@Composable
private fun StatItem(label: String, count: String, colors: ChatColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
        Text(text = label, fontSize = 13.sp, color = colors.textPrimary)
    }
}

@Composable
private fun MetaSettingsGroup(title: String, colors: ChatColors, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            modifier = Modifier.padding(start = 28.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            fontWeight = FontWeight.Bold
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = colors.secondaryBackground,
            tonalElevation = 1.dp
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun ProfileEditRow(label: String, value: String, onValueChange: (String) -> Unit, icon: ImageVector, colors: ChatColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MessengerBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MessengerBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MessengerBlue)
            )
        }
    }
}

@Composable
private fun PresenceSelectorRow(selectedPresence: String, onClick: () -> Unit, colors: ChatColors) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = when(selectedPresence) {
            "Online" -> iOSGreen
            "Ocupado" -> iOSRed
            "Ausente" -> iOSOrange
            else -> Color.Gray
        }
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Status de Presença", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Text(selectedPresence, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary)
    }
}

@Composable
private fun ActionItemRow(label: String, icon: ImageVector, iconColor: Color, onClick: () -> Unit, colors: ChatColors) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), color = colors.textPrimary)
        Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary)
    }
}
