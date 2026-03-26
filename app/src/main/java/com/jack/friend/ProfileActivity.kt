package com.jack.friend

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jack.friend.ui.chat.MediaViewerItem
import com.jack.friend.ui.chat.MediaViewerScreen
import com.jack.friend.ui.chat.StatusViewer
import com.jack.friend.ui.profile.PrivacyPolicyScreen
import com.jack.friend.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {
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
                val myPresenceStatus by viewModel.myPresenceStatus.collectAsStateWithLifecycle(stringResource(R.string.status_online))
                val isHiddenFromSearch by viewModel.isHiddenFromSearch.collectAsStateWithLifecycle(false)
                
                val myContacts by viewModel.contacts.collectAsStateWithLifecycle(emptyList())
                val feedPosts by viewModel.feedPosts.collectAsStateWithLifecycle(emptyList())
                val myPosts = feedPosts.filter { it.authorId == myUsername }
                val statuses by viewModel.statuses.collectAsStateWithLifecycle(emptyList())
                val myStatuses = statuses.filter { it.userId == myUsername }

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
                
                var viewingMyStatuses by remember { mutableStateOf<List<UserStatus>?>(null) }
                var showMyQRSheet by remember { mutableStateOf(false) }

                var nameLoaded by remember { mutableStateOf(false) }
                var statusLoaded by remember { mutableStateOf(false) }

                LaunchedEffect(myName) {
                    if (!nameLoaded && myName.isNotEmpty()) {
                        nameInput = myName
                        nameLoaded = true
                    }
                }
                
                LaunchedEffect(myStatus) {
                    if (!statusLoaded && myStatus.isNotEmpty()) {
                        statusInput = myStatus
                        statusLoaded = true
                    }
                }
                
                LaunchedEffect(isHiddenFromSearch) {
                    hideFromSearch = isHiddenFromSearch
                }

                LaunchedEffect(myPresenceStatus) { selectedPresence = myPresenceStatus }

                val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    selectedImageUri = uri
                }

                BackHandler(enabled = showPrivacyPolicy || fullScreenPhotoUrl != null || showSettingsMenu || viewingMyStatuses != null || showMyQRSheet) {
                    if (fullScreenPhotoUrl != null) fullScreenPhotoUrl = null
                    else if (viewingMyStatuses != null) viewingMyStatuses = null
                    else if (showMyQRSheet) showMyQRSheet = false
                    else if (showSettingsMenu) showSettingsMenu = false
                    else if (showPrivacyPolicy) showPrivacyPolicy = false
                }

                if (showPrivacyPolicy) {
                    PrivacyPolicyScreen(onBack = { showPrivacyPolicy = false })
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = colors.background,
                        topBar = {
                            PremiumProfileTopBar(
                                username = myUsername,
                                colors = colors,
                                onBack = { finish() },
                                onQRClick = { showMyQRSheet = true },
                                onSettingsClick = { 
                                    context.startActivity(Intent(context, SettingsActivity::class.java))
                                    finish()
                                }
                            )
                        }
                    ) { innerPadding ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = innerPadding.calculateTopPadding()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Hero Section with Blurred Background
                            item {
                                HeroSection(
                                    photoUrl = (selectedImageUri ?: myPhotoUrl)?.toString(),
                                    name = myName,
                                    username = myUsername,
                                    bio = statusInput,
                                    presenceStatus = selectedPresence,
                                    postsCount = myPosts.size,
                                    friendsCount = myContacts.size,
                                    hasStory = myStatuses.isNotEmpty(),
                                    colors = colors,
                                    onPhotoClick = {
                                        if (myStatuses.isNotEmpty()) viewingMyStatuses = myStatuses
                                        else fullScreenPhotoUrl = (selectedImageUri ?: myPhotoUrl)?.toString()
                                    },
                                    onEditClick = { showSettingsMenu = true },
                                    onShareClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.profile_share_message, myUsername))
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.profile_share_chooser_title)))
                                    }
                                )
                            }

                            // Tabs
                            item {
                                ProfileTabs(
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it },
                                    colors = colors
                                )
                            }

                            // Content based on Tab
                            if (myPosts.isEmpty()) {
                                item { EmptyPostsState(colors = colors) }
                            } else {
                                if (selectedTab == 0) {
                                    // Grid implementation using chunked for LazyColumn (since LazyVerticalGrid inside Scrollable is tricky)
                                    val rows = myPosts.chunked(3)
                                    items(rows.size) { rowIndex ->
                                        Row(Modifier.fillMaxWidth()) {
                                            rows[rowIndex].forEach { post ->
                                                ProfileGridItem(post = post, colors = colors, onClick = { selectedTab = 1 })
                                            }
                                            // Spacers for incomplete rows
                                            repeat(3 - rows[rowIndex].size) {
                                                Spacer(Modifier.weight(1f).aspectRatio(1f))
                                            }
                                        }
                                    }
                                } else {
                                    items(myPosts.size) { index ->
                                        FeedPostCard(
                                            post = myPosts[index],
                                            isAuthorOnline = true,
                                            myUsername = myUsername,
                                            myPhotoUrl = myPhotoUrl,
                                            viewModel = viewModel,
                                            onAuthorClick = { },
                                            onImageClick = { fullScreenPhotoUrl = it }
                                        )
                                    }
                                }
                            }

                            item { Spacer(Modifier.height(100.dp)) }
                        }

                        // Sheets (Settings, QR, Presence, etc.)
                        if (showSettingsMenu) {
                            EditProfileSheet(
                                name = nameInput,
                                bio = statusInput,
                                photoUrl = (selectedImageUri ?: myPhotoUrl),
                                presence = selectedPresence,
                                ghostMode = hideFromSearch,
                                colors = colors,
                                onDismiss = { showSettingsMenu = false },
                                onPhotoChange = { photoLauncher.launch("image/*") },
                                onUpdate = { n, b, p, g ->
                                    isSaving = true
                                    viewModel.updateProfile(n, (selectedImageUri), b, p, mapOf("isHiddenFromSearch" to g)) { s ->
                                        isSaving = false
                                        if (s) {
                                            nameInput = n
                                            statusInput = b
                                            selectedPresence = p
                                            hideFromSearch = g
                                            showSettingsMenu = false
                                            Toast.makeText(context, context.getString(R.string.profile_update_success), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                isSaving = isSaving,
                                onLogout = {
                                    viewModel.logout()
                                    context.startActivity(Intent(context, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    })
                                    finish()
                                },
                                onPresenceClick = { showPresenceMenu = true },
                                myUsername = myUsername
                            )
                        }

                        if (showMyQRSheet) {
                            MyQRSheet(
                                userId = myUsername,
                                displayName = nameInput.ifBlank { myUsername },
                                photoUrl = myPhotoUrl,
                                onDismiss = { showMyQRSheet = false }
                            )
                        }

                        if (viewingMyStatuses != null) {
                            StatusViewer(
                                userStatuses = viewingMyStatuses!!,
                                myUsername = myUsername,
                                viewModel = viewModel,
                                onClose = { viewingMyStatuses = null },
                                onDelete = { id -> viewModel.deleteStatus(id) }
                            )
                        }

                        if (fullScreenPhotoUrl != null) {
                            MediaViewerScreen(
                                mediaItem = MediaViewerItem.Image(fullScreenPhotoUrl!!),
                                onDismiss = { fullScreenPhotoUrl = null }
                            )
                        }

                        if (showPresenceMenu) {
                            PresenceSheet(
                                selected = selectedPresence,
                                colors = colors,
                                onSelected = { 
                                    selectedPresence = it
                                    showPresenceMenu = false 
                                },
                                onDismiss = { showPresenceMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumProfileTopBar(
    username: String,
    colors: ChatColors,
    onBack: () -> Unit,
    onQRClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = colors.background.copy(alpha = 0.9f),
        modifier = Modifier.statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
            }
            Text(
                text = "@${username.lowercase()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary
            )
            Row {
                IconButton(onClick = onQRClick) {
                    Icon(Icons.Rounded.QrCode, null, tint = colors.textPrimary, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Rounded.Settings, null, tint = colors.textPrimary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    photoUrl: String?,
    name: String,
    username: String,
    bio: String,
    presenceStatus: String,
    postsCount: Int,
    friendsCount: Int,
    hasStory: Boolean,
    colors: ChatColors,
    onPhotoClick: () -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper part: Blur + Avatar
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Blurred Background
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(30.dp).alpha(0.3f),
                contentScale = ContentScale.Crop
            )
            
            // Avatar
            Surface(
                modifier = Modifier
                    .size(110.dp)
                    .offset(y = 20.dp),
                shape = CircleShape,
                border = if (hasStory) BorderStroke(3.dp, Brush.linearGradient(InstagramStoryBorder)) else BorderStroke(2.dp, colors.separator.copy(0.3f)),
                color = colors.secondaryBackground
            ) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape).clickable { onPhotoClick() },
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Info
        Text(
            text = name.ifBlank { stringResource(R.string.profile_default_name) },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textPrimary
        )

        Text(
            text = "@${username.lowercase()}",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary.copy(0.7f),
            fontWeight = FontWeight.Bold
        )
        
        // Presence badge
        val presenceColor = when(presenceStatus) {
            "Online" -> iOSGreen
            "Ocupado" -> iOSRed
            "Ausente" -> iOSOrange
            else -> Color.Gray
        }
        Surface(
            color = presenceColor.copy(0.1f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Box(Modifier.size(6.dp).background(presenceColor, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(presenceStatus, style = MaterialTheme.typography.labelSmall, color = presenceColor, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HeroStat(count = "$postsCount", label = stringResource(R.string.profile_stat_posts), colors = colors)
            HeroStat(count = "$friendsCount", label = stringResource(R.string.profile_stat_friends), colors = colors)
        }

        Spacer(Modifier.height(24.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onEditClick,
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryBackground)
            ) {
                Text(stringResource(R.string.profile_edit_profile), color = colors.textPrimary, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onShareClick,
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryBackground)
            ) {
                Text(stringResource(R.string.profile_share_button), color = colors.textPrimary, fontWeight = FontWeight.Bold)
            }
        }

        if (bio.isNotBlank()) {
            Text(
                text = bio,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary.copy(0.8f)
            )
        } else {
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroStat(count: String, label: String, colors: ChatColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontWeight = FontWeight.Black, fontSize = 20.sp, color = colors.textPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

@Composable
private fun ProfileTabs(selectedTab: Int, onTabSelected: (Int) -> Unit, colors: ChatColors) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            TabIcon(
                icon = Icons.Rounded.GridView,
                isSelected = selectedTab == 0,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(0) },
                colors = colors
            )
            TabIcon(
                icon = Icons.Rounded.ViewDay,
                isSelected = selectedTab == 1,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(1) },
                colors = colors
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = colors.separator.copy(0.2f))
    }
}

@Composable
private fun TabIcon(icon: ImageVector, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit, colors: ChatColors) {
    Column(
        modifier = modifier.clickable { onClick() }.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            null,
            tint = if (isSelected) colors.primary else colors.textSecondary.copy(0.4f),
            modifier = Modifier.size(26.dp)
        )
        if (isSelected) {
            Spacer(Modifier.height(4.dp))
            Box(Modifier.height(2.dp).fillMaxWidth(0.4f).background(colors.primary, CircleShape))
        }
    }
}

@Composable
private fun RowScope.ProfileGridItem(post: FeedPost, colors: ChatColors, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(1.dp)
            .clickable { onClick() }
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
        } else {
            Box(
                Modifier.fillMaxSize().background(colors.secondaryBackground).padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(post.text, fontSize = 10.sp, color = colors.textPrimary, textAlign = TextAlign.Center, maxLines = 4)
            }
        }
    }
}

@Composable
private fun EmptyPostsState(colors: ChatColors) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.CameraEnhance, null, modifier = Modifier.size(48.dp), tint = colors.textSecondary.copy(0.3f))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.profile_empty_posts), fontWeight = FontWeight.Bold, color = colors.textSecondary)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditProfileSheet(
    name: String,
    bio: String,
    photoUrl: Any?,
    presence: String,
    ghostMode: Boolean,
    colors: ChatColors,
    onDismiss: () -> Unit,
    onPhotoChange: () -> Unit,
    onPresenceClick: () -> Unit,
    onUpdate: (String, String, String, Boolean) -> Unit,
    onLogout: () -> Unit,
    isSaving: Boolean,
    myUsername: String
) {
    var tempName by remember(name) { mutableStateOf(name) }
    var tempBio by remember(bio) { mutableStateOf(bio) }
    var tempGhost by remember(ghostMode) { mutableStateOf(ghostMode) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = colors.textSecondary) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.profile_edit_profile), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text("@${myUsername.lowercase()}", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
                TextButton(
                    onClick = { onUpdate(tempName, tempBio, presence, tempGhost) },
                    enabled = !isSaving
                ) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primary)
                    else Text(stringResource(R.string.profile_finish_button), color = colors.primary, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Avatar Edit
            Box(modifier = Modifier.align(Alignment.CenterHorizontally).size(110.dp)) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, colors.separator.copy(0.3f), CircleShape),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onPhotoChange,
                    modifier = Modifier.align(Alignment.BottomEnd).shadow(4.dp, CircleShape).background(colors.primary, CircleShape).size(34.dp)
                ) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            
            TextButton(
                onClick = onPhotoChange,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.profile_change_photo), color = colors.primary, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(30.dp))

            // Sections
            ProfileSettingsSection(title = stringResource(R.string.profile_section_account), colors = colors) {
                // Username (Read-only)
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AlternateEmail, null, tint = colors.textSecondary.copy(0.7f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.label_username), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        Text("@${myUsername.lowercase()}", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                    }
                }
                
                SettingsInputRow(label = stringResource(R.string.profile_label_name), value = tempName, onValueChange = { tempName = it }, icon = Icons.Rounded.Person, colors = colors)
                SettingsInputRow(label = stringResource(R.string.profile_label_bio), value = tempBio, onValueChange = { tempBio = it }, icon = Icons.Rounded.FormatQuote, colors = colors)
                
                // Account Email (Read-only)
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                val isGoogle = currentUser?.providerData?.any { it.providerId == "google.com" } ?: false
                
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isGoogle) Icons.Rounded.Mail else Icons.Rounded.AlternateEmail, null, tint = if (isGoogle) Color(0xFF4285F4) else colors.textSecondary.copy(0.7f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.label_email), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currentUser?.email ?: "", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                            if (isGoogle) {
                                Spacer(Modifier.width(8.dp))
                                Surface(color = Color(0xFF4285F4).copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                                    Text("Google", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4285F4), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            ProfileSettingsSection(title = stringResource(R.string.profile_section_presence), colors = colors) {
                SettingsClickRow(label = stringResource(R.string.profile_presence_status_title), value = presence, icon = Icons.Rounded.Circle, iconColor = getPresenceColor(presence), colors = colors, onClick = onPresenceClick)
                SettingsSwitchRow(label = stringResource(R.string.profile_ghost_mode), checked = tempGhost, icon = Icons.Rounded.VisibilityOff, colors = colors, onCheckedChange = { tempGhost = it })
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSRed.copy(0.1f))
            ) {
                Text(stringResource(R.string.profile_logout_button), color = iOSRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileSettingsSection(title: String, colors: ChatColors, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp), fontWeight = FontWeight.Bold)
        Surface(
            color = colors.secondaryBackground,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsInputRow(label: String, value: String, onValueChange: (String) -> Unit, icon: ImageVector, colors: ChatColors) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary, fontWeight = FontWeight.Medium),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary)
            )
        }
    }
}

@Composable
private fun SettingsClickRow(label: String, value: String, icon: ImageVector, iconColor: Color, colors: ChatColors, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary.copy(0.5f))
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, icon: ImageVector, colors: ChatColors, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), color = colors.textPrimary, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = colors.primary))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PresenceSheet(selected: String, colors: ChatColors, onSelected: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.background) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)) {
            Text(stringResource(R.string.profile_presence_status_title), modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = colors.textPrimary)
            val options = listOf("Online", "Ocupado", "Ausente", "Invisível")
            options.forEach { opt ->
                val presenceDisplayRes = when(opt) {
                    "Online" -> R.string.status_online
                    "Ocupado" -> R.string.status_busy
                    "Ausente" -> R.string.status_away
                    "Invisível" -> R.string.status_invisible
                    else -> R.string.status_online
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelected(opt) }.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(12.dp).background(getPresenceColor(opt), CircleShape))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(presenceDisplayRes), Modifier.weight(1f), color = colors.textPrimary, fontWeight = if (selected == opt) FontWeight.Bold else FontWeight.Normal)
                    if (selected == opt) Icon(Icons.Rounded.Check, null, tint = colors.primary)
                }
            }
        }
    }
}

private fun getPresenceColor(status: String) = when(status) {
    "Online" -> iOSGreen
    "Ocupado" -> iOSRed
    "Ausente" -> iOSOrange
    else -> Color.Gray
}
