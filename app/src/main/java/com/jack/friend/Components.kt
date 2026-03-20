package com.jack.friend

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jack.friend.ui.theme.*

@Composable
fun MetaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MetaGray4) },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(icon, null, tint = MetaGray4, modifier = Modifier.size(20.dp)) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = LocalChatColors.current.tertiaryBackground,
            unfocusedContainerColor = LocalChatColors.current.tertiaryBackground,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MessengerBlue,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun AddContactDialog(
    icon: ImageVector,
    searchResults: List<UserProfile>,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    val chatColors = LocalChatColors.current
    
    // Garantir que comece com @ se o usuário digitar algo
    val onUsernameChange: (String) -> Unit = { input ->
        val clean = if (input.startsWith("@")) input.substring(1) else input
        username = if (input.isEmpty()) "" else "@$clean"
        onSearch(clean)
    }

    LaunchedEffect(Unit) {
        onSearch("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Encontrar Amigos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Adicione pelo @id ou encontre na lista", style = MaterialTheme.typography.labelSmall, color = MetaGray4)
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                MetaTextField(username, onUsernameChange, "@usuario", icon)
                Spacer(Modifier.height(16.dp))
                
                if (searchResults.isNotEmpty()) {
                    Text("Sugestões", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MessengerBlue)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults.take(5)) { user ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    username = "@${user.id}"
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = chatColors.secondaryBackground.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = user.photoUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(user.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("@${user.id}", fontSize = 12.sp, color = MetaGray4)
                                    }
                                    TextButton(onClick = { onAdd(user.id) }) {
                                        Text("Add", fontSize = 12.sp, color = MessengerBlue, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                } else if (username.length > 2) {
                    Text("Nenhum usuário encontrado", style = MaterialTheme.typography.bodySmall, color = MetaGray4)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (username.length > 1) onAdd(username.removePrefix("@")) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MessengerBlue)
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MetaGray4)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = chatColors.background
    )
}
