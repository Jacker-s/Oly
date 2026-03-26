package com.jack.friend.ui.chat

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import coil.compose.AsyncImage
import com.jack.friend.ChatViewModel
import com.jack.friend.MetaTextField
import com.jack.friend.ui.theme.LocalChatColors
import androidx.compose.ui.res.stringResource
import com.jack.friend.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.jack.friend.getGoogleSignInClient
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

@Composable
fun LoginScreen(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE) }

    var email by remember { mutableStateOf(prefs.getString("saved_email", "") ?: "") }
    var password by remember { mutableStateOf(prefs.getString("saved_password", "") ?: "") }
    var username by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSignUp by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var showResetPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(prefs.getBoolean("remember_me", false)) }
    var showEmailForm by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> selectedImageUri = uri }
    val chatColors = LocalChatColors.current

    val myId by viewModel.myId.collectAsStateWithLifecycle()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    val myUsername by viewModel.myUsername.collectAsStateWithLifecycle()

    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                loading = true
                viewModel.signInWithGoogle(idToken) { success, error ->
                    loading = false
                    if (!success) {
                        Toast.makeText(context, error ?: context.getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    if (isUserLoggedIn && myUsername.isEmpty()) {
        InitialProfileSetupScreen(viewModel)
    } else {
    Box(modifier = Modifier.fillMaxSize().background(chatColors.background)) {
        // Decorative Background Elements (Premium Feel)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            chatColors.primary.copy(alpha = 0.08f),
                            chatColors.background,
                            chatColors.primary.copy(alpha = 0.03f)
                        )
                    )
                )
        )

        // Floating Blur Circles (Simulated Glassmorphism background)
        Box(
            modifier = Modifier
                .offset(x = (-50).dp, y = (-50).dp)
                .size(300.dp)
                .background(chatColors.primary.copy(alpha = 0.05f), CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .size(400.dp)
                .background(chatColors.primary.copy(alpha = 0.03f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(70.dp))

            // Logo Section with Premium Glow
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier
                        .size(90.dp)
                        .shadow(20.dp, RoundedCornerShape(26.dp), spotColor = chatColors.primary),
                    shape = RoundedCornerShape(26.dp),
                    color = chatColors.primary,
                    tonalElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.ChatBubble,
                            null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                }
                
                // Subtle Ring around logo
                Box(
                    modifier = Modifier
                        .size(115.dp)
                        .border(1.dp, chatColors.primary.copy(alpha = 0.15f), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Oly Messenger",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = chatColors.textPrimary
                )
            )
            
            Text(
                if (isSignUp) stringResource(R.string.signup_subtitle) else stringResource(R.string.login_welcome),
                style = MaterialTheme.typography.bodyMedium,
                color = chatColors.textSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            AnimatedContent(
                targetState = showEmailForm,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 40 })).togetherWith(
                        fadeOut(animationSpec = tween(400)) + slideOutVertically(targetOffsetY = { -40 })
                    )
                },
                label = "login_mode_transition"
            ) { isEmailMode ->
                if (!isEmailMode) {
                    // Initial Selection Mode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SocialLoginButton(
                            painter = painterResource(R.drawable.ic_google),
                            text = "Entrar com Google",
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            onClick = { 
                                val client = getGoogleSignInClient(context)
                                client.signOut().addOnCompleteListener {
                                    googleSignInLauncher.launch(client.signInIntent)
                                }
                            }
                        )
                        
                        SocialLoginButton(
                            painter = rememberVectorPainter(Icons.Rounded.Email),
                            text = "Login com E-mail",
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            onClick = { showEmailForm = true }
                        )
                    }
                } else {
                    // Email Form Mode
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Main Input Container (Glassy Look)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            color = chatColors.secondaryBackground.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, chatColors.primary.copy(alpha = 0.05f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedContent(
                                    targetState = isSignUp,
                                    transitionSpec = {
                                        (fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300))) togetherWith
                                        (fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300)))
                                    },
                                    label = "auth_toggle"
                                ) { signUp ->
                                    if (signUp) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(110.dp)
                                                    .clip(CircleShape)
                                                    .background(chatColors.tertiaryBackground)
                                                    .clickable { photoLauncher.launch("image/*") }
                                                    .border(2.dp, if (selectedImageUri != null) chatColors.primary else chatColors.primary.copy(0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (selectedImageUri != null) {
                                                    AsyncImage(
                                                        model = selectedImageUri,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Rounded.AddAPhoto, null, tint = chatColors.primary, modifier = Modifier.size(30.dp))
                                                        Text(stringResource(R.string.label_photo), fontSize = 11.sp, color = chatColors.primary, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            MetaTextField(
                                                value = username,
                                                onValueChange = { if (!it.contains(".")) username = it },
                                                placeholder = stringResource(R.string.label_username),
                                                icon = Icons.Rounded.AlternateEmail
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }

                                MetaTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    placeholder = stringResource(R.string.label_email),
                                    icon = Icons.Rounded.Email,
                                    keyboardType = KeyboardType.Email
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                MetaTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    placeholder = stringResource(R.string.label_password),
                                    icon = Icons.Rounded.Lock,
                                    isPassword = true
                                )

                                if (!isSignUp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                modifier = Modifier.size(20.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (rememberMe) chatColors.primary else chatColors.tertiaryBackground,
                                                onClick = { rememberMe = !rememberMe }
                                            ) {
                                                if (rememberMe) {
                                                    Icon(Icons.Rounded.Check, null, modifier = Modifier.padding(2.dp), tint = Color.White)
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.label_remember_me), fontSize = 14.sp, color = chatColors.textSecondary, fontWeight = FontWeight.Medium)
                                        }
                                        TextButton(onClick = { showResetPassword = true }) {
                                            Text(stringResource(R.string.action_forgot_password_short), color = chatColors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                if (loading) {
                                    Box(modifier = Modifier.height(56.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = chatColors.primary, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            loading = true
                                            if (isSignUp) viewModel.signUp(email, password, username, selectedImageUri) { s, e -> loading = false; if (!s) Toast.makeText(context, e ?: context.getString(R.string.error_generic), Toast.LENGTH_SHORT).show() }
                                            else viewModel.login(email, password) { s, e ->
                                                loading = false
                                                if (!s) {
                                                    Toast.makeText(context, e ?: context.getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                                                } else {
                                                    if (rememberMe) {
                                                        prefs.edit {
                                                            putString("saved_email", email)
                                                            putString("saved_password", password)
                                                            putBoolean("remember_me", true)
                                                        }
                                                    } else {
                                                        prefs.edit { clear() }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(58.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = chatColors.primary),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
                                    ) {
                                        Text(
                                            if (isSignUp) stringResource(R.string.action_signup) else stringResource(R.string.action_login),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        TextButton(
                            onClick = { isSignUp = !isSignUp },
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isSignUp) stringResource(R.string.login_switch_to_login_question) else stringResource(R.string.login_switch_to_signup_question),
                                    color = chatColors.textSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (isSignUp) stringResource(R.string.login_switch_to_login_action) else stringResource(R.string.login_switch_to_signup_action),
                                    color = chatColors.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        
                        TextButton(
                            onClick = { showEmailForm = false }
                        ) {
                            Text(
                                "Voltar para opções de login",
                                color = chatColors.textSecondary.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
    }

    if (showResetPassword) {
        var resetEmail by remember { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = { showResetPassword = false },
            title = { Text(stringResource(R.string.dialog_reset_password_title), fontWeight = FontWeight.Bold, color = chatColors.textPrimary) },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_reset_password_message), color = chatColors.textSecondary)
                    Spacer(Modifier.height(20.dp))
                    MetaTextField(resetEmail, { resetEmail = it }, stringResource(R.string.label_email_reset), Icons.Rounded.Email)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) viewModel.resetPassword(resetEmail) { s, e ->
                            if (s) { Toast.makeText(context, context.getString(R.string.toast_link_sent), Toast.LENGTH_LONG).show(); showResetPassword = false }
                            else Toast.makeText(context, e ?: context.getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = chatColors.primary)
                ) {
                    Text(stringResource(R.string.action_send_link), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPassword = false }) {
                    Text(stringResource(R.string.action_cancel), color = chatColors.textSecondary)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = chatColors.secondaryBackground
        )
    }
}

@Composable
fun SocialLoginButton(
    painter: Painter,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, chatColors.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(painter = painter, contentDescription = "Social Logo", modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                text, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold, 
                color = chatColors.textPrimary
            )
        }
    }
}


@Composable
fun InitialProfileSetupScreen(viewModel: ChatViewModel) {
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedImageUri = it }
    val chatColors = LocalChatColors.current

    Box(modifier = Modifier.fillMaxSize().background(chatColors.background)) {
        // Background Elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            chatColors.primary.copy(alpha = 0.05f),
                            chatColors.background,
                            chatColors.primary.copy(alpha = 0.02f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            
            // Top Back Button
            Box(Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Voltar",
                        tint = chatColors.textPrimary
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
            
            Text(
                stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = chatColors.textPrimary
            )
            
            Text(
                stringResource(R.string.setup_subtitle),
                textAlign = TextAlign.Center,
                color = chatColors.textSecondary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(Modifier.height(56.dp))
            
            // Premium Photo Picker
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(chatColors.tertiaryBackground)
                    .clickable { photoLauncher.launch("image/*") }
                    .border(3.dp, chatColors.primary.copy(alpha = 0.2f), CircleShape)
                    .shadow(12.dp, CircleShape, spotColor = chatColors.primary),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.AddAPhoto,
                            null,
                            modifier = Modifier.size(44.dp),
                            tint = chatColors.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.setup_label_photo),
                            fontSize = 12.sp,
                            color = chatColors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Active indicator dot
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 8.dp, end = 8.dp)
                        .background(chatColors.primary, CircleShape)
                        .border(2.dp, chatColors.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp), tint = Color.White)
                }
            }

            Spacer(Modifier.height(56.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = chatColors.secondaryBackground.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, chatColors.primary.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    MetaTextField(
                        value = username, 
                        onValueChange = { if (!it.contains(".")) username = it }, 
                        placeholder = stringResource(R.string.setup_hint_username), 
                        icon = Icons.Rounded.AlternateEmail
                    )
                    Spacer(Modifier.width(20.dp))
                    MetaTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = stringResource(R.string.setup_hint_full_name),
                        icon = Icons.Rounded.Badge
                    )
                }
            }
            
            Spacer(Modifier.height(56.dp))
            
            if (loading) {
                CircularProgressIndicator(color = chatColors.primary, modifier = Modifier.size(32.dp))
            } else {
                Button(
                    onClick = {
                        if (username.length < 3) {
                            Toast.makeText(context, context.getString(R.string.setup_error_username_short), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        loading = true
                        viewModel.finalizeProfile(username, name.ifBlank { username }, selectedImageUri) { success, error ->
                            loading = false
                            if (!success) {
                                Toast.makeText(context, error ?: context.getString(R.string.setup_error_save), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(62.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = chatColors.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text(stringResource(R.string.setup_button_finish), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                }
            }
            
            Spacer(Modifier.height(24.dp))

            TextButton(
                onClick = { viewModel.logout() }
            ) {
                Text(
                    stringResource(R.string.login_switch_to_login_action),
                    color = chatColors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}
