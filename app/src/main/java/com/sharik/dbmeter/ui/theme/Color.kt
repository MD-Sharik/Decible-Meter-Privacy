package com.sharik.dbmeter.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app palette, held in a CompositionLocal rather than read as top-level
 * values so light and dark swap from one place.
 *
 * The loudness ramp below is deliberately outside this: those colours encode
 * how loud something is, which does not change with the device theme.
 */
@Immutable
data class DbColors(
    val screenBackground: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val gaugeTrack: Color,
    val gaugeTrackMinor: Color,
    val chartGridLine: Color,
    val chartLineStart: Color,
    val chartLineEnd: Color,
    val buttonPrimary: Color,
    val onButtonPrimary: Color,
    val buttonSecondaryBorder: Color,
    val chipBackground: Color,
    val needleHub: Color
)

val LightColors = DbColors(
    screenBackground = Color(0xFFF0F0F3),
    cardBackground = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFECECEF),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF8E8E93),
    gaugeTrack = Color(0xFFD9D9DE),
    gaugeTrackMinor = Color(0xFFC7C7CC),
    chartGridLine = Color(0xFFE5E5EA),
    chartLineStart = Color(0xFF4CD137),
    chartLineEnd = Color(0xFF6C5DD3),
    buttonPrimary = Color(0xFF1C1C1E),
    onButtonPrimary = Color(0xFFFFFFFF),
    buttonSecondaryBorder = Color(0xFFE2E2E6),
    chipBackground = Color(0xFFF0F0F3),
    needleHub = Color(0xFFFFFFFF)
)

/** Built on the launcher icon's backdrop, so app and icon read as one thing. */
val DarkColors = DbColors(
    screenBackground = Color(0xFF060A17),
    cardBackground = Color(0xFF111725),
    cardBorder = Color(0xFF1D2434),
    textPrimary = Color(0xFFF2F4F8),
    textSecondary = Color(0xFF8B92A3),
    gaugeTrack = Color(0xFF232B3C),
    gaugeTrackMinor = Color(0xFF39424F),
    chartGridLine = Color(0xFF1C2331),
    chartLineStart = Color(0xFF56DD41),
    chartLineEnd = Color(0xFF8B7BF0),
    // Inverted: a light pill with dark text is the dark-theme equivalent of
    // the near-black primary button.
    buttonPrimary = Color(0xFFF2F4F8),
    onButtonPrimary = Color(0xFF0B1120),
    buttonSecondaryBorder = Color(0xFF2A3242),
    chipBackground = Color(0xFF1A2130),
    needleHub = Color(0xFFFFFFFF)
)

val LocalDbColors = staticCompositionLocalOf { LightColors }

object DbTheme {
    val colors: DbColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDbColors.current
}

val NeedleGreen = Color(0xFF8DD426)

// Loudness ramp for the gauge stripe, quiet -> painful. Theme independent.
val LevelQuiet = Color(0xFF2ECC40)
val LevelModerate = Color(0xFFA8DB1E)
val LevelLoud = Color(0xFFFFC300)
val LevelVeryLoud = Color(0xFFFF8A00)
val LevelDanger = Color(0xFFFF3B30)
