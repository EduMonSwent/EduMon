package com.android.sample.todo.ui

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.sample.ui.theme.*

/** Centralized color definitions for the To-Do feature. */
object TodoColors {
  val Background = BackgroundDark // Background color for the whole screen

  // 🃏 Card container colors (used for each To-Do card)
  val Card = MidDarkCard
  val CardStroke = PurpleGrey40.copy(alpha = 0.3f) // Subtle border transparency

  // Text & icon colors
  val OnCard = TextLight
  val OnBackground = TextLight

  // Accent color for buttons, highlights, and FABs
  val Accent = AccentMagenta // Pink accent (from theme)

  // Secondary chip colors (for categories, priorities, etc.)
  val ChipViolet = AccentViolet // violet chip
  val ChipGreen = AccentMint // mint chip
  val ChipPink = Pink80 // softer pink chip
}

/** Reusable color scheme for Cards (To-Do items) */
@Composable
fun todoCardColors() =
    CardDefaults.cardColors(containerColor = TodoColors.Card, contentColor = TodoColors.OnCard)

/** Reusable color scheme for Buttons in the To-Do feature */
@Composable
fun todoButtonColors() =
    ButtonDefaults.buttonColors(
        containerColor = TodoColors.Accent, // button background
        contentColor = Color.White // text color on the button
        )
