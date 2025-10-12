package com.android.sample.ui.widgets

/*
 * WeekProgDailyObj (modularized)
 * ------------------------------------------------------------
 * Public container composable that takes a ViewModel as the single
 * source of truth. Week Progress UI and Daily Objectives UI are
 * extracted into separate files for readability and testing.
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.sample.ui.theme.EduMonTheme
import com.android.sample.ui.viewmodel.WeekProgressViewModel

@Composable
fun WeekProgDailyObj(
    viewModel: WeekProgressViewModel,
    modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsState()
  val cs = MaterialTheme.colorScheme

  Card(
      modifier =
          modifier
              .padding(horizontal = 16.dp, vertical = 10.dp)
              .wrapContentWidth()
              .widthIn(min = 320.dp, max = 600.dp)
              .testTag(WeekProgDailyObjTags.ROOT_CARD),
      colors = CardDefaults.cardColors(containerColor = cs.surface),
      shape = RoundedCornerShape(24.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(16.dp)) {
          // Week Progress Section
          WeekProgressSection(
              weekProgressPercent = state.weekProgressPercent,
              weeks = state.weeks,
              selectedWeekIndex = state.selectedWeekIndex,
              onSelectWeek = { idx -> viewModel.selectWeek(idx) },
              modifier = Modifier.fillMaxWidth())

          Spacer(Modifier.height(18.dp))

          // Objectives Section
          DailyObjectivesSection(
              objectives = state.objectives,
              showWhy = state.showWhy,
              onStartObjective = { idx -> viewModel.startObjective(idx) },
              modifier = Modifier.fillMaxWidth())
        }

        // Footer: weekly dots
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = cs.onSurface.copy(alpha = 0.08f))

        WeekDotsRow(
            dayStatuses = state.dayStatuses,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .testTag(WeekProgDailyObjTags.WEEK_DOTS_ROW))
      }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F1A)
@Composable
private fun WeekProgDailyObjPreview() {
  EduMonTheme { WeekProgDailyObj(viewModel = WeekProgressViewModel()) }
}
