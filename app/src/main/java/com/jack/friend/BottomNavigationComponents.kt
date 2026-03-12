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

    val items = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Feed,
        BottomBarScreen.Contacts,
        BottomBarScreen.Calls,
        BottomBarScreen.Settings
    )

    val selectedIndex = if (pagerOffset != null) {
        (pagerOffset + 0.5f).toInt().coerceIn(0, items.size - 1)
    } else {
        items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = chatColors.secondaryBackground,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = chatColors.separator.copy(alpha = 0.4f)
            )
            
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp) 
            ) {
                val maxWidth = maxWidth
                val itemWidth = maxWidth / items.size
                
                val animatedIndicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f),
                    label = "pill"
                )
                
                val indicatorOffset = if (pagerOffset != null) {
                    itemWidth * pagerOffset
                } else {
                    animatedIndicatorOffset
                }

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset + (itemWidth - 64.dp) / 2, y = 8.dp)
                        .width(64.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(chatColors.primary.copy(alpha = 0.12f))
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
                            targetValue = if (isSelected) chatColors.primary else chatColors.textSecondary.copy(alpha = 0.5f),
                            animationSpec = tween(300)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        onNavigate(screen)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(26.dp),
                                    tint = tint
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = tint,
                                    letterSpacing = (-0.3).sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
