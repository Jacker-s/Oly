package com.jack.friend.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

enum class AppTheme(val title: String) {
    OLY_DYNAMIC("Oly Dynamic"),
    DEFAULT("Padrão iOS"),
    WARM("Cálido"),
    TELEGRAM("Telegram"),
    OCEAN("Oceano"),
    LAVENDER("Lavanda"),
    MIDNIGHT("Midnight (OLED)"),
    FOREST("Floresta"),
    SAKURA("Sakura"),
    COFFEE("Café"),
    CYBERPUNK("Cyberpunk"),
    DRACULA("Dracula"),
    NORD("Nord"),
    AMETHYST("Ametista Lux"),
    SUNSET("Pôr do Sol"),
    MATCHA("Zen Matcha"),
    NEON("Neon Night"),
    GOTHIC("Gothic Crimson")
}

@Immutable
data class ChatColors(
    val bubbleMe: Color,
    val onBubbleMe: Color,
    val bubbleOther: Color,
    val onBubbleOther: Color,
    val background: Color,
    val topBar: Color,
    val onTopBar: Color,
    val primary: Color,
    val primaryBackground: Color,
    val secondaryBackground: Color,
    val tertiaryBackground: Color,
    val separator: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val titlePrimary: Color,
    val error: Color = Color(0xFFFF3B30),
    val success: Color = Color(0xFF34C759),
    val warning: Color = Color(0xFFFFCC00),
    val mention: Color = Color(0xFF007AFF),
    val link: Color = Color(0xFF007AFF),
    val backgroundGradient: List<Color>? = null,
    val bubbleCornerRadius: androidx.compose.ui.unit.Dp = 18.dp,
    val waveColors: List<Color>
)

val LocalChatColors = staticCompositionLocalOf {
    ChatColors(
        bubbleMe = iOSBubbleMe,
        onBubbleMe = Color.White,
        bubbleOther = iOSBubbleOtherLight,
        onBubbleOther = Color.Black,
        background = iOSBackgroundLight,
        topBar = iOSSecondaryBackgroundLight,
        onTopBar = iOSLabelPrimaryLight,
        primary = iOSAccent,
        primaryBackground = iOSBackgroundLight,
        secondaryBackground = iOSSecondaryBackgroundLight,
        tertiaryBackground = iOSTertiaryBackgroundLight,
        separator = iOSSeparatorLight,
        textPrimary = iOSLabelPrimaryLight,
        textSecondary = iOSLabelSecondaryLight,
        titlePrimary = iOSLabelPrimaryLight,
        bubbleCornerRadius = 18.dp,
        waveColors = listOf(WaveBlue, WavePurple, WaveDeepPurple)
    )
}

@Composable
fun getAppTypography(): Typography {
    val chatColors = LocalChatColors.current
    return Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Light,
            fontSize = 34.sp,
            letterSpacing = (-0.3).sp,
            color = chatColors.titlePrimary
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            letterSpacing = (-0.2).sp,
            color = chatColors.titlePrimary
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            color = chatColors.titlePrimary
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = chatColors.textPrimary
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = chatColors.textSecondary
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = chatColors.textSecondary
        )
    )
}

@Composable
fun FriendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isDarkModeOverride: Boolean? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val uiPrefs = remember { context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE) }
    
    var isDarkModePref by remember { mutableStateOf(uiPrefs.getBoolean("dark_mode", false)) }
    var followSystemPref by remember { mutableStateOf(uiPrefs.getBoolean("follow_system", true)) }
    var selectedThemeName by remember { mutableStateOf(uiPrefs.getString("app_theme", AppTheme.OLY_DYNAMIC.name) ?: AppTheme.OLY_DYNAMIC.name) }

    DisposableEffect(uiPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                "dark_mode" -> isDarkModePref = prefs.getBoolean("dark_mode", false)
                "follow_system" -> followSystemPref = prefs.getBoolean("follow_system", true)
                "app_theme" -> selectedThemeName = prefs.getString("app_theme", AppTheme.OLY_DYNAMIC.name) ?: AppTheme.OLY_DYNAMIC.name
            }
        }
        uiPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { uiPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val actualDark = when {
        isDarkModeOverride != null -> isDarkModeOverride
        followSystemPref -> darkTheme
        else -> isDarkModePref
    }

    val selectedTheme = try { AppTheme.valueOf(selectedThemeName) } catch (e: Exception) { AppTheme.OLY_DYNAMIC }
    val chatColors = getThemeColors(selectedTheme, actualDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = chatColors.topBar.toArgb()
            window.navigationBarColor = chatColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !actualDark
                isAppearanceLightNavigationBars = !actualDark
            }
        }
    }

    val colorScheme = if (actualDark) {
        darkColorScheme(
            primary = chatColors.primary,
            onPrimary = Color.White,
            primaryContainer = chatColors.bubbleMe,
            onPrimaryContainer = Color.White,
            secondary = chatColors.bubbleMe,
            tertiary = chatColors.bubbleOther,
            background = chatColors.background,
            surface = chatColors.secondaryBackground,
            onBackground = chatColors.textPrimary,
            onSurface = chatColors.textPrimary,
            outline = chatColors.separator,
            surfaceVariant = chatColors.tertiaryBackground
        )
    } else {
        lightColorScheme(
            primary = chatColors.primary,
            onPrimary = Color.White,
            primaryContainer = chatColors.bubbleMe,
            onPrimaryContainer = Color.White,
            secondary = chatColors.bubbleMe,
            tertiary = chatColors.bubbleOther,
            background = chatColors.background,
            surface = chatColors.secondaryBackground,
            onBackground = chatColors.textPrimary,
            onSurface = chatColors.textPrimary,
            outline = chatColors.separator,
            surfaceVariant = chatColors.tertiaryBackground
        )
    }

    CompositionLocalProvider(LocalChatColors provides chatColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getAppTypography(),
            shapes = AppShapes,
            content = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (chatColors.backgroundGradient != null) Color.Transparent else chatColors.background
                ) {
                    if (chatColors.backgroundGradient != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(chatColors.backgroundGradient))
                        )
                    }
                    content()
                }
            }
        )
    }
}

fun getThemeColors(theme: AppTheme, isDark: Boolean): ChatColors {
    return when (theme) {
        AppTheme.OLY_DYNAMIC -> if (isDark) {
            ChatColors(
                bubbleMe = Color(0xFFFF5E3A),
                onBubbleMe = Color.White,
                bubbleOther = Color(0xFF231A20),
                onBubbleOther = Color.White,
                background = Color(0xFF0F0B10),
                topBar = Color(0xFF181119).copy(alpha = 0.95f),
                onTopBar = Color.White,
                primary = Color(0xFFFF2A68),
                primaryBackground = Color(0xFF0F0B10),
                secondaryBackground = Color(0xFF181119),
                tertiaryBackground = Color(0xFF231A20),
                separator = Color(0xFF32252D),
                textPrimary = Color.White,
                textSecondary = Color(0xFFC4B8C0),
                titlePrimary = Color(0xFFFF5E3A),
                mention = Color(0xFFFF5E3A),
                link = Color(0xFFFF2A68),
                backgroundGradient = listOf(Color(0xFF160910), Color(0xFF070305)),
                bubbleCornerRadius = 24.dp,
                waveColors = listOf(Color(0xFFFF5E3A), Color(0xFFFF2A68), Color(0xFFFF0055))
            )
        } else {
            ChatColors(
                bubbleMe = Color(0xFFFF5E3A),
                onBubbleMe = Color.White,
                bubbleOther = Color(0xFFFFFFFF),
                onBubbleOther = Color.Black,
                background = Color(0xFFFDF8F9),
                topBar = Color(0xFFFFFFFF).copy(alpha = 0.95f),
                onTopBar = Color.Black,
                primary = Color(0xFFFF2A68),
                primaryBackground = Color(0xFFFDF8F9),
                secondaryBackground = Color(0xFFFFFFFF),
                tertiaryBackground = Color(0xFFFFF0F3),
                separator = Color(0xFFEACFD6),
                textPrimary = Color(0xFF1E1417),
                textSecondary = Color(0xFF7D6B72),
                titlePrimary = Color(0xFFFF5E3A),
                mention = Color(0xFFFF5E3A),
                link = Color(0xFFFF2A68),
                backgroundGradient = listOf(Color(0xFFFFF2F5), Color(0xFFFDF8F9)),
                bubbleCornerRadius = 24.dp,
                waveColors = listOf(Color(0xFFFF9EA2), Color(0xFFFF5E3A), Color(0xFFFF2A68))
            )
        }
        AppTheme.DEFAULT -> if (isDark) {
            ChatColors(bubbleMe = iOSBubbleMe, onBubbleMe = Color.White, bubbleOther = iOSBubbleOtherDark, onBubbleOther = Color.White, background = iOSBackgroundDark, topBar = iOSSecondaryBackgroundDark, onTopBar = iOSLabelPrimaryDark, primary = iOSBlue, primaryBackground = iOSBackgroundDark, secondaryBackground = iOSSecondaryBackgroundDark, tertiaryBackground = iOSTertiaryBackgroundDark, separator = iOSSeparatorDark, textPrimary = iOSLabelPrimaryDark, textSecondary = iOSLabelSecondaryDark, titlePrimary = Color.White, mention = iOSBlue, link = iOSBlue, bubbleCornerRadius = 18.dp, waveColors = listOf(WaveBlue, WavePurple, WaveDeepPurple))
        } else {
            ChatColors(bubbleMe = iOSBubbleMe, onBubbleMe = Color.White, bubbleOther = iOSBubbleOtherLight, onBubbleOther = Color.Black, background = iOSBackgroundLight, topBar = iOSSecondaryBackgroundLight, onTopBar = iOSLabelPrimaryLight, primary = iOSBlue, primaryBackground = iOSBackgroundLight, secondaryBackground = iOSSecondaryBackgroundLight, tertiaryBackground = iOSTertiaryBackgroundLight, separator = iOSSeparatorLight, textPrimary = iOSLabelPrimaryLight, textSecondary = iOSLabelSecondaryLight, titlePrimary = Color.Black, mention = iOSBlue, link = iOSBlue, bubbleCornerRadius = 18.dp, waveColors = listOf(WaveBlue, WavePurple, WaveDeepPurple))
        }
        AppTheme.WARM -> if (isDark) {
            ChatColors(bubbleMe = WarmPrimary, onBubbleMe = Color.White, bubbleOther = WarmSurfaceDark, onBubbleOther = Color.White, background = WarmBackgroundDark, topBar = WarmSurfaceDark, onTopBar = WarmOnBackgroundDark, primary = WarmPrimary, primaryBackground = WarmBackgroundDark, secondaryBackground = WarmSurfaceDark, tertiaryBackground = WarmBackgroundDark, separator = Color.DarkGray, textPrimary = WarmOnBackgroundDark, textSecondary = WarmTextSecondary, titlePrimary = WarmPrimary, mention = WarmPrimary, link = WarmPrimary, bubbleCornerRadius = 20.dp, waveColors = listOf(Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFFE65100)))
        } else {
            ChatColors(bubbleMe = WarmPrimary, onBubbleMe = Color.White, bubbleOther = WarmSurfaceLight, onBubbleOther = WarmOnBackgroundLight, background = WarmBackgroundLight, topBar = WarmSurfaceLight, onTopBar = WarmOnBackgroundLight, primary = WarmPrimary, primaryBackground = WarmBackgroundLight, secondaryBackground = WarmSurfaceLight, tertiaryBackground = WarmBackgroundLight, separator = Color.LightGray, textPrimary = WarmOnBackgroundLight, textSecondary = WarmTextSecondary, titlePrimary = WarmPrimary, mention = WarmPrimary, link = WarmPrimary, bubbleCornerRadius = 20.dp, waveColors = listOf(Color(0xFFFFCC80), Color(0xFFFFAB91), Color(0xFFFF8A65)))
        }
        AppTheme.TELEGRAM -> if (isDark) {
            ChatColors(bubbleMe = TelegramBlue, onBubbleMe = Color.White, bubbleOther = Color(0xFF212D3B), onBubbleOther = Color.White, background = Color(0xFF17212B), topBar = Color(0xFF242F3D), onTopBar = Color.White, primary = TelegramBlue, primaryBackground = Color(0xFF17212B), secondaryBackground = Color(0xFF242F3D), tertiaryBackground = Color(0xFF17212B), separator = Color.Black, textPrimary = Color.White, textSecondary = Color.Gray, titlePrimary = TelegramBlue, mention = TelegramBlue, link = TelegramBlue, bubbleCornerRadius = 16.dp, waveColors = listOf(Color(0xFF24A1DE), Color(0xFF1E88BE), Color(0xFF1565C0)))
        } else {
            ChatColors(bubbleMe = TelegramBlue, onBubbleMe = Color.White, bubbleOther = Color.White, onBubbleOther = Color.Black, background = Color(0xFFDEE4E8), topBar = Color.White, onTopBar = Color.Black, primary = TelegramBlue, primaryBackground = Color(0xFFDEE4E8), secondaryBackground = Color.White, tertiaryBackground = Color(0xFFDEE4E8), separator = Color.LightGray, textPrimary = Color.Black, textSecondary = Color.Gray, titlePrimary = TelegramBlue, mention = TelegramBlue, link = TelegramBlue, bubbleCornerRadius = 16.dp, waveColors = listOf(Color(0xFF54A9EB), Color(0xFF24A1DE), Color(0xFF1E88BE)))
        }
        AppTheme.OCEAN -> if (isDark) {
            ChatColors(bubbleMe = Color(0xFF006064), onBubbleMe = Color.White, bubbleOther = Color(0xFF004D40), onBubbleOther = Color.White, background = Color(0xFF002424), topBar = Color(0xFF004D40), onTopBar = Color.White, primary = Color(0xFF00BCD4), primaryBackground = Color(0xFF002424), secondaryBackground = Color(0xFF004D40), tertiaryBackground = Color(0xFF002424), separator = Color.DarkGray, textPrimary = Color.White, textSecondary = Color.Cyan, titlePrimary = Color(0xFF00838F), mention = Color(0xFF00BCD4), link = Color(0xFF00BCD4), backgroundGradient = listOf(Color(0xFF002424), Color(0xFF004D40)), bubbleCornerRadius = 22.dp, waveColors = listOf(Color(0xFF00ACC1), Color(0xFF00838F), Color(0xFF006064)))
        } else {
            ChatColors(bubbleMe = Color(0xFF00BCD4), onBubbleMe = Color.White, bubbleOther = Color(0xFFE0F7FA), onBubbleOther = Color.Black, background = Color(0xFFB2EBF2), topBar = Color(0xFF00BCD4), onTopBar = Color.White, primary = Color(0xFF00838F), primaryBackground = Color(0xFFB2EBF2), secondaryBackground = Color.White, tertiaryBackground = Color(0xFFE0F7FA), separator = Color.Cyan, textPrimary = Color.Black, textSecondary = Color(0xFF006064), titlePrimary = Color(0xFF006064), mention = Color(0xFF00838F), link = Color(0xFF00838F), backgroundGradient = listOf(Color(0xFFB2EBF2), Color(0xFFE0F7FA)), bubbleCornerRadius = 22.dp, waveColors = listOf(Color(0xFF80DEEA), Color(0xFF4DD0E1), Color(0xFF26C6DA)))
        }
        AppTheme.LAVENDER -> if (isDark) {
            ChatColors(bubbleMe = Color(0xFF7E57C2), onBubbleMe = Color.White, bubbleOther = Color(0xFF4527A0), onBubbleOther = Color.White, background = Color(0xFF311B92), topBar = Color(0xFF4527A0), onTopBar = Color.White, primary = Color(0xFF9575CD), primaryBackground = Color(0xFF311B92), secondaryBackground = Color(0xFF4527A0), tertiaryBackground = Color(0xFF311B92), separator = Color.Black, textPrimary = Color.White, textSecondary = Color(0xFFB39DDB), titlePrimary = Color(0xFF7E57C2), mention = Color(0xFFB39DDB), link = Color(0xFFB39DDB), bubbleCornerRadius = 18.dp, waveColors = listOf(Color(0xFF9575CD), Color(0xFF7E57C2), Color(0xFF673AB7)))
        } else {
            ChatColors(bubbleMe = Color(0xFF9575CD), onBubbleMe = Color.White, bubbleOther = Color(0xFFF3E5F5), onBubbleOther = Color.Black, background = Color(0xFFEDE7F6), topBar = Color.White, onTopBar = Color(0xFF4A148C), primary = Color(0xFF673AB7), primaryBackground = Color(0xFFEDE7F6), secondaryBackground = Color.White, tertiaryBackground = Color(0xFFF3E5F5), separator = Color(0xFFD1C4E9), textPrimary = Color.Black, textSecondary = Color(0xFF512DA8), titlePrimary = Color(0xFF512DA8), mention = Color(0xFF673AB7), link = Color(0xFF673AB7), bubbleCornerRadius = 18.dp, waveColors = listOf(Color(0xFFD1C4E9), Color(0xFFB39DDB), Color(0xFF9575CD)))
        }
        AppTheme.MIDNIGHT -> if (isDark) {
            ChatColors(bubbleMe = MidnightBubbleMe, onBubbleMe = Color.White, bubbleOther = MidnightBubbleOther, onBubbleOther = Color.White, background = MidnightBackground, topBar = MidnightSecondary, onTopBar = Color.White, primary = MidnightAccent, primaryBackground = MidnightBackground, secondaryBackground = MidnightSecondary, tertiaryBackground = MidnightTertiary, separator = Color.DarkGray, textPrimary = Color.White, textSecondary = Color.LightGray, titlePrimary = MidnightAccent, mention = MidnightAccent, link = MidnightAccent, bubbleCornerRadius = 12.dp, waveColors = listOf(Color(0xFF3700B3), Color(0xFF6200EE), Color(0xFFBB86FC)))
        } else {
            ChatColors(bubbleMe = Color.Black, onBubbleMe = Color.White, bubbleOther = Color(0xFFE0E0E0), onBubbleOther = Color.Black, background = Color.White, topBar = Color.White, onTopBar = Color.Black, primary = Color.Black, primaryBackground = Color.White, secondaryBackground = Color.White, tertiaryBackground = Color(0xFFF5F5F5), separator = Color.LightGray, textPrimary = Color.Black, textSecondary = Color.Gray, titlePrimary = Color.Black, mention = Color.Black, link = Color.Black, bubbleCornerRadius = 12.dp, waveColors = listOf(Color.Black, Color.DarkGray, Color.Gray))
        }
        AppTheme.FOREST -> if (isDark) {
            ChatColors(bubbleMe = ForestGreenPrimary, onBubbleMe = Color.White, bubbleOther = ForestGreenDark, onBubbleOther = Color.White, background = ForestBackgroundDark, topBar = ForestGreenDark, onTopBar = Color.White, primary = ForestAccent, primaryBackground = ForestBackgroundDark, secondaryBackground = ForestGreenDark, tertiaryBackground = ForestBackgroundDark, separator = Color(0xFF1B3017), textPrimary = Color.White, textSecondary = Color(0xFFAED581), titlePrimary = ForestAccent, mention = ForestAccent, link = ForestAccent, bubbleCornerRadius = 20.dp, waveColors = listOf(Color(0xFF2D5A27), Color(0xFF388E3C), Color(0xFF4CAF50)))
        } else {
            ChatColors(bubbleMe = ForestGreenPrimary, onBubbleMe = Color.White, bubbleOther = ForestGreenLight, onBubbleOther = Color.Black, background = ForestBackgroundLight, topBar = ForestGreenPrimary, onTopBar = Color.White, primary = ForestGreenPrimary, primaryBackground = ForestBackgroundLight, secondaryBackground = Color.White, tertiaryBackground = ForestGreenLight, separator = Color(0xFFC8E6C9), textPrimary = Color.Black, textSecondary = Color(0xFF2E7D32), titlePrimary = Color(0xFF1B5E20), mention = ForestGreenPrimary, link = ForestGreenPrimary, bubbleCornerRadius = 20.dp, waveColors = listOf(Color(0xFF81C784), Color(0xFF66BB6A), Color(0xFF4CAF50)))
        }
        AppTheme.SAKURA -> if (isDark) {
            ChatColors(bubbleMe = SakuraPinkPrimary, onBubbleMe = Color.White, bubbleOther = SakuraBackgroundDark, onBubbleOther = Color.White, background = SakuraBackgroundDark, topBar = SakuraBackgroundDark, onTopBar = Color.White, primary = SakuraAccent, primaryBackground = SakuraBackgroundDark, secondaryBackground = SakuraBackgroundDark, tertiaryBackground = Color(0xFF3D2B2E), separator = Color(0xFF4D3B3E), textPrimary = Color.White, textSecondary = Color(0xFFF48FB1), titlePrimary = SakuraAccent, mention = SakuraAccent, link = SakuraAccent, bubbleCornerRadius = 22.dp, waveColors = listOf(Color(0xFFF06292), Color(0xFFEC407A), Color(0xFFE91E63)))
        } else {
            ChatColors(bubbleMe = SakuraPinkPrimary, onBubbleMe = Color.White, bubbleOther = SakuraPinkLight, onBubbleOther = Color.Black, background = SakuraBackgroundLight, topBar = SakuraPinkPrimary, onTopBar = Color.White, primary = SakuraPinkPrimary, primaryBackground = SakuraBackgroundLight, secondaryBackground = Color.White, tertiaryBackground = SakuraPinkLight, separator = Color(0xFFF8BBD0), textPrimary = Color.Black, textSecondary = Color(0xFFC2185B), titlePrimary = Color(0xFFAD1457), mention = SakuraPinkPrimary, link = SakuraPinkPrimary, bubbleCornerRadius = 22.dp, waveColors = listOf(Color(0xFFF48FB1), Color(0xFFF06292), Color(0xFFEC407A)))
        }
        AppTheme.COFFEE -> if (isDark) {
            ChatColors(bubbleMe = CoffeePrimary, onBubbleMe = Color.White, bubbleOther = CoffeeSurfaceDark, onBubbleOther = Color.White, background = CoffeeBackgroundDark, topBar = CoffeeSurfaceDark, onTopBar = Color.White, primary = CoffeeSecondary, primaryBackground = CoffeeBackgroundDark, secondaryBackground = CoffeeSurfaceDark, tertiaryBackground = CoffeeBackgroundDark, separator = Color(0xFF3E2C1C), textPrimary = Color.White, textSecondary = Color(0xFFA67B5B), titlePrimary = CoffeeSecondary, mention = CoffeeSecondary, link = CoffeeSecondary, bubbleCornerRadius = 14.dp, waveColors = listOf(Color(0xFF6F4E37), Color(0xFF5D4037), Color(0xFF4E342E)))
        } else {
            ChatColors(bubbleMe = CoffeePrimary, onBubbleMe = Color.White, bubbleOther = CoffeeSurfaceLight, onBubbleOther = Color.Black, background = CoffeeBackgroundLight, topBar = CoffeePrimary, onTopBar = Color.White, primary = CoffeePrimary, primaryBackground = CoffeeBackgroundLight, secondaryBackground = Color.White, tertiaryBackground = CoffeeSurfaceLight, separator = Color(0xFFD7CCC8), textPrimary = Color.Black, textSecondary = Color(0xFF5D4037), titlePrimary = Color(0xFF3E2C1C), mention = CoffeePrimary, link = CoffeePrimary, bubbleCornerRadius = 14.dp, waveColors = listOf(Color(0xFFA67B5B), Color(0xFF8D6E63), Color(0xFF795548)))
        }
        AppTheme.CYBERPUNK -> if (isDark) {
            ChatColors(bubbleMe = CyberpunkPink, onBubbleMe = Color.Black, bubbleOther = CyberpunkBlue, onBubbleOther = Color.White, background = CyberpunkBlack, topBar = CyberpunkDarkBlue, onTopBar = CyberpunkCyan, primary = CyberpunkCyan, primaryBackground = CyberpunkBlack, secondaryBackground = CyberpunkDarkBlue, tertiaryBackground = CyberpunkBlack, separator = CyberpunkPink.copy(alpha = 0.3f), textPrimary = Color.White, textSecondary = CyberpunkCyan, titlePrimary = CyberpunkPink, mention = CyberpunkYellow, link = CyberpunkCyan, bubbleCornerRadius = 4.dp, waveColors = listOf(CyberpunkPink, CyberpunkCyan, CyberpunkYellow))
        } else {
            ChatColors(bubbleMe = CyberpunkPink, onBubbleMe = Color.White, bubbleOther = Color(0xFFF2F2F2), onBubbleOther = Color.Black, background = Color.White, topBar = Color.White, onTopBar = CyberpunkBlack, primary = CyberpunkPink, primaryBackground = Color.White, secondaryBackground = Color.White, tertiaryBackground = Color(0xFFF5F5F5), separator = Color.LightGray, textPrimary = CyberpunkBlack, textSecondary = Color.Gray, titlePrimary = CyberpunkPink, mention = CyberpunkPink, link = CyberpunkPink, bubbleCornerRadius = 4.dp, waveColors = listOf(CyberpunkPink, CyberpunkCyan, CyberpunkYellow))
        }
        AppTheme.DRACULA -> if (isDark) {
            ChatColors(bubbleMe = DraculaPurple, onBubbleMe = Color.White, bubbleOther = DraculaCurrentLine, onBubbleOther = Color.White, background = DraculaBackground, topBar = DraculaCurrentLine, onTopBar = DraculaForeground, primary = DraculaPurple, primaryBackground = DraculaBackground, secondaryBackground = DraculaCurrentLine, tertiaryBackground = DraculaBackground, separator = Color.Black, textPrimary = DraculaForeground, textSecondary = DraculaComment, titlePrimary = DraculaPink, mention = DraculaCyan, link = DraculaCyan, bubbleCornerRadius = 14.dp, waveColors = listOf(DraculaPurple, DraculaPink, DraculaCyan))
        } else {
            ChatColors(bubbleMe = DraculaPurple, onBubbleMe = Color.White, bubbleOther = Color(0xFFF2F2F2), onBubbleOther = Color.Black, background = Color.White, topBar = Color.White, onTopBar = Color.Black, primary = DraculaPurple, primaryBackground = Color.White, secondaryBackground = Color.White, tertiaryBackground = Color(0xFFF5F5F5), separator = Color.LightGray, textPrimary = Color.Black, textSecondary = Color.Gray, titlePrimary = DraculaPurple, mention = DraculaPurple, link = DraculaPurple, bubbleCornerRadius = 14.dp, waveColors = listOf(DraculaPurple, DraculaPink, DraculaCyan))
        }
        AppTheme.NORD -> if (isDark) {
            ChatColors(bubbleMe = Nord10, onBubbleMe = Color.White, bubbleOther = Nord2, onBubbleOther = Color.White, background = Nord0, topBar = Nord1, onTopBar = Nord4, primary = Nord8, primaryBackground = Nord0, secondaryBackground = Nord1, tertiaryBackground = Nord0, separator = Nord3, textPrimary = Nord6, textSecondary = Nord4, titlePrimary = Nord9, mention = Nord8, link = Nord8, bubbleCornerRadius = 16.dp, waveColors = listOf(Nord7, Nord8, Nord9))
        } else {
            ChatColors(bubbleMe = Nord10, onBubbleMe = Color.White, bubbleOther = Nord5, onBubbleOther = Color.Black, background = Nord6, topBar = Color.White, onTopBar = Nord0, primary = Nord10, primaryBackground = Nord6, secondaryBackground = Color.White, tertiaryBackground = Nord5, separator = Nord4, textPrimary = Nord0, textSecondary = Nord3, titlePrimary = Nord10, mention = Nord10, link = Nord10, bubbleCornerRadius = 16.dp, waveColors = listOf(Nord7, Nord8, Nord9))
        }
        AppTheme.AMETHYST -> if (isDark) {
            ChatColors(bubbleMe = Color(0xFF9C27B0), onBubbleMe = Color.White, bubbleOther = Color(0xFF4A148C), onBubbleOther = Color.White, background = Color(0xFF1A0033), topBar = Color(0xFF311B92), onTopBar = Color.White, primary = Color(0xFFE1BEE7), primaryBackground = Color(0xFF1A0033), secondaryBackground = Color(0xFF311B92), tertiaryBackground = Color(0xFF1A0033), separator = Color(0xFF4A148C), textPrimary = Color.White, textSecondary = Color(0xFFD1C4E9), titlePrimary = Color(0xFFBA68C8), mention = Color(0xFFE1BEE7), link = Color(0xFFE1BEE7), backgroundGradient = listOf(Color(0xFF1A0033), Color(0xFF311B92)), bubbleCornerRadius = 24.dp, waveColors = listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2), Color(0xFF4A148C)))
        } else {
            ChatColors(bubbleMe = Color(0xFF9C27B0), onBubbleMe = Color.White, bubbleOther = Color(0xFFF3E5F5), onBubbleOther = Color.Black, background = Color(0xFFF5EEFD), topBar = Color.White, onTopBar = Color(0xFF4A148C), primary = Color(0xFF7B1FA2), primaryBackground = Color(0xFFF5EEFD), secondaryBackground = Color.White, tertiaryBackground = Color(0xFFF3E5F5), separator = Color(0xFFE1BEE7), textPrimary = Color.Black, textSecondary = Color(0xFF4A148C), titlePrimary = Color(0xFF7B1FA2), mention = Color(0xFF9C27B0), link = Color(0xFF9C27B0), backgroundGradient = listOf(Color(0xFFF5EEFD), Color(0xFFE1BEE7)), bubbleCornerRadius = 24.dp, waveColors = listOf(Color(0xFFE1BEE7), Color(0xFFCE93D8), Color(0xFFBA68C8)))
        }
        AppTheme.SUNSET -> if (isDark) {
            ChatColors(bubbleMe = Color(0xFFD84315), onBubbleMe = Color.White, bubbleOther = Color(0xFF4E342E), onBubbleOther = Color.White, background = Color(0xFF21100C), topBar = Color(0xFF3E2723), onTopBar = Color.White, primary = Color(0xFFFF8A65), primaryBackground = Color(0xFF21100C), secondaryBackground = Color(0xFF3E2723), tertiaryBackground = Color(0xFF21100C), separator = Color(0xFF3E2723), textPrimary = Color.White, textSecondary = Color(0xFFFFCCBC), titlePrimary = Color(0xFFFF7043), mention = Color(0xFFFFAB91), link = Color(0xFFFFAB91), backgroundGradient = listOf(Color(0xFF21100C), Color(0xFFBF360C)), bubbleCornerRadius = 24.dp, waveColors = listOf(Color(0xFFFF5722), Color(0xFFFFB300), Color(0xFFD84315)))
        } else {
            ChatColors(bubbleMe = Color(0xFFFF5722), onBubbleMe = Color.White, bubbleOther = Color(0xFFFFF3E0), onBubbleOther = Color.Black, background = Color(0xFFFFF8F1), topBar = Color.White, onTopBar = Color(0xFFD84315), primary = Color(0xFFD84315), primaryBackground = Color(0xFFFFF8F1), secondaryBackground = Color.White, tertiaryBackground = Color(0xFFFFF3E0), separator = Color(0xFFFFDCC8), textPrimary = Color.Black, textSecondary = Color(0xFFBF360C), titlePrimary = Color(0xFFE64A19), mention = Color(0xFFFF5722), link = Color(0xFFFF5722), backgroundGradient = listOf(Color(0xFFFFF8F1), Color(0xFFFFE0B2)), bubbleCornerRadius = 24.dp, waveColors = listOf(Color(0xFFFFE0B2), Color(0xFFFFCC80), Color(0xFFFFB74D)))
        }
        AppTheme.MATCHA -> if (isDark) {
            ChatColors(bubbleMe = Color(0xFF558B2F), onBubbleMe = Color.White, bubbleOther = Color(0xFF33691E), onBubbleOther = Color.White, background = Color(0xFF1B2E15), topBar = Color(0xFF2E7D32), onTopBar = Color.White, primary = Color(0xFFAED581), primaryBackground = Color(0xFF1B2E15), secondaryBackground = Color(0xFF2E7D32), tertiaryBackground = Color(0xFF1B2E15), separator = Color(0xFF1B2E15), textPrimary = Color.White, textSecondary = Color(0xFFDCEDC8), titlePrimary = Color(0xFF9CCC65), mention = Color(0xFFC5E1A5), link = Color(0xFFC5E1A5), bubbleCornerRadius = 26.dp, waveColors = listOf(Color(0xFF8BC34A), Color(0xFF689F38), Color(0xFF558B2F)))
        } else {
            ChatColors(bubbleMe = Color(0xFF8BC34A), onBubbleMe = Color.White, bubbleOther = Color(0xFFF1F8E9), onBubbleOther = Color.Black, background = Color(0xFFF9FBF7), topBar = Color.White, onTopBar = Color(0xFF33691E), primary = Color(0xFF33691E), primaryBackground = Color(0xFFF9FBF7), secondaryBackground = Color.White, tertiaryBackground = Color(0xFFF1F8E9), separator = Color(0xFFDCEDC8), textPrimary = Color.Black, textSecondary = Color(0xFF33691E), titlePrimary = Color(0xFF689F38), mention = Color(0xFF8BC34A), link = Color(0xFF8BC34A), bubbleCornerRadius = 26.dp, waveColors = listOf(Color(0xFFDCEDC8), Color(0xFFC5E1A5), Color(0xFFAED581)))
        }
        AppTheme.NEON -> if (isDark) {
            ChatColors(bubbleMe = Color(0xFF00E5FF), onBubbleMe = Color.Black, bubbleOther = Color(0xFF121212), onBubbleOther = Color(0xFF00E5FF), background = Color(0xFF050505), topBar = Color.Black, onTopBar = Color(0xFF00E5FF), primary = Color(0xFF00E5FF), primaryBackground = Color(0xFF050505), secondaryBackground = Color(0xFF0A0A0A), tertiaryBackground = Color(0xFF000000), separator = Color(0xFF00E5FF).copy(alpha = 0.2f), textPrimary = Color.White, textSecondary = Color(0xFF00E5FF), titlePrimary = Color(0xFF00E5FF), mention = Color(0xFFF50057), link = Color(0xFF00E5FF), bubbleCornerRadius = 2.dp, waveColors = listOf(Color(0xFF00E5FF), Color(0xFFF50057), Color(0xFFD500F9)))
        } else {
            ChatColors(bubbleMe = Color(0xFF00B8D4), onBubbleMe = Color.White, bubbleOther = Color(0xFFEEEEEE), onBubbleOther = Color.Black, background = Color.White, topBar = Color.White, onTopBar = Color.Black, primary = Color(0xFF00B8D4), primaryBackground = Color.White, secondaryBackground = Color.White, tertiaryBackground = Color(0xFFFAFAFA), separator = Color(0xFF00B8D4).copy(alpha = 0.2f), textPrimary = Color.Black, textSecondary = Color.DarkGray, titlePrimary = Color(0xFF00B8D4), mention = Color(0xFFD81B60), link = Color(0xFF00B8D4), bubbleCornerRadius = 2.dp, waveColors = listOf(Color(0xFF00B8D4), Color(0xFFD81B60), Color(0xFF8E24AA)))
        }
        AppTheme.GOTHIC -> if (isDark) {
            ChatColors(bubbleMe = Color(0xFF7B0000), onBubbleMe = Color.White, bubbleOther = Color(0xFF1A1A1A), onBubbleOther = Color(0xFFB71C1C), background = Color(0xFF0A0A0A), topBar = Color.Black, onTopBar = Color(0xFFB71C1C), primary = Color(0xFFB71C1C), primaryBackground = Color(0xFF0A0A0A), secondaryBackground = Color(0xFF121212), tertiaryBackground = Color(0xFF000000), separator = Color(0xFF7B0000).copy(alpha = 0.3f), textPrimary = Color(0xFFE0E0E0), textSecondary = Color(0xFFB71C1C), titlePrimary = Color(0xFFB71C1C), mention = Color(0xFFD32F2F), link = Color(0xFFD32F2F), bubbleCornerRadius = 8.dp, waveColors = listOf(Color(0xFF4A0000), Color(0xFF7B0000), Color(0xFFB71C1C)))
        } else {
            ChatColors(bubbleMe = Color(0xFFB71C1C), onBubbleMe = Color.White, bubbleOther = Color(0xFFF5F5F5), onBubbleOther = Color.Black, background = Color.White, topBar = Color.White, onTopBar = Color.Black, primary = Color(0xFFB71C1C), primaryBackground = Color.White, secondaryBackground = Color.White, tertiaryBackground = Color(0xFFF5F5F5), separator = Color(0xFFB71C1C).copy(alpha = 0.1f), textPrimary = Color.Black, textSecondary = Color.DarkGray, titlePrimary = Color(0xFFB71C1C), mention = Color(0xFFB71C1C), link = Color(0xFFB71C1C), bubbleCornerRadius = 8.dp, waveColors = listOf(Color(0xFFFFEBEE), Color(0xFFFFCDD2), Color(0xFFEF9A9A)))
        }
    }
}
