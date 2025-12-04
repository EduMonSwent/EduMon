// OnboardingDimens.kt
package com.android.sample.ui.onBoarding

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// This code has been written partially using A.I (LLM).
object OnboardingDimens {
  // Layout & spacing
  val screenPadding = 24.dp
  val contentSpacing = 16.dp
  val logoSize = 160.dp
  val professorImageSize = 220.dp
  val dialogCornerRadius = 16.dp
  val dialogBorderWidth = 2.dp
  val dialogPadding = 16.dp
  val dialogVerticalPadding = 20.dp
  val tapToStartBottomPadding = 32.dp
  val startersRowSpacing = 16.dp
  val startersBottomSpacer = 24.dp
  val starterCardSize = 140.dp
  val starterCardCornerRadius = 20.dp
  val starterCardElevation = 4.dp
  val confirmButtonTopPadding = 24.dp

  // Typography
  val titleTextSize = 22.sp
  val bodyTextSize = 16.sp

  // Animation constants (floats / ints)
  const val tapBlinkAlphaMin = 0.3f
  const val tapBlinkAlphaMax = 1.0f
  const val tapBlinkDurationMillis = 700
  const val transitionDurationMillis = 500
  const val dialogueLetterDelayMillis: Long = 18L

  // AnimatedContent slide
  const val transitionSlideOffsetDivisor = 8

  // Misc
  const val dialogMaxLines = 4

  val professorLargeSize = 420.dp
  val professorOffsetY = (40).dp
}
