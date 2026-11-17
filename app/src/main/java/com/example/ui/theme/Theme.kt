package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LuminousCyan,
    onPrimary = MidnightNavy,
    primaryContainer = MidnightSurfaceHigh,
    onPrimaryContainer = SoftIceText,
    secondary = WarmAmber,
    onSecondary = MidnightNavy,
    secondaryContainer = MidnightSurfaceHigh,
    onSecondaryContainer = WarmAmber,
    background = MidnightNavy,
    onBackground = SoftIceText,
    surface = MidnightSurface,
    onSurface = SoftIceText,
    surfaceVariant = MidnightSurfaceHigh,
    onSurfaceVariant = MutedSlateText,
)

private val LightColorScheme = lightColorScheme(
    primary = DeepIndigoPrimary,
    onPrimary = LightCardSurface,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = DeepIndigoPrimary,
    secondary = TealAccent,
    onSecondary = LightCardSurface,
    background = LightCloudBg,
    onBackground = DarkInkText,
    surface = LightCardSurface,
    onSurface = DarkInkText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = ColorTokens.MutedLight,
)

private object ColorTokens {
    val MutedLight = androidx.compose.ui.graphics.Color(0xFF4B5563)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
