package com.jack.friend.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jack.friend.R
import androidx.compose.ui.res.stringResource
import com.jack.friend.ui.theme.LocalChatColors

/**
 * Premium location sharing dialog allowing the user to choose between
 * sharing a one-time static location or starting a live tracking session.
 */
@Composable
fun LocationShareDialog(
    isSharingLive: Boolean,
    onDismiss: () -> Unit,
    onShareStatic: () -> Unit,
    onStartLive: (durationMinutes: Int) -> Unit,
    onStopLive: () -> Unit
) {
    val chatColors = LocalChatColors.current
    var selectedDuration by remember { mutableIntStateOf(15) }
    val durations = listOf(5, 15, 30, 60)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(chatColors.secondaryBackground)
                    .clickable(enabled = false) {}
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(chatColors.separator.copy(0.3f))
                        .align(Alignment.CenterHorizontally)
                )

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4CAF50), Color(0xFF1B5E20))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.location_dialog_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = chatColors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.location_dialog_subtitle),
                            fontSize = 13.sp,
                            color = chatColors.textSecondary
                        )
                    }
                }

                HorizontalDivider(color = chatColors.separator.copy(0.3f), modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(20.dp))

                // --- LIVE LOCATION CARD ---
                if (isSharingLive) {
                    // Active live location indicator
                    LiveLocationActiveCard(
                        onStop = {
                            onStopLive()
                            onDismiss()
                        },
                        chatColors = chatColors
                    )
                } else {
                    // Static location option
                    LocationOptionCard(
                        icon = Icons.Rounded.PinDrop,
                        gradient = Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF0D47A1))),
                        title = stringResource(R.string.location_static_title),
                        subtitle = stringResource(R.string.location_static_subtitle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        onClick = {
                            onShareStatic()
                            onDismiss()
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Live location option
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(chatColors.tertiaryBackground.copy(0.5f))
                            .border(1.dp, chatColors.separator.copy(0.2f), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF4CAF50), Color(0xFF1B5E20))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.MyLocation,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column {
                                Text(
                                    stringResource(R.string.location_live_title),
                                    fontWeight = FontWeight.Bold,
                                    color = chatColors.textPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    stringResource(R.string.location_live_subtitle),
                                    color = chatColors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Duration selector
                        Text(
                            stringResource(R.string.location_duration_label),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = chatColors.textSecondary
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            durations.forEach { min ->
                                val isSelected = selectedDuration == min
                                Surface(
                                    modifier = Modifier.clickable { selectedDuration = min },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF4CAF50) else chatColors.separator.copy(0.2f),
                                    border = if (isSelected) null else
                                        androidx.compose.foundation.BorderStroke(1.dp, chatColors.separator.copy(0.3f))
                                ) {
                                    Text(
                                        text = if (min < 60) stringResource(R.string.time_minutes_short, min) else stringResource(R.string.time_hours_short, 1),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        color = if (isSelected) Color.White else chatColors.textSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onStartLive(selectedDuration)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.NearMe, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            val durationText = if (selectedDuration < 60) stringResource(R.string.time_minutes_short, selectedDuration) else stringResource(R.string.time_hours_short, 1)
                            Text(
                                stringResource(R.string.location_action_share_duration, durationText),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.action_cancel), color = chatColors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun LiveLocationActiveCard(
    onStop: () -> Unit,
    chatColors: com.jack.friend.ui.theme.ChatColors
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
            .border(1.dp, Color(0xFF4CAF50).copy(0.3f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
            )
            Icon(
                Icons.Rounded.MyLocation,
                null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.location_live_active_title),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
                fontSize = 15.sp
            )
            Text(
                stringResource(R.string.location_live_active_subtitle),
                color = chatColors.textSecondary,
                fontSize = 12.sp
            )
        }

        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.action_stop), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LocationOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val chatColors = LocalChatColors.current

    Surface(
        modifier = modifier
            .height(76.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = chatColors.tertiaryBackground.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, chatColors.separator.copy(0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = chatColors.textPrimary,
                    fontSize = 15.sp
                )
                Text(
                    subtitle,
                    color = chatColors.textSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.weight(1f))

            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = chatColors.textSecondary.copy(0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


