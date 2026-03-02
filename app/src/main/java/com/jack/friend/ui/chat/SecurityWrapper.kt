package com.jack.friend.ui.chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.jack.friend.ChatViewModel

@Composable
fun SecurityWrapper(isUserLoggedIn: Boolean, viewModel: ChatViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE) }
    
    var isPinEnabled by remember { mutableStateOf(prefs.getBoolean("pin_enabled", false)) }
    var isBiometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_enabled", false)) }
    var correctPin by remember { mutableStateOf(prefs.getString("security_pin", "") ?: "") }
    var isUnlocked by remember { mutableStateOf(!(isPinEnabled || isBiometricEnabled)) }

    // Refresh security settings when returning to the screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isPinEnabled = prefs.getBoolean("pin_enabled", false)
                isBiometricEnabled = prefs.getBoolean("biometric_enabled", false)
                correctPin = prefs.getString("security_pin", "") ?: ""
                
                if (!isPinEnabled && !isBiometricEnabled) {
                    isUnlocked = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            !isUserLoggedIn -> LoginScreen(viewModel)
            !isUnlocked -> PinLockScreen(correctPin, isBiometricEnabled) { isUnlocked = true }
            else -> ChatScreen(viewModel)
        }
    }
}
