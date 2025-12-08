package com.android.sample.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.sample.R

/**
 * Visual configuration for an Edumon: creature sprite, environment background and color scheme
 * (light + dark).
 *
 * starterId should match the ids used by StarterSelectionScreen (e.g. "pyrmon", "aquamon",
 * "floramon").
 */
data class EdumonAppearance(
    val id: String,
    @DrawableRes val creatureResId: Int,
    @DrawableRes val environmentResId: Int,
    val lightColors: ColorScheme,
    val darkColors: ColorScheme,
)

object EdumonAppearances {

  // Default look (used before onboarding or as fallback)
  val Default =
      EdumonAppearance(
          id = "default",
          creatureResId = R.drawable.edumon,
          environmentResId = R.drawable.home,
          lightColors =
              lightColorScheme(
                  primary = Color(0xFF6750A4),
                  secondary = Color(0xFF625B71),
                  tertiary = Color(0xFF7D5260)),
          darkColors =
              darkColorScheme(
                  primary = Color(0xFFD0BCFF),
                  secondary = Color(0xFFCCC2DC),
                  tertiary = Color(0xFFEFB8C8)),
      )

  // 🔥 Pyrmon – warm, orange-ish theme
  val Pyrmon =
      EdumonAppearance(
          id = "pyrmon",
          creatureResId = R.drawable.edumon3, // adjust if you want a specific sprite
          environmentResId = R.drawable.bg_pyrmon,
          lightColors =
              lightColorScheme(
                  primary = Color(0xFF039BE5),
                  secondary = Color(0xFF26C6DA),
                  tertiary = Color(0xFF00ACC1)),
          darkColors =
              darkColorScheme(
                  primary = Color(0xFF29B6F6),
                  secondary = Color(0xFF4DD0E1),
                  tertiary = Color(0xFF26C6DA)),
      )

  // 💧 Aquamon – blue/cyan theme
  val Aquamon =
      EdumonAppearance(
          id = "aquamon",
          creatureResId = R.drawable.edumon2,
          environmentResId = R.drawable.bg_aquamon,
          lightColors =
              lightColorScheme(
                  primary = Color(0xFFFF7043),
                  secondary = Color(0xFFFFA726),
                  tertiary = Color(0xFFFFB300)),
          darkColors =
              darkColorScheme(
                  primary = Color(0xFFFFB74D),
                  secondary = Color(0xFFFFCC80),
                  tertiary = Color(0xFFFFE082)),
      )

  // 🌿 Floramon – green theme
  val Floramon =
      EdumonAppearance(
          id = "floramon",
          creatureResId = R.drawable.edumon1,
          environmentResId = R.drawable.bg_floramon,
          lightColors =
              lightColorScheme(
                  primary = Color(0xFF43A047),
                  secondary = Color(0xFF66BB6A),
                  tertiary = Color(0xFF8BC34A)),
          darkColors =
              darkColorScheme(
                  primary = Color(0xFF66BB6A),
                  secondary = Color(0xFF81C784),
                  tertiary = Color(0xFFAED581)),
      )

  val all = listOf(Default, Pyrmon, Aquamon, Floramon)

  /** Map onboarding/starter id → appearance. */
  fun fromStarterId(starterId: String?): EdumonAppearance =
      when (starterId?.lowercase()) {
        "pyrmon" -> Pyrmon
        "aquamon" -> Aquamon
        "floramon" -> Floramon
        else -> Default
      }
}
