package com.android.sample.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.android.sample.ui.theme.TextLight
import com.android.sample.ui.theme.UiValues

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
              val p = it.split(":")
              if (p.size == 2) p[0] to p[1] else null
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
                      Modifier.fillMaxSize()
                          .background(
                              Brush.radialGradient(
                                  colors =
                                      listOf(
                                          accent.copy(alpha = UiValues.AuraAlpha),
                                          Color.Transparent))))

              Image(
                  painter = painterResource(id = R.drawable.edumon),
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
              color = TextLight,
              fontWeight = FontWeight.SemiBold,
              fontSize = UiValues.LevelTextSize)
        }
      }
}
