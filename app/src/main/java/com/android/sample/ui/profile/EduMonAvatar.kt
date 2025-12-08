package com.android.sample.ui.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.android.sample.ui.theme.UiValues

@Composable
fun EduMonAvatar(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
    showLevelLabel: Boolean = true,
    avatarSize: Dp = UiValues.AvatarSize,
    @DrawableRes avatarResId: Int = R.drawable.edumon,
    /** Optional override for the aura/background gradient around EduMon. */
    auraColors: List<Color>? = null,
) {
  val user by viewModel.userProfile.collectAsState()
  val accent by viewModel.accentEffective.collectAsState()
  val accessories = user.accessories
  val colorScheme = MaterialTheme.colorScheme

  val equipped =
      remember(accessories) {
        accessories
            .mapNotNull {
              val p = it.split(":")
              if (p.size == 2) p[0] to p[1] else null
            }
            .toMap()
      }

  val effectiveAuraColors =
      auraColors ?: listOf(accent.copy(alpha = UiValues.AuraAlpha), Color.Transparent)

  Column(
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Box(
            modifier =
                Modifier.size(avatarSize * UiValues.AvatarScale)
                    .clip(RoundedCornerShape(UiValues.AvatarCornerRadius)),
            contentAlignment = Alignment.Center) {
              // Aura / environment
              Box(
                  modifier =
                      Modifier.fillMaxSize()
                          .background(Brush.radialGradient(colors = effectiveAuraColors)))

              // Base EduMon sprite (configurable)
              Image(
                  painter = painterResource(id = avatarResId),
                  contentDescription = stringResource(id = R.string.edumon_content_description),
                  modifier = Modifier.size(avatarSize).zIndex(UiValues.ZBase))

              @Composable
              fun draw(slot: AccessorySlot, z: Float) {
                val id = equipped[slot.name.lowercase()] ?: return
                val res = viewModel.accessoryResId(slot, id)
                if (res != 0) {
                  Image(
                      painter = painterResource(res),
                      contentDescription = null,
                      modifier = Modifier.size(avatarSize).zIndex(z))
                }
              }

              draw(AccessorySlot.BACK, UiValues.ZBack)
              draw(AccessorySlot.TORSO, UiValues.ZTorso)
              draw(AccessorySlot.HEAD, UiValues.ZHead)
            }

        if (showLevelLabel) {
          Spacer(Modifier.height(UiValues.AvatarLevelSpacing))
          Text(
              text = stringResource(R.string.level_label, user.level),
              color = colorScheme.onBackground,
              fontWeight = FontWeight.SemiBold,
              fontSize = UiValues.LevelTextSize)
        }
      }
}
