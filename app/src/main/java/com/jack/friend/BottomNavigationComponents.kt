package com.jack.friend

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jack.friend.ui.theme.LocalChatColors

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
) {

    object Home : BottomBarScreen(
        "home", "Chats", 
        Icons.Outlined.ChatBubbleOutline, 
        Icons.Rounded.ChatBubble
    )

    object Feed : BottomBarScreen(
        "feed", "Feed",
        Icons.Outlined.Explore,
        Icons.Rounded.Explore
    )
    object Contacts : BottomBarScreen(
        "contacts", "Amigos", 
        Icons.Outlined.Person, 
        Icons.Rounded.Person
    )
    object Search : BottomBarScreen(
        "search", "Busca", 
        Icons.Outlined.Search, 
        Icons.Rounded.Search
    )
    object Calls : BottomBarScreen(
        "calls", "Ligações", 
        Icons.Outlined.Phone, 
        Icons.Rounded.Phone
    )
    object Settings : BottomBarScreen(
        "settings", "Ajustes", 
        Icons.Outlined.Settings, 
        Icons.Rounded.Settings
    )
}

@Composable
fun ResponsiveFloatingDock(
    currentRoute: String,
    onNavigate: (BottomBarScreen) -> Unit,
    onFabClick: () -> Unit = {},
    pagerOffset: Float? = null
) {
    val chatColors = LocalChatColors.current

    // Reordered items to prioritize Feed and Chat at center
    val items = listOf(
        BottomBarScreen.Feed,
        BottomBarScreen.Home,
        BottomBarScreen.Contacts,
        BottomBarScreen.Calls,
        BottomBarScreen.Settings
    )

    val selectedIndex = if (pagerOffset != null) {
        (pagerOffset + 0.5f).toInt().coerceIn(0, items.size - 1)
    } else {
        items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }

    // Wrap in a transparent box to act as a floating dock
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            color = chatColors.secondaryBackground.copy(alpha = 0.95f),
            shape = RoundedCornerShape(34.dp),
            shadowElevation = 12.dp,
            tonalElevation = 8.dp
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val itemWidth = maxWidth / items.size
                
                val animatedIndicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "pill"
                )
                
                val indicatorOffset = if (pagerOffset != null) {
                    itemWidth * pagerOffset
                } else {
                    animatedIndicatorOffset
                }

                // Smooth glowing indicator circle behind the active icon
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset + (itemWidth - 48.dp) / 2, y = 10.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(chatColors.primary.copy(alpha = 0.15f))
                        .zIndex(0f)
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, screen ->
                        val isSelected = index == selectedIndex
                        val tint by animateColorAsState(
                            targetValue = if (isSelected) chatColors.primary else chatColors.textSecondary.copy(alpha = 0.6f),
                            animationSpec = tween(300)
                        )
                        
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.2f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onNavigate(screen) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title,
                                modifier = Modifier
                                    .size(26.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                                tint = tint
                            )
                        }
                    }
                }
            }
        }
    }
}
