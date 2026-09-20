package com.jcompute.mermaid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jcompute.mermaid.data.Appearance

val PaperLight = Color(0xFFFAFAFA)
val PaperDark = Color(0xFF121212)

private val Teal = Color(0xFF0F766E)
private val TealLight = Color(0xFF2DD4BF)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF3F3F46),
    background = PaperLight,
    surface = PaperLight,
    surfaceContainerLowest = Color.White,
    surfaceContainer = Color(0xFFEEEEF0),
    onBackground = Color(0xFF18181B),
    onSurface = Color(0xFF18181B),
    outline = Color(0xFFD4D4D8),
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFD4D4D8),
    background = PaperDark,
    surface = PaperDark,
    surfaceContainerLowest = Color(0xFF18181B),
    surfaceContainer = Color(0xFF27272A),
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5),
    outline = Color(0xFF3F3F46),
)

@Composable
fun MermaidTheme(
    appearance: Appearance,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (appearance) {
        Appearance.System -> systemDark
        Appearance.Light -> false
        Appearance.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

fun Appearance.resolvesDark(systemDark: Boolean): Boolean {
    return when (this) {
        Appearance.System -> systemDark
        Appearance.Light -> false
        Appearance.Dark -> true
    }
}
