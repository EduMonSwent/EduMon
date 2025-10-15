package com.android.sample.feature.weeks.widgets

/*
 * WeekProgDailyObj (modularized)
 * ------------------------------------------------------------
 * Public container composable that takes three ViewModels as sources of truth:
 * - WeeksViewModel for the week section
 * - ObjectivesViewModel for the objectives section
 * - WeekDotsViewModel for the footer 7-day row
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
import androidx.compose.ui.unit.dp
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.feature.weeks.viewmodel.WeekDotsViewModel
import com.android.sample.feature.weeks.viewmodel.WeeksViewModel

@Composable
fun WeekProgDailyObj(
    weeksViewModel: WeeksViewModel,
    objectivesViewModel: ObjectivesViewModel,
    dotsViewModel: WeekDotsViewModel,
    modifier: Modifier = Modifier,
) {
  val weeks by weeksViewModel.uiState.collectAsState()
  val objectives by objectivesViewModel.uiState.collectAsState()
  val dots by dotsViewModel.uiState.collectAsState()
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
              weekProgressPercent = weeks.weekProgressPercent,
              weeks = weeks.weeks,
              selectedWeekIndex = weeks.selectedWeekIndex,
              onSelectWeek = { idx -> weeksViewModel.selectWeek(idx) },
              modifier = Modifier.fillMaxWidth())

          Spacer(Modifier.height(18.dp))

          // Objectives Section
          DailyObjectivesSection(
              objectives = objectives.objectives,
              showWhy = objectives.showWhy,
              onStartObjective = { idx -> objectivesViewModel.startObjective(idx) },
              modifier = Modifier.fillMaxWidth())
        }

        // Footer: weekly dots
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = cs.onSurface.copy(alpha = 0.08f))

        WeekDotsRow(
            dayStatuses = dots.dayStatuses,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .testTag(WeekProgDailyObjTags.WEEK_DOTS_ROW))
      }
}
