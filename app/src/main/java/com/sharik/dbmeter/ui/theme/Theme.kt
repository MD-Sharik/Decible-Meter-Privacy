package com.sharik.dbmeter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Follows the device setting by default: [isSystemInDarkTheme] reads the
 * system night mode, so there is no in-app toggle to keep in sync.
 */
@Composable
fun DbMeterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = NeedleGreen,
            background = colors.screenBackground,
            surface = colors.cardBackground,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    } else {
        lightColorScheme(
            primary = NeedleGreen,
            background = colors.screenBackground,
            surface = colors.cardBackground,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    }

    CompositionLocalProvider(LocalDbColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
