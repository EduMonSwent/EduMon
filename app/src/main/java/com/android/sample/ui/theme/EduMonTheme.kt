package com.android.sample.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun EduMonTheme(
    appearance: EdumonAppearance = EdumonAppearances.Default,
    content: @Composable () -> Unit
) {
  val darkTheme = true
  val colorScheme = if (darkTheme) appearance.darkColors else appearance.lightColors

  MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content,
  )
}
