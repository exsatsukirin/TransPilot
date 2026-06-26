package com.exsatsukirin.transpilot.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightBlueColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = BlueGrey40,
    onSecondary = Color.White,
    secondaryContainer = BlueGrey90,
    onSecondaryContainer = BlueGrey10,
    tertiary = Color(0xFF6F5A00),
    background = Color(0xFFFDFBFF),
    surface = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1C2E),
    onSurface = Color(0xFF1A1C2E),
    surfaceVariant = Color(0xFFE0E2EC),
    outline = Color(0xFF74768B),
)

private val DarkBlueColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    secondary = BlueGrey80,
    onSecondary = BlueGrey20,
    secondaryContainer = BlueGrey30,
    onSecondaryContainer = BlueGrey90,
    tertiary = Color(0xFFDBBD4C),
    background = Color(0xFF1A1C2E),
    surface = Color(0xFF1A1C2E),
    onBackground = Color(0xFFE3E2F9),
    onSurface = Color(0xFFE3E2F9),
    surfaceVariant = Color(0xFF46475B),
    outline = Color(0xFF9090A6),
)

@Composable
fun TransPilotTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkBlueColorScheme
        else -> LightBlueColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
