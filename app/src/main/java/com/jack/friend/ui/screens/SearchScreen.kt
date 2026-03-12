package com.jack.friend.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jack.friend.ChatViewModel
import com.jack.friend.UserProfile
import com.jack.friend.ui.chat.MetaUserItem
import com.jack.friend.ui.theme.LocalChatColors
import com.jack.friend.ui.theme.MessengerBlue
import com.jack.friend.ui.theme.iOSGreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SearchScreen(
    viewModel: ChatViewModel,
    searchInput: String,
    onUserClick: (UserProfile) -> Unit,
    onChatClick: (UserProfile) -> Unit,
    onAddContact: (String) -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val chatColors = LocalChatColors.current

    // Mocking suggestions and nearby for now
    val suggestedUsers = remember(searchResults, contacts) {
        searchResults.filter { res -> contacts.none { it.id == res.id } }.take(10)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(chatColors.background)
    ) {
        if (searchInput.isEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Text(
                        "Pessoas Próximas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = chatColors.textPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    NearbyUsersRow(
                        users = suggestedUsers.shuffled().take(5),
                        onUserClick = onUserClick
                    )

                    Spacer(Modifier.height(24.dp))
                }

                item {
                    Text(
                        "Sugestões para você",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = chatColors.textPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(suggestedUsers) { user ->
                    SuggestionItem(
                        user = user,
                        onUserClick = { onUserClick(user) },
                        onAddClick = { onAddContact(user.id) }
                    )
                }

                if (suggestedUsers.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Busque por @username para encontrar amigos", color = chatColors.textSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults) { user ->
                    val isContact = contacts.any { it.id == user.id }
                    MetaUserItem(
                        user = user,
                        isContact = isContact,
                        onItemClick = { onUserClick(user) },
                        onChatClick = { onChatClick(user) },
                        onAddContactClick = { onAddContact(user.id) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        thickness = 0.5.dp,
                        color = chatColors.separator.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun NearbyUsersRow(users: List<UserProfile>, onUserClick: (UserProfile) -> Unit) {
    val chatColors = LocalChatColors.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(users) { user ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(80.dp)
                    .clickable { onUserClick(user) }
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(chatColors.separator),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(iOSGreen))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    user.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = chatColors.textPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, tint = MessengerBlue, modifier = Modifier.size(10.dp))
                    Text("2km", fontSize = 10.sp, color = MessengerBlue)
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(
    user: UserProfile,
    onUserClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val chatColors = LocalChatColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(chatColors.separator),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name, fontWeight = FontWeight.Bold, color = chatColors.textPrimary)
            Text("@${user.id}", fontSize = 13.sp, color = chatColors.textSecondary)
        }
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MessengerBlue)
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Adicionar", fontSize = 13.sp)
        }
    }
}
