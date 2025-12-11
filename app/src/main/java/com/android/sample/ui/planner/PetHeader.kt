package com.android.sample.ui.planner

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.android.sample.R
import com.android.sample.screens.CreatureHouseCard
import com.android.sample.ui.profile.EduMonAvatar

@Composable
fun PetHeader(
    modifier: Modifier = Modifier,
    level: Int,
    @DrawableRes avatarResId: Int = R.drawable.edumon,
    @DrawableRes environmentResId: Int = R.drawable.home, // ✅ neutral default
    backgroundBrush: Brush? = null,
) {
  // Optional extra background (if you want a gradient behind the card)
  val outerModifier =
      if (backgroundBrush != null) {
        modifier.fillMaxWidth().background(backgroundBrush)
      } else {
        modifier.fillMaxWidth()
      }

  Box(modifier = outerModifier) {
    CreatureHouseCard(
        creatureResId = avatarResId, // ✅ chosen Edumon sprite
        level = level,
        environmentResId = environmentResId, // ✅ chosen environment
        overrideCreature = {
          // Reuse your avatar system so accessories / aura / accent work
          EduMonAvatar(
              showLevelLabel = false,
              avatarResId = avatarResId,
          )
        },
        modifier = Modifier.fillMaxWidth(),
    )
  }
}
