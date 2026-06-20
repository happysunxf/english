package com.morningenglish.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFFFFB4A2),       // 暖橙色 — 清晨阳光
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFFFDAD0),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFFE57373),     // Peppa 粉
    onSecondary = Color.White,
    background = Color(0xFFFFFBF8),
    surface = Color(0xFFFFFBF8),
    onSurface = Color(0xFF201A19)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A2),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF93000A),
    secondary = Color(0xFFE57373),
    onSecondary = Color.White,
    background = Color(0xFF201A19),
    surface = Color(0xFF201A19),
    onSurface = Color(0xFFEDE0DE)
)

@Composable
fun MorningEnglishTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}