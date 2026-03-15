package com.jack.friend

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jack.friend.ui.theme.FriendTheme
import com.jack.friend.ui.theme.MessengerBlue
import kotlinx.coroutines.delay

class SplashScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)

        if (!isFirstRun) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            FriendTheme {
                SplashScreenContent {
                    prefs.edit().putBoolean("is_first_run", false).apply()
                    startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashScreenContent(onFinish: () -> Unit) {
    val letters = listOf("W", "A", "P", "P", "I", ".", ".", ".")
    val animatables = letters.map { remember { Animatable(0f) } }
    var allVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Animação letra por letra
        animatables.forEachIndexed { index, animatable ->
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
            )
            // Delay menor entre as letras do nome, maior para os pontos
            if (index < 4) {
                delay(80)
            } else {
                delay(250)
            }
        }
        
        delay(1000) // Espera um pouco com tudo visível
        
        // Desaparece tudo de uma vez
        allVisible = false
        delay(500)
        
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (allVisible) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                letters.forEachIndexed { index, letter ->
                    Text(
                        text = letter,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        color = MessengerBlue,
                        modifier = Modifier
                            .alpha(animatables[index].value)
                            .padding(horizontal = if (letter == ".") 2.dp else 1.dp),
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            }
        }
    }
}
