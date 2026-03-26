package com.jack.friend

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jack.friend.ui.theme.LocalChatColors

@Composable
fun FeedGalleryScreen(
    onImagesSelected: (List<Uri>) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalChatColors.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) onImagesSelected(uris)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Rounded.Close, null, tint = colors.textPrimary)
            }
            Icon(Icons.Rounded.Collections, null, tint = colors.primary)
            Text("Galeria", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text("Selecione uma ou mais imagens.", color = colors.textSecondary)
            Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text("Abrir galeria")
            }
        }
    }
}
