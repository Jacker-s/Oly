package com.jack.friend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.jack.friend.ui.theme.iOSGreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.jack.friend.R

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
    val nearbyUsers by viewModel.nearbyUsers.collectAsStateWithLifecycle()
    val suggestedUsers by viewModel.suggestedUsers.collectAsStateWithLifecycle()
    val chatColors = LocalChatColors.current

    LaunchedEffect(Unit) {
        viewModel.fetchSuggestedUsers()
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(chatColors.background)
    ) {
        if (searchInput.isEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 130.dp)
            ) {
                item {
                    Text(
                        stringResource(R.string.search_nearby_people),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = chatColors.textPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    NearbyUsersRow(
                        users = nearbyUsers,
                        onUserClick = onUserClick
                    )

                    Spacer(Modifier.height(24.dp))
                }

                item {
                    Text(
                        stringResource(R.string.search_suggestions_for_you),
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
                            Text(stringResource(R.string.search_empty_hint), color = chatColors.textSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 130.dp)
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(users) { user ->
            Surface(
                modifier = Modifier
                    .width(100.dp)
                    .clickable { onUserClick(user) }
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = chatColors.secondaryBackground,
                tonalElevation = 2.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(chatColors.separator),
                            contentScale = ContentScale.Crop
                        )
                        if (user.isOnline && user.isVisibleOnline) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(Modifier.size(12.dp).clip(CircleShape).background(iOSGreen))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        user.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = chatColors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Rounded.LocationOn,
                            null,
                            tint = chatColors.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            user.status.ifBlank { stringResource(R.string.search_nearby_label) },
                            fontSize = 11.sp,
                            color = chatColors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onUserClick() },
        shape = RoundedCornerShape(16.dp),
        color = chatColors.secondaryBackground,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(chatColors.separator),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    user.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = chatColors.textPrimary
                )
                Text(
                    "@${user.id.lowercase()}",
                    fontSize = 13.sp,
                    color = chatColors.textSecondary
                )
            }
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = chatColors.primary)
            ) {
                Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_add_friend), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
