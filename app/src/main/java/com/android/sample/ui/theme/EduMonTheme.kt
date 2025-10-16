package com.android.sample.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/*val AccentViolet = Color(0xFF9333EA)
val AccentBlue = Color(0xFF4C7EFF)
val MidDarkCard = Color(0xFF232445)
val TextLight = Color(0xFFE0E0E0)
val BackgroundDark = Color(0xFF0F0F1A)
val Glow = Color(0xFFB48CF0)*/

private val DarkColorScheme =
    darkColorScheme(
        primary = AccentViolet,
        background = BackgroundDark,
        surface = MidDarkCard,
        onPrimary = TextLight,
        onBackground = TextLight,
        onSurface = TextLight,
        secondary = AccentBlue)

@Composable
fun EduMonTheme(content: @Composable () -> Unit) {
  MaterialTheme(
      colorScheme = DarkColorScheme,
      typography = androidx.compose.material3.Typography(),
      content = content)
}
