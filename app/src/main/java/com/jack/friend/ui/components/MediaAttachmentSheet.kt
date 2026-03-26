package com.jack.friend.ui.components

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jack.friend.ChatViewModel
import com.jack.friend.LocalMedia
import com.jack.friend.R
import androidx.compose.ui.res.stringResource
import com.jack.friend.ui.theme.LocalChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaAttachmentSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenFile: () -> Unit,
    onShareLocation: () -> Unit,
    isStatus: Boolean = false,
    onMediaSelected: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val localMedia by viewModel.localMedia.collectAsState()
    val selectedUris = remember { mutableStateListOf<Uri>() }
    val colors = LocalChatColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.fetchLocalMedia(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.secondaryBackground.copy(alpha = 0.98f),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 12.dp).width(36.dp).height(4.dp),
                color = colors.textSecondary.copy(alpha = 0.2f),
                shape = CircleShape
            ) {}
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isStatus) stringResource(R.string.attachment_status_title) else stringResource(R.string.attachment_media_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                
                if (selectedUris.isNotEmpty()) {
                    Button(
                        onClick = {
                            onMediaSelected(selectedUris.toList())
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.attachment_action_send_count, selectedUris.size), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Recent Media Horizontal List
            if (localMedia.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.height(160.dp)
                ) {
                    items(localMedia.take(15)) { media ->
                        val isSelected = selectedUris.contains(media.uri)
                        
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.separator.copy(0.3f))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) colors.primary else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    if (isSelected) selectedUris.remove(media.uri)
                                    else selectedUris.add(media.uri)
                                }
                        ) {
                            AsyncImage(
                                model = media.uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Glass Overlay for selection
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(colors.primary.copy(alpha = 0.25f))
                                )
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(36.dp)
                                )
                            }

                            if (media.isVideo) {
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                                    color = Color.Black.copy(0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                        Text(stringResource(R.string.attachment_label_video), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            // High-Tech Action Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PremiumActionCard(
                        icon = Icons.Rounded.PhotoCamera,
                        label = stringResource(R.string.attachment_action_camera),
                        gradient = Brush.linearGradient(listOf(Color(0xFF6200EE), Color(0xFF3700B3))),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenCamera(); onDismiss() }
                    )
                    PremiumActionCard(
                        icon = Icons.Rounded.Image,
                        label = stringResource(R.string.attachment_action_gallery),
                        gradient = Brush.linearGradient(listOf(Color(0xFF03DAC6), Color(0xFF018786))),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenGallery(); onDismiss() }
                    )
                }
                if (!isStatus) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PremiumActionCard(
                            icon = Icons.Rounded.AttachFile,
                            label = stringResource(R.string.attachment_action_document),
                            gradient = Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFF57C00))),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onOpenFile() }
                        )
                        PremiumActionCard(
                            icon = Icons.Rounded.LocationOn,
                            label = stringResource(R.string.attachment_action_location),
                            gradient = Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF388E3C))),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onShareLocation() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumActionCard(
    icon: ImageVector,
    label: String,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalChatColors.current
    
    Surface(
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = colors.tertiaryBackground.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, colors.separator.copy(0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }
    }
}
