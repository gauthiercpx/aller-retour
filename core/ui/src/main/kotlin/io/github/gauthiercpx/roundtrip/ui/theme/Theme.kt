package io.github.gauthiercpx.roundtrip.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = StarRed,
    onPrimary = Color.White,
    secondary = IdfmBlue,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    onSurfaceVariant = Slate,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B6F),
    onPrimary = Ink,
    secondary = Color(0xFF8FB2F0),
    onSecondary = Ink,
    background = Ink,
    onBackground = Paper,
    surface = Ink,
    onSurface = Paper,
    onSurfaceVariant = Color(0xFFA9B1BC),
)

/** Static brand scheme on purpose: dynamic color would replace the STAR/IDFM identity. */
@Composable
fun RoundTripTheme(isDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) DarkColors else LightColors,
        content = content,
    )
}
