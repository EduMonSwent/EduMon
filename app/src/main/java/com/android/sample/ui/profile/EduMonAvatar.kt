package com.android.sample.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.data.AccessorySlot
import com.android.sample.ui.theme.TextLight
import com.android.sample.ui.theme.UiValues

// This code has been written partially using A.I (LLM).
@Composable
fun EduMonAvatar(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
    showLevelLabel: Boolean = true,
    avatarSize: Dp = UiValues.AvatarSize,
) {
  val user by viewModel.userProfile.collectAsState()
  val accent by viewModel.accentEffective.collectAsState()
  val accessories = user.accessories

  val equipped =
      remember(accessories) {
        accessories
            .mapNotNull {
              val parts = it.split(":")
              if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
      }

  Column(
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Box(
            modifier =
                Modifier.size(avatarSize * UiValues.AvatarScale)
                    .clip(RoundedCornerShape(UiValues.AvatarCornerRadius)),
            contentAlignment = Alignment.Center) {
              Box(
                  modifier =
                      Modifier.background(
                              Brush.radialGradient(
                                  colors =
                                      listOf(
                                          accent.copy(alpha = UiValues.AuraAlpha),
                                          Color.Transparent)))
                          .size(avatarSize * UiValues.AvatarScale))

              val starterRes = viewModel.starterDrawable()

              Image(
                  painter = painterResource(id = starterRes),
                  contentDescription = stringResource(id = R.string.edumon_content_description),
                  modifier = Modifier.size(avatarSize).zIndex(UiValues.ZBase))

              @Composable
              fun DrawAccessory(slot: AccessorySlot, z: Float) {
                val id = equipped[slot.name.lowercase()] ?: return
                val res = viewModel.accessoryResId(slot, id)
                if (res != 0) {
                  Image(
                      painter = painterResource(res),
                      contentDescription = null,
                      modifier = Modifier.size(avatarSize).zIndex(z))
                }
              }

              DrawAccessory(AccessorySlot.BACK, UiValues.ZBack)
              DrawAccessory(AccessorySlot.TORSO, UiValues.ZTorso)
              DrawAccessory(AccessorySlot.HEAD, UiValues.ZHead)
            }

        if (showLevelLabel) {
          Spacer(Modifier.height(UiValues.AvatarLevelSpacing))
          Text(
              text = stringResource(R.string.level_label, user.level),
              color = TextLight,
              fontWeight = FontWeight.SemiBold,
              fontSize = UiValues.LevelTextSize)
        }
      }
}
