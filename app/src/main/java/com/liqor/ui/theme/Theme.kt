package com.liqor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LiqorLightColorScheme = lightColorScheme(
    primary = TextPrimary,
    onPrimary = Raised,
    secondary = Caution,
    onSecondary = TextPrimary,
    tertiary = Danger,
    onTertiary = Raised,
    error = Danger,
    onError = Raised,
    background = Ground,
    onBackground = TextPrimary,
    surface = Raised,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Hairline,
    outlineVariant = Stale,
)

@Composable
fun LiqorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LiqorLightColorScheme,
        typography = LiqorTypography,
        content = content,
    )
}
