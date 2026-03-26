package com.jack.friend.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MetaSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    isSearching: Boolean,
    onActiveChange: (Boolean) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (isSearching) {
                IconButton(onClick = {
                    onValueChange("")
                    onActiveChange(false)
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                }
            }
        },
        label = { Text("Buscar") }
    )
}
