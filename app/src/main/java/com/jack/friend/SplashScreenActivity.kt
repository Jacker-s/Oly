package com.jack.friend

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jack.friend.ui.theme.FriendTheme
import kotlinx.coroutines.delay

class SplashScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
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
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }
        }
    }
}

@Composable
fun SplashScreenContent(onFinish: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    // Animação de expansão (bounce/spring effect) para a logo
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    // Fade and slide para o título "Oly"
    val textOffset by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 50f,
        animationSpec = tween(durationMillis = 800, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "textOffset"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 300, easing = LinearEasing),
        label = "textAlpha"
    )

    // Fade in para o subtítulo "Feed e Chats"
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 0.85f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 800, easing = LinearOutSlowInEasing),
        label = "subtitleAlpha"
    )

    // Inicar sequências de ativação
    LaunchedEffect(Unit) {
        delay(100) // Pequeno atraso para o Compose construir a tela
        startAnimation = true
        delay(2600) // Espera antes de navegar para o app principal
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF5E3A), // Sunset Orange
                        Color(0xFFFF2A68)  // Vibrant Pink/Red
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo (O-ring)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale),
                contentAlignment = Alignment.Center
            ) {
                // Glow translúcido embaixo
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = size.minDimension / 2 + 10.dp.toPx()
                    )
                }
                
                // Anel Principal "O"
                Canvas(modifier = Modifier.size(80.dp)) {
                    val strokeWidth = 20.dp.toPx()
                    drawCircle(
                        color = Color.White,
                        radius = (size.minDimension / 2) - (strokeWidth / 2),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Título
            Text(
                text = "Oly",
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-1.5).sp,
                modifier = Modifier
                    .offset(y = textOffset.dp)
                    .alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo
            Text(
                text = "Feed e Chats",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(subtitleAlpha)
            )
        }
    }
}
