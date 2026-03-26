package com.jack.friend.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.Send
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
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = if (filled) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun WappiCommentIcon(
    tint: Color,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.Comment,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun WappiShareIcon(
    tint: Color,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.Send,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}
