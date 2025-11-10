package com.android.sample.ui.planner

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.sample.R
import com.android.sample.screens.CreatureHouseCard

@Composable
fun PetHeader(
    level: Int,
    modifier: Modifier = Modifier,
    environmentResId: Int = R.drawable.epfl_amphi_background,
    creatureResId: Int = R.drawable.edumon
) {
  Box(modifier = modifier.fillMaxWidth()) {
    // Exact same card as on the Home screen
    CreatureHouseCard(
        creatureResId = creatureResId,
        level = level,
        environmentResId = environmentResId,
        modifier = Modifier.fillMaxWidth())
  }
}
