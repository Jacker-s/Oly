package com.jack.friend.ui.chat

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import coil.compose.SubcomposeAsyncImage
import com.jack.friend.R
import androidx.compose.ui.res.stringResource
import com.jack.friend.ui.theme.LocalChatColors
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun MessageVideoItem(
    videoUrl: String,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isUploading: Boolean = false
) {
    val context = LocalContext.current
    var videoDuration by remember { mutableLongStateOf(0L) }
    var isVideoPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(videoUrl) {
        if (!isUploading) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(videoUrl, HashMap<String, String>())
                val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                videoDuration = time?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 320.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LocalChatColors.current.separator.copy(alpha = 0.1f))
            // Removed outer clickable to allow ModernVideoPlayer gestures to work
    ) {
        if (!isUploading) {
            com.jack.friend.ui.components.ModernVideoPlayer(
                videoUrl = videoUrl,
                modifier = Modifier.fillMaxSize(),
                autoPlay = false,
                loop = true,
                showControls = false, // Simplified controls for chat bubble
                onSingleTap = { 
                    isVideoPlaying = !isVideoPlaying
                },
                onDoubleTap = { onVideoClick(videoUrl) } // Open full screen on double tap
            )
            
            // Premium Play Button Overlay (if not playing yet)
            AnimatedVisibility(
                visible = !isVideoPlaying,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = CircleShape,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp).padding(start = 4.dp)
                    )
                }
            }
        } else {
             Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(30.dp))
            }
        }

        // Duration Label (Premium Style)
        if (videoDuration > 0 && !isUploading) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Text(
                    text = formatDuration(videoDuration),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

private fun formatDuration(durationMillis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
