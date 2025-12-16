// This code was written with the assistance of an AI (LLM).
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

private object AvatarConstants {
    const val ACCESSORY_SEPARATOR = ":"
    const val EXPECTED_PARTS_COUNT = 2
    const val SLOT_INDEX = 0
    const val ID_INDEX = 1
    const val NO_RESOURCE = 0
}

@Composable
fun EduMonAvatar(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
    showLevelLabel: Boolean = true,
    avatarSize: Dp = UiValues.AvatarSize,
) {
    val user by viewModel.userProfile.collectAsState()
    val accent by viewModel.accentEffective.collectAsState()

    val equipped = remember(user.accessories) {
        user.accessories
            .mapNotNull { entry ->
                val parts = entry.split(AvatarConstants.ACCESSORY_SEPARATOR)
                if (parts.size == AvatarConstants.EXPECTED_PARTS_COUNT) {
                    parts[AvatarConstants.SLOT_INDEX] to parts[AvatarConstants.ID_INDEX]
                } else {
                    null
                }
            }
            .toMap()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize * UiValues.AvatarScale)
                .clip(RoundedCornerShape(UiValues.AvatarCornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = UiValues.AuraAlpha),
                                Color.Transparent
                            )
                        )
                    )
                    .size(avatarSize * UiValues.AvatarScale)
            )

            Image(
                painter = painterResource(id = viewModel.starterDrawable()),
                contentDescription = stringResource(id = R.string.edumon_content_description),
                modifier = Modifier
                    .size(avatarSize)
                    .zIndex(UiValues.ZBase)
            )

            AccessoryLayer(viewModel, equipped, AccessorySlot.BACK, avatarSize, UiValues.ZBack)
            AccessoryLayer(viewModel, equipped, AccessorySlot.TORSO, avatarSize, UiValues.ZTorso)
            AccessoryLayer(viewModel, equipped, AccessorySlot.HEAD, avatarSize, UiValues.ZHead)
        }

        if (showLevelLabel) {
            Spacer(Modifier.height(UiValues.AvatarLevelSpacing))
            Text(
                text = stringResource(R.string.level_label, user.level),
                color = TextLight,
                fontWeight = FontWeight.SemiBold,
                fontSize = UiValues.LevelTextSize
            )
        }
    }
}

@Composable
private fun AccessoryLayer(
    viewModel: ProfileViewModel,
    equipped: Map<String, String>,
    slot: AccessorySlot,
    size: Dp,
    zIndex: Float
) {
    val accessoryId = equipped[slot.name.lowercase()] ?: return
    val resId = viewModel.accessoryResId(slot, accessoryId)

    if (resId != AvatarConstants.NO_RESOURCE) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .zIndex(zIndex)
        )
    }
}