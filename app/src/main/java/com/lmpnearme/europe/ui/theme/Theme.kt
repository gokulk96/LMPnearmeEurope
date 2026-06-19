package com.lmpnearme.europe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LmpColorScheme = darkColorScheme(
    primary = SkyBlueLight,
    secondary = SkyBluePale,
    background = SkyBlueDeep,
    surface = CardBackground,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun LmpNearMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LmpColorScheme,
        typography = AppTypography,
        content = content
    )
}
