package com.jack.friend

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jack.friend.ui.theme.LocalChatColors
import com.jack.friend.utils.QRCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQRSheet(
    userId: String,
    displayName: String,
    photoUrl: String?,
    onDismiss: () -> Unit
) {
    val colors = LocalChatColors.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Gera o QR Code em uma cor que combine com o tema
    val qrColor = if (isSystemInDarkTheme()) android.graphics.Color.WHITE else android.graphics.Color.BLACK

    LaunchedEffect(userId) {
        withContext(Dispatchers.IO) {
            // Codifica o ID com um prefixo para identificação fácil no scanner
            qrBitmap = QRCodeUtils.generateQRCode("wappi://user/$userId", size = 768)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 12.dp).width(36.dp).height(4.dp),
                color = colors.textSecondary.copy(alpha = 0.2f),
                shape = CircleShape
            ) {}
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = stringResource(R.string.qr_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(vertical = 20.dp)
            )

            // QR Code Card
            Surface(
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .border(1.dp, colors.separator.copy(0.1f), RoundedCornerShape(40.dp)),
                color = Color.White, // QR Codes são melhores em fundo branco puro
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "My QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        CircularProgressIndicator(color = colors.primary)
                    }

                    // Logo no centro do QR
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(3.dp, Color.White)
                    ) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // User Info
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "@${userId}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(40.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QRActionButton(
                    icon = Icons.Rounded.Share,
                    label = "Compartilhar",
                    modifier = Modifier.weight(1f),
                    onClick = { /* Implementar Share Intent se desejar */ }
                )
                QRActionButton(
                    icon = Icons.Rounded.ContentCopy,
                    label = "Copiar Link",
                    modifier = Modifier.weight(1f),
                    onClick = { /* Implementar Clipboard */ }
                )
            }
        }
    }
}

@Composable
fun QRActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalChatColors.current
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = colors.secondaryBackground,
        border = BorderStroke(1.dp, colors.separator.copy(0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 14.sp)
        }
    }
}
