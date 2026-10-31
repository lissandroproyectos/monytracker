package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = NavySurface,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = AccentAmber,
    onSecondary = NavySurface,
    secondaryContainer = SlateCardLight,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = AccentCyan,
    background = SlateBackground,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondaryDark,
    error = AccentRed,
    onError = TextPrimaryDark,
    outline = SlateCardLight
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = CardLight,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = EmeraldDark,
    secondary = AccentAmber,
    onSecondary = CardLight,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = AccentBlue,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    error = AccentRed,
    onError = CardLight,
    outline = BorderLight
)

@Composable
fun FinanzasTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
