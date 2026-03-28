package com.jack.friend.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun WappiIcon(
    imageVector: ImageVector,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Icon(imageVector = imageVector, contentDescription = null, tint = tint, modifier = modifier)
}

@Composable
fun WappiLikeIcon(
    filled: Boolean = false,
    tint: Color,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = if (filled) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}

@Composable
fun WappiCommentIcon(
    tint: Color,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Rounded.ChatBubbleOutline,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}

@Composable
fun WappiShareIcon(
    tint: Color,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Rounded.Share,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}
