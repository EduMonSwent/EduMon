package com.android.sample.feature.weeks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeekDotsRow(viewModel: ObjectivesViewModel, modifier: Modifier = Modifier) {
  // Collect to recompose when objectives change.
  val uiState by viewModel.uiState.collectAsState()

  val cs = MaterialTheme.colorScheme

  Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        DayOfWeek.values().forEach { day ->
          // Base the check on the boolean result from the ViewModel.
          val metTarget = viewModel.isObjectivesOfDayCompleted(day)

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier.size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (metTarget) cs.primary.copy(alpha = 0.35f)
                            else cs.onSurface.copy(alpha = 0.08f))
                        .testTag(WeekProgDailyObjTags.WEEK_DOT_PREFIX + day.name),
                contentAlignment = Alignment.Center) {
                  if (metTarget) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "met",
                        modifier = Modifier.size(14.dp),
                        tint = cs.onPrimary)
                  } else {
                    Box(
                        Modifier.size(6.dp)
                            .clip(CircleShape)
                            .background(cs.onSurface.copy(alpha = 0.45f)))
                  }
                }

            Text(
                day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                fontSize = 11.sp,
                color = cs.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Clip)
          }
        }
      }
}
