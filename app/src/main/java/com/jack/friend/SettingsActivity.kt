package com.jack.friend

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jack.friend.ui.components.*
import com.jack.friend.ui.theme.*

import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var billingManager: BillingManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billingManager = BillingManager(this)
        setContent {
            FriendTheme {
                val viewModel: ChatViewModel = viewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    billingManager = billingManager,
                    onBack = { finish() },
                    activity = this@SettingsActivity
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    billingManager: BillingManager? = null,
    onBack: () -> Unit,
    activity: Activity? = null
) {
    val context = LocalContext.current

    val uiPrefs = remember { context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE) }
    var isDarkMode by remember { mutableStateOf(uiPrefs.getBoolean("dark_mode", false)) }
    var followSystem by remember { mutableStateOf(uiPrefs.getBoolean("follow_system", true)) }
    var selectedThemeName by remember { mutableStateOf(uiPrefs.getString("app_theme", AppTheme.DEFAULT.name) ?: AppTheme.DEFAULT.name) }

    val isPremium by (billingManager?.isPremiumPurchased?.collectAsStateWithLifecycle(false) ?: remember { mutableStateOf(false) })

    FriendTheme {
        val myName by viewModel.myName.collectAsStateWithLifecycle("")
        val myPhotoUrl by viewModel.myPhotoUrl.collectAsStateWithLifecycle(null)
        val myStatus by viewModel.myStatus.collectAsStateWithLifecycle("")
        val blockedProfiles by viewModel.blockedProfiles.collectAsStateWithLifecycle(emptyList())
        val isHiddenFromSearch by viewModel.isHiddenFromSearch.collectAsStateWithLifecycle(false)
        val showLastSeen by viewModel.showLastSeen.collectAsStateWithLifecycle(true)
        val showReadReceipts by viewModel.showReadReceipts.collectAsStateWithLifecycle(true)
        val showOnlineStatus by viewModel.showOnlineStatus.collectAsStateWithLifecycle(true)

        val securityPrefs = remember { context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE) }

        var isPinEnabled by remember { mutableStateOf(securityPrefs.getBoolean("pin_enabled", false)) }
        var isBiometricEnabled by remember { mutableStateOf(securityPrefs.getBoolean("biometric_enabled", false)) }

        var showPinDialog by remember { mutableStateOf(false) }
        var pinInput by remember { mutableStateOf("") }
        var showDeleteAccountDialog by remember { mutableStateOf(false) }
        var isDeletingAccount by remember { mutableStateOf(false) }
        var showBlockedDialog by remember { mutableStateOf(false) }
        var showThemePicker by remember { mutableStateOf(false) }
        var showClearCacheDialog by remember { mutableStateOf(false) }
        var showLanguageDialog by remember { mutableStateOf(false) }

        val themeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalChatColors.current.topBar)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    onClick = { context.startActivity(Intent(context, ProfileActivity::class.java)) },
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = myPhotoUrl,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(LocalChatColors.current.separator),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = myName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(text = myStatus, style = MaterialTheme.typography.bodyMedium, color = WarmTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                    }
                }

                if (!isPremium) {
                    MetaSettingsSection {
                        MetaSettingsItem(
                            title = stringResource(R.string.settings_premium_title),
                            icon = Icons.Default.Star,
                            iconColor = Color(0xFFFFD700),
                            subtitle = stringResource(R.string.settings_premium_sub),
                            onClick = {
                                Log.d("SettingsScreen", "Botão Remover Anúncios clicado. Activity: $activity")
                                if (activity != null) {
                                    billingManager?.launchPurchaseFlow(activity)
                                } else {
                                    Log.e("SettingsScreen", "Activity nula ao tentar iniciar compra")
                                    Toast.makeText(context, "Erro ao iniciar compra. Tente novamente.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                Text(stringResource(R.string.settings_appearance), modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = WarmPrimary, fontWeight = FontWeight.Bold)
                MetaSettingsSection {
                    MetaSettingsItem(
                        title = stringResource(R.string.settings_theme_title),
                        icon = Icons.Default.Palette,
                        iconColor = iOSPurple,
                        subtitle = AppTheme.valueOf(selectedThemeName).title,
                        onClick = { showThemePicker = true }
                    )
                    MetaSettingsDivider()
                    MetaSettingsSwitchItem(
                        icon = Icons.Default.SettingsSuggest,
                        iconColor = iOSBlue,
                        title = stringResource(R.string.settings_sync_system),
                        checked = followSystem,
                        onCheckedChange = {
                            followSystem = it
                            uiPrefs.edit().putBoolean("follow_system", it).apply()
                        }
                    )
                    MetaSettingsDivider()
                    MetaSettingsSwitchItem(
                        icon = Icons.Default.DarkMode,
                        iconColor = Color.DarkGray,
                        title = stringResource(R.string.settings_dark_mode),
                        checked = isDarkMode,
                        enabled = !followSystem,
                        onCheckedChange = {
                            isDarkMode = it
                            uiPrefs.edit().putBoolean("dark_mode", it).apply()
                        }
                    )
                    MetaSettingsDivider()
                    MetaSettingsItem(
                        title = stringResource(R.string.settings_language),
                        icon = Icons.Default.Language,
                        iconColor = iOSBlue,
                        subtitle = when (AppCompatDelegate.getApplicationLocales().get(0)?.language) {
                            "pt" -> "Português"
                            "en" -> "English"
                            "es" -> "Español"
                            "fr" -> "Français"
                            "de" -> "Deutsch"
                            "it" -> "Italiano"
                            "ru" -> "Русский"
                            "ar" -> "العربية"
                            "zh" -> "中文"
                            "hi" -> "हिन्दी"
                            "bn" -> "বাংলা"
                            "id", "in" -> "Bahasa Indonesia"
                            "ja" -> "日本語"
                            "ko" -> "한국어"
                            "tr" -> "Türkçe"
                            "ur" -> "اردو"
                            else -> stringResource(R.string.settings_sync_system)
                        },
                        onClick = { showLanguageDialog = true }
                    )
                }

                Text(stringResource(R.string.settings_privacy), modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = WarmPrimary, fontWeight = FontWeight.Bold)
                MetaSettingsSection {
                    MetaSettingsSwitchItem(
                        icon = Icons.Default.Timer,
                        iconColor = iOSBlue,
                        title = stringResource(R.string.settings_last_seen),
                        subtitle = stringResource(R.string.settings_last_seen_sub),
                        checked = showLastSeen,
                        onCheckedChange = { viewModel.updateProfile(privacySettings = mapOf("showLastSeen" to it)) }
                    )
                    MetaSettingsDivider()
                    MetaSettingsSwitchItem(
                        icon = Icons.Default.DoneAll,
                        iconColor = iOSBlue,
                        title = stringResource(R.string.settings_read_receipts),
                        subtitle = stringResource(R.string.settings_read_receipts_sub),
                        checked = showReadReceipts,
                        onCheckedChange = { viewModel.updateProfile(privacySettings = mapOf("showReadReceipts" to it)) }
                    )
                    MetaSettingsDivider()
                    MetaSettingsSwitchItem(
                        icon = Icons.Default.OnlinePrediction,
                        iconColor = iOSGreen,
                        title = stringResource(R.string.settings_online_status),
                        subtitle = stringResource(R.string.settings_online_status_sub),
                        checked = showOnlineStatus,
                        onCheckedChange = { viewModel.updateProfile(privacySettings = mapOf("showOnlineStatus" to it)) }
                    )
                    MetaSettingsDivider()
                    MetaSettingsItem(title = stringResource(R.string.settings_blocked_users), icon = Icons.Default.Block, iconColor = MessengerBusy, onClick = { showBlockedDialog = true })
                    MetaSettingsDivider()
                    MetaSettingsSwitchItem(
                        icon = Icons.Default.VisibilityOff,
                        iconColor = iOSBlue,
                        title = stringResource(R.string.settings_hide_search),
                        subtitle = stringResource(R.string.settings_hide_search_sub),
                        checked = isHiddenFromSearch,
                        onCheckedChange = { viewModel.updateProfile(privacySettings = mapOf("isHiddenFromSearch" to it)) }
                    )
                }

                Text(stringResource(R.string.settings_security), modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = WarmPrimary, fontWeight = FontWeight.Bold)
                MetaSettingsSection {
                    MetaSettingsSwitchItem(icon = Icons.Default.Lock, iconColor = WarmTextSecondary, title = stringResource(R.string.settings_pin_lock), checked = isPinEnabled, onCheckedChange = {
                        if (it) showPinDialog = true else {
                            isPinEnabled = false
                            securityPrefs.edit().putBoolean("pin_enabled", false).apply()
                        }
                    })
                    if (isPinEnabled) {
                        MetaSettingsDivider()
                        MetaSettingsItem(title = stringResource(R.string.settings_biometrics), icon = Icons.Default.Fingerprint, iconColor = WarmPrimary, trailing = {
                            Switch(checked = isBiometricEnabled, onCheckedChange = {
                                isBiometricEnabled = it
                                securityPrefs.edit().putBoolean("biometric_enabled", it).apply()
                            }, colors = SwitchDefaults.colors(checkedTrackColor = WarmPrimary))
                        })
                    }
                }

                Text(stringResource(R.string.settings_data), modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = WarmPrimary, fontWeight = FontWeight.Bold)
                MetaSettingsSection {
                    MetaSettingsItem(
                        title = stringResource(R.string.settings_clear_cache),
                        icon = Icons.Default.CleaningServices,
                        iconColor = iOSOrange,
                        subtitle = stringResource(R.string.settings_clear_cache_sub),
                        onClick = { showClearCacheDialog = true }
                    )
                }

                Text(stringResource(R.string.settings_account), modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = WarmPrimary, fontWeight = FontWeight.Bold)
                MetaSettingsSection {
                    MetaSettingsItem(title = stringResource(R.string.settings_logout), icon = Icons.AutoMirrored.Filled.Logout, iconColor = WarmTextSecondary, onClick = {
                        viewModel.logout()
                        val intent = Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        context.startActivity(intent)
                    })
                    MetaSettingsDivider()
                    MetaSettingsItem(title = stringResource(R.string.settings_delete_account), icon = Icons.Default.DeleteForever, iconColor = MessengerBusy, textColor = MessengerBusy, onClick = { showDeleteAccountDialog = true })
                }
                Spacer(Modifier.height(130.dp))
            }
        }

        if (showThemePicker) {
            ModalBottomSheet(
                onDismissRequest = { showThemePicker = false },
                sheetState = themeSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        "Personalize seu Estilo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 20.dp, start = 8.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.heightIn(max = 500.dp)
                    ) {
                        items(AppTheme.entries) { theme ->
                            val themeColors = getThemeColors(theme, isDarkMode)
                            ThemeCardPreview(
                                theme = theme,
                                isSelected = selectedThemeName == theme.name,
                                colors = themeColors,
                                onSelect = {
                                    selectedThemeName = theme.name
                                    uiPrefs.edit().putString("app_theme", theme.name).apply()
                                    showThemePicker = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showBlockedDialog) {
            AlertDialog(
                onDismissRequest = { showBlockedDialog = false },
                title = { Text(stringResource(R.string.settings_dialog_blocked_title)) },
                text = {
                    if (blockedProfiles.isEmpty()) {
                        Text(stringResource(R.string.settings_dialog_blocked_empty))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(blockedProfiles) { profile ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        AsyncImage(model = profile.photoUrl, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape).background(LocalChatColors.current.separator), contentScale = ContentScale.Crop)
                                        Spacer(Modifier.width(12.dp))
                                        Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    TextButton(onClick = { viewModel.unblockUser(profile.id) }) {
                                        Text(stringResource(R.string.action_unblock), color = WarmPrimary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showBlockedDialog = false }) { Text(stringResource(R.string.action_close)) } }
            )
        }

        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
                title = { Text(stringResource(R.string.settings_dialog_delete_title)) },
                text = { Text(stringResource(R.string.settings_dialog_delete_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isDeletingAccount = true
                            viewModel.deleteAccount { success, error ->
                                isDeletingAccount = false
                                if (success) {
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, error ?: "Erro ao excluir conta. Tente sair e entrar novamente.", Toast.LENGTH_LONG).show()
                                }
                                showDeleteAccountDialog = false
                            }
                        },
                        enabled = !isDeletingAccount
                    ) {
                        if (isDeletingAccount) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MessengerBusy)
                        } else {
                            Text(stringResource(R.string.action_delete), color = MessengerBusy)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }, enabled = !isDeletingAccount) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        if (showPinDialog) {
            AlertDialog(onDismissRequest = { showPinDialog = false }, title = { Text(stringResource(R.string.settings_dialog_pin_title)) },
                text = { TextField(value = pinInput, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it }, placeholder = { Text(stringResource(R.string.settings_dialog_pin_placeholder)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()) },
                confirmButton = { TextButton(onClick = { if (pinInput.length == 4) {
                    securityPrefs.edit().putBoolean("pin_enabled", true).putString("security_pin", pinInput).apply()
                    isPinEnabled = true
                    showPinDialog = false
                    pinInput = ""
                } }) { Text(stringResource(R.string.profile_finish_button)) } }
            )
        }

        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text(stringResource(R.string.settings_dialog_cache_title)) },
                text = { Text(stringResource(R.string.settings_dialog_cache_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        (context.applicationContext as? FriendApplication)?.clearAppData()
                        Toast.makeText(context, context.getString(R.string.settings_toast_cache_cleared), Toast.LENGTH_SHORT).show()
                        showClearCacheDialog = false
                    }) { Text(stringResource(R.string.action_clear)) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) { Text(stringResource(R.string.action_cancel)) }
                }
            )
        }

        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text(stringResource(R.string.settings_language)) },
                text = {
                    val currentLang = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: ""
                    val syncSystemText = stringResource(R.string.settings_sync_system)
                    val languages = listOf(
                        "" to syncSystemText,
                        "pt" to "Português",
                        "en" to "English",
                        "es" to "Español",
                        "fr" to "Français",
                        "de" to "Deutsch",
                        "it" to "Italiano",
                        "ru" to "Русский",
                        "ar" to "العربية",
                        "zh" to "中文",
                        "hi" to "हिन्दी",
                        "bn" to "বাংলা",
                        "id" to "Bahasa Indonesia",
                        "ja" to "日本語",
                        "ko" to "한국어",
                        "tr" to "Türkçe",
                        "ur" to "اردو"
                    )
                    LazyColumn {
                        items(languages) { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val appLocale = if (lang.first.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(lang.first)
                                        AppCompatDelegate.setApplicationLocales(appLocale)
                                        showLanguageDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = currentLang == lang.first, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(lang.second)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun ThemeCardPreview(
    theme: AppTheme,
    isSelected: Boolean,
    colors: ChatColors,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f)
    val borderWidth by animateFloatAsState(if (isSelected) 3f else 0f)

    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .aspectRatio(0.85f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.background),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = borderWidth.dp,
                    color = colors.primary,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Mini Chat Preview
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp, 8.dp, 8.dp, 2.dp))
                            .background(colors.bubbleOther)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(8.dp, 8.dp, 2.dp, 8.dp))
                            .background(colors.bubbleMe)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        colors.waveColors.forEach { color ->
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        theme.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        )
                    }
                }
            }
        }
    }
}
