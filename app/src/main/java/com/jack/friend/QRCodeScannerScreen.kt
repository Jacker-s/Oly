package com.jack.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jack.friend.ui.theme.LocalChatColors

@Composable
fun QRCodeScannerScreen(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    val colors = LocalChatColors.current
    var scannedId by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = colors.textPrimary)
                }
            }
            Icon(
                Icons.Rounded.QrCodeScanner,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier
                    .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Escanear QR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Se o scanner não estiver disponível, informe o ID manualmente.", color = colors.textSecondary)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = scannedId,
                onValueChange = { scannedId = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ID do usuário") },
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (scannedId.isNotBlank()) onResult(scannedId.trim()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar")
            }
        }
    }
}
