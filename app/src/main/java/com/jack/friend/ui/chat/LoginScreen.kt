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
import com.jack.friend.ui.theme.MetaGray4

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

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> selectedImageUri = uri }
    val chatColors = LocalChatColors.current

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
                if (isSignUp) "Crie sua conta e comece a conversar" else "Bem-vindo de volta! Sentimos sua falta",
                style = MaterialTheme.typography.bodyMedium,
                color = chatColors.textSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

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
                                            Text("Foto", fontSize = 11.sp, color = chatColors.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                MetaTextField(
                                    value = username,
                                    onValueChange = { if (!it.contains(".")) username = it },
                                    placeholder = "Nome de usuário",
                                    icon = Icons.Rounded.AlternateEmail
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    MetaTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "E-mail",
                        icon = Icons.Rounded.Email,
                        keyboardType = KeyboardType.Email
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    MetaTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Senha",
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
                                Text("Lembrar", fontSize = 14.sp, color = chatColors.textSecondary, fontWeight = FontWeight.Medium)
                            }
                            TextButton(onClick = { showResetPassword = true }) {
                                Text("Esqueceu?", color = chatColors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                if (isSignUp) viewModel.signUp(email, password, username, selectedImageUri) { s, e -> loading = false; if (!s) Toast.makeText(context, e ?: "Erro", Toast.LENGTH_SHORT).show() }
                                else viewModel.login(email, password) { s, e ->
                                    loading = false
                                    if (!s) {
                                        Toast.makeText(context, e ?: "Erro", Toast.LENGTH_SHORT).show()
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
                                if (isSignUp) "Criar Conta" else "Entrar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Social Login Divider
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = chatColors.textSecondary.copy(alpha = 0.1f))
                Text("ou continue com", modifier = Modifier.padding(horizontal = 16.dp), color = chatColors.textSecondary.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                HorizontalDivider(modifier = Modifier.weight(1f), color = chatColors.textSecondary.copy(alpha = 0.1f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Social Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SocialLoginButton(
                    icon = Icons.Rounded.GTranslate, // Placeholder for Google
                    text = "Google",
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO */ }
                )
                SocialLoginButton(
                    icon = Icons.Rounded.AccountCircle, // Placeholder for Apple (Icons.Rounded.Apple does not exist)
                    text = "Apple",
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            TextButton(
                onClick = { isSignUp = !isSignUp },
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isSignUp) "Já tem uma conta?" else "Não tem uma conta?",
                        color = chatColors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isSignUp) "Entrar" else "Cadastre-se",
                        color = chatColors.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showResetPassword) {
        var resetEmail by remember { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = { showResetPassword = false },
            title = { Text("Recuperar Senha", fontWeight = FontWeight.Bold, color = chatColors.textPrimary) },
            text = {
                Column {
                    Text("Enviaremos um link de redefinição para o seu e-mail.", color = chatColors.textSecondary)
                    Spacer(Modifier.height(20.dp))
                    MetaTextField(resetEmail, { resetEmail = it }, "E-mail de cadastro", Icons.Rounded.Email)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) viewModel.resetPassword(resetEmail) { s, e ->
                            if (s) { Toast.makeText(context, "Link enviado!", Toast.LENGTH_LONG).show(); showResetPassword = false }
                            else Toast.makeText(context, e ?: "Erro", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = chatColors.primary)
                ) {
                    Text("Enviar Link", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPassword = false }) {
                    Text("Cancelar", color = chatColors.textSecondary)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = chatColors.secondaryBackground
        )
    }
}

@Composable
fun SocialLoginButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(20.dp),
        color = chatColors.secondaryBackground.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, chatColors.textSecondary.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = chatColors.textPrimary)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = chatColors.textPrimary)
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
            Spacer(Modifier.height(80.dp))
            
            Text(
                "Quase lá!",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = chatColors.textPrimary
            )
            
            Text(
                "Personalize seu perfil para que seus amigos possam te encontrar",
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
                            "Foto de Perfil",
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
                        placeholder = "ID de usuário (ex: jack)", 
                        icon = Icons.Rounded.AlternateEmail
                    )
                    Spacer(Modifier.height(20.dp))
                    MetaTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Seu nome completo",
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
                            Toast.makeText(context, "Username muito curto", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        loading = true
                        viewModel.finalizeProfile(username, name.ifBlank { username }, selectedImageUri) { success, error ->
                            loading = false
                            if (!success) {
                                Toast.makeText(context, error ?: "Erro ao salvar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(62.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = chatColors.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text("Concluir Cadastro", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}
