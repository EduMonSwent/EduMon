package com.android.sample.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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
