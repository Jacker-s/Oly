package com.jack.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jack.friend.ui.theme.LocalChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQRSheet(
    userId: String,
    displayName: String,
    photoUrl: String?,
    onDismiss: () -> Unit
) {
    val colors = LocalChatColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.secondaryBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.QrCode2,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier
                    .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text(userId, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            if (!photoUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(photoUrl, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    }
}
