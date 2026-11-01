package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WristMirrorColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = AmoledBlack,
    primaryContainer = Color(0xFF00365D),
    onPrimaryContainer = Color(0xFFCBE6FF),
    
    secondary = NeonCyan,
    onSecondary = AmoledBlack,
    secondaryContainer = Color(0xFF003830),
    onSecondaryContainer = Color(0xFFA6F6EC),
    
    tertiary = CyberPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF321959),
    onTertiaryContainer = Color(0xFFEADBFF),
    
    background = AmoledBlack,
    onBackground = TextPrimary,
    
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCardBg,
    onSurfaceVariant = TextSecondary,
    
    outline = DarkCardBorder,
    outlineVariant = Color(0xFF132032),
    
    error = StatusIncompatible,
    onError = Color.White
)

@Composable
fun WristMirrorTheme(
    darkTheme: Boolean = true, // Default to futuristic AMOLED dark
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WristMirrorColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WristMirrorTheme(darkTheme = true, content = content)
}
