package com.jack.friend.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jack.friend.ui.theme.LocalChatColors
import com.jack.friend.ui.theme.iOSRed
import androidx.compose.ui.res.stringResource
import com.jack.friend.R

@Composable
fun ChatOptionsMenuSheet(
    isMuted: Boolean,
    isPinned: Boolean,
    tempMessageDuration: Long,
    isBlocked: Boolean,
    onDismiss: () -> Unit,
    onViewInfo: () -> Unit,
    onToggleMute: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleTempMessages: () -> Unit,
    onClearChat: () -> Unit,
    onBlockToggle: () -> Unit,
    onStarredMessages: () -> Unit = {},
    onSendTestAd: () -> Unit = {}
) {
    val colors = LocalChatColors.current
    val durationText = when {
        tempMessageDuration <= 0 -> stringResource(R.string.time_off)
        tempMessageDuration < 60_000 -> stringResource(R.string.time_seconds, tempMessageDuration / 1000)
        tempMessageDuration < 3_600_000 -> stringResource(R.string.time_minutes, tempMessageDuration / 60_000)
        else -> stringResource(R.string.time_hours, tempMessageDuration / 3_600_000)
    }

    Column(
        modifier = Modifier
            .background(colors.secondaryBackground)
            .navigationBarsPadding()
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(colors.separator)
            )
        }

        SheetOption(
            icon = Icons.Rounded.AccountCircle,
            text = stringResource(R.string.menu_view_profile),
            onClick = { onDismiss(); onViewInfo() }
        )
        SheetOption(
            icon = Icons.Rounded.PushPin,
            text = if (isPinned) stringResource(R.string.action_unpin) else stringResource(R.string.action_pin),
            onClick = { onDismiss(); onTogglePin() }
        )
        SheetOption(
            icon = if (isMuted) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
            text = if (isMuted) stringResource(R.string.action_unmute) else stringResource(R.string.action_mute),
            onClick = { onDismiss(); onToggleMute() }
        )
        SheetOption(
            icon = Icons.Rounded.Timer,
            text = stringResource(R.string.action_temp_messages),
            trailingText = durationText,
            onClick = { onDismiss(); onToggleTempMessages() }
        )
        SheetOption(
            icon = Icons.Rounded.Star,
            text = stringResource(R.string.action_starred_messages),
            onClick = { onDismiss(); onStarredMessages() }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.separator.copy(alpha = 0.5f))

        SheetOption(
            icon = Icons.Rounded.DeleteSweep,
            text = stringResource(R.string.action_clear_chat),
            onClick = { onDismiss(); onClearChat() }
        )
        SheetOption(
            icon = if (isBlocked) Icons.Rounded.LockOpen else Icons.Rounded.Block,
            text = if (isBlocked) stringResource(R.string.action_unblock) else stringResource(R.string.action_block),
            contentColor = iOSRed,
            onClick = { onDismiss(); onBlockToggle() }
        )
    }
}

@Composable
private fun SheetOption(
    icon: ImageVector,
    text: String,
    trailingText: String? = null,
    contentColor: Color = LocalChatColors.current.textPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                color = LocalChatColors.current.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
