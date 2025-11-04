package com.android.sample.feature.weeks.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.sample.feature.weeks.model.WeekContent
import com.android.sample.feature.weeks.model.WeekProgressItem
import com.android.sample.feature.weeks.viewmodel.WeeksViewModel

@Composable
fun WeekProgressSection(
    viewModel: WeeksViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier,
) {
  val ui by viewModel.uiState.collectAsState()
  val cs = MaterialTheme.colorScheme
  var weeksExpanded by rememberSaveable { mutableStateOf(false) }
  val clampedPct = remember(ui.weekProgressPercent) { ui.weekProgressPercent.coerceIn(0, 100) }

  GlassSurface(modifier = modifier, testTag = WeekProgDailyObjTags.WEEK_PROGRESS_SECTION) {
    val rotation by
        animateFloatAsState(
            targetValue = if (weeksExpanded) 180f else 0f, label = "chevron-rotation")

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clickable { weeksExpanded = !weeksExpanded }
                .testTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE)) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Week progression",
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold, color = cs.onSurface),
                modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = if (weeksExpanded) "Collapse weeks" else "Expand weeks",
                tint = cs.primary,
                modifier = Modifier.rotate(rotation))
            Spacer(Modifier.width(8.dp))
            Text(
                "$clampedPct%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = cs.primary,
                modifier = Modifier.testTag(WeekProgDailyObjTags.WEEK_PROGRESS_PERCENT))
          }
          Spacer(Modifier.height(8.dp))
          LinearProgressIndicator(
              progress = { clampedPct / 100f },
              modifier =
                  Modifier.fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(50))
                      .testTag(WeekProgDailyObjTags.WEEK_PROGRESS_BAR),
              color = cs.primary,
              trackColor = cs.primary.copy(alpha = 0.15f))
        }

    if (ui.weeks.isNotEmpty()) {
      AnimatedVisibility(
          visible = weeksExpanded,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()) {
            WeeksExpandedList(
                weeks = ui.weeks,
                selectedIndex = ui.selectedWeekIndex,
                onSelect = { index -> viewModel.selectWeek(index) },
                currentContent = ui.currentWeekContent,
                isLoading = ui.isLoading,
                modifier = Modifier.padding(top = 12.dp).testTag(WeekProgDailyObjTags.WEEKS_LIST))
          }
    }
  }
}

/* ---------- Refactored: small, focused composables ---------- */

// Simple “router” that delegates one row to a dedicated composable.
@Composable
private fun WeeksExpandedList(
    weeks: List<WeekProgressItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    currentContent: WeekContent,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
  Column(modifier.fillMaxWidth().padding(top = 10.dp)) {
    weeks.forEachIndexed { index, item ->
      WeekRowItem(
          index = index,
          item = item,
          selected = index == selectedIndex,
          onSelect = onSelect,
          currentContent = currentContent,
          isLoading = isLoading,
      )
    }
  }
}

@Composable
private fun WeekRowItem(
    index: Int,
    item: WeekProgressItem,
    selected: Boolean,
    onSelect: (Int) -> Unit,
    currentContent: WeekContent,
    isLoading: Boolean,
) {
  val cs = MaterialTheme.colorScheme
  val bg = if (selected) cs.primary.copy(alpha = 0.12f) else cs.onSurface.copy(alpha = 0.04f)
  var menuOpen by rememberSaveable(index) { mutableStateOf(false) }
  val chevronRotation by
      animateFloatAsState(targetValue = if (menuOpen) 180f else 0f, label = "row-dropdown-chevron")

  Column(
      Modifier.fillMaxWidth()
          .padding(vertical = 4.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(bg)) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag(WeekProgDailyObjTags.WEEK_ROW_PREFIX + index),
            verticalAlignment = Alignment.CenterVertically) {
              SmallProgressRing(
                  percent = item.percent,
                  ringSize = 26.dp,
                  stroke = 4.dp,
                  tag = WeekProgDailyObjTags.WEEK_RING_PREFIX + index)
              Spacer(Modifier.width(12.dp))
              Column(Modifier.weight(1f)) {
                Text(
                    item.label,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = cs.onSurface)
              }
              Text(
                  "${item.percent}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = cs.onSurface.copy(alpha = 0.75f),
                  modifier =
                      Modifier.padding(end = 8.dp)
                          .testTag(WeekProgDailyObjTags.WEEK_PERCENT_PREFIX + index))
              WeekStatusIcon(
                  finished = item.percent >= 100,
                  modifier =
                      Modifier.size(20.dp).testTag(WeekProgDailyObjTags.WEEK_STATUS_PREFIX + index))

              // Dropdown anchor (details for the SELECTED week)
              Box(Modifier.wrapContentSize(Alignment.Center)) {
                IconButton(
                    onClick = {
                      if (!selected) onSelect(index)
                      menuOpen = !menuOpen
                    },
                    modifier = Modifier.size(28.dp).testTag("WEEK_DROPDOWN_BTN_$index")) {
                      Icon(
                          imageVector = Icons.Filled.ExpandMore,
                          contentDescription = if (menuOpen) "Hide details" else "Show details",
                          tint = cs.primary,
                          modifier = Modifier.rotate(chevronRotation))
                    }

                WeekDropdownMenu(
                    expanded = menuOpen,
                    onDismiss = { menuOpen = false },
                    index = index,
                    selected = selected,
                    isLoading = isLoading,
                    content = currentContent,
                    onSelect = onSelect)
              }
            }
      }
}

@Composable
private fun WeekStatusIcon(finished: Boolean, modifier: Modifier = Modifier) {
  val cs = MaterialTheme.colorScheme
  if (finished) {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = "Finished week",
        tint = cs.primary,
        modifier = modifier)
  } else {
    Icon(
        imageVector = Icons.Outlined.HourglassEmpty,
        contentDescription = "Pending week",
        tint = cs.onSurface.copy(alpha = 0.8f),
        modifier = modifier)
  }
}

@Composable
private fun WeekDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    index: Int,
    selected: Boolean,
    isLoading: Boolean,
    content: WeekContent,
    onSelect: (Int) -> Unit
) {
  DropdownMenu(
      expanded = expanded,
      onDismissRequest = onDismiss,
      modifier = Modifier.testTag("WEEK_DROPDOWN_$index")) {
        when {
          !selected ->
              DropdownMenuItem(
                  text = { Text("Select this week to view details") },
                  onClick = { onSelect(index) })
          isLoading -> DropdownMenuItem(text = { Text("Loading…") }, onClick = {}, enabled = false)
          else -> DropdownContent(content)
        }
      }
}

/* ------------ LOWER COMPLEXITY DROPDOWN CONTENT ------------- */

@Composable
private fun DropdownContent(currentContent: WeekContent) {
  val exercises = currentContent.exercises
  val courses = currentContent.courses

  // Early exit keeps branching minimal in this function
  if (exercises.isEmpty() && courses.isEmpty()) {
    DropdownMenuItem(text = { Text("No content for this week") }, onClick = {}, enabled = false)
    return
  }

  if (exercises.isNotEmpty()) ExercisesSection(exercises)
  if (exercises.isNotEmpty() && courses.isNotEmpty()) Divider()
  if (courses.isNotEmpty()) CoursesSection(courses)
}

@Composable
private fun ExercisesSection(exercises: List<com.android.sample.feature.weeks.model.Exercise>) {
  val cs = MaterialTheme.colorScheme
  SectionHeader(
      title = "Exercises (${exercises.count { it.done }}/${exercises.size})", color = cs.primary)
  exercises.forEach { ex ->
    DropdownListRow(
        title = ex.title,
        done = ex.done,
        leadingIcon = Icons.Outlined.FitnessCenter,
    )
  }
}

@Composable
private fun CoursesSection(courses: List<com.android.sample.feature.weeks.model.CourseMaterial>) {
  val cs = MaterialTheme.colorScheme
  SectionHeader(
      title = "Courses (${courses.count { it.read }}/${courses.size})", color = cs.primary)
  courses.forEach { c ->
    DropdownListRow(
        title = c.title,
        done = c.read,
        leadingIcon = Icons.Outlined.MenuBook,
    )
  }
}

@Composable
private fun DropdownListRow(
    title: String,
    done: Boolean,
    leadingIcon: ImageVector,
) {
  DropdownMenuItem(
      leadingIcon = { Icon(leadingIcon, contentDescription = null) },
      text = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(title, modifier = Modifier.weight(1f))
          Spacer(Modifier.width(8.dp))
          DonePendingIcon(done)
        }
      },
      onClick = {} // display-only
      )
}

@Composable
private fun DonePendingIcon(done: Boolean) {
  val cs = MaterialTheme.colorScheme
  Icon(
      imageVector = if (done) Icons.Filled.Check else Icons.Outlined.HourglassEmpty,
      contentDescription = if (done) "Done" else "Pending",
      tint = if (done) cs.primary else cs.onSurface.copy(alpha = 0.8f))
}

@Composable
private fun SectionHeader(title: String, color: androidx.compose.ui.graphics.Color) {
  DropdownMenuItem(
      text = { Text(title, style = MaterialTheme.typography.labelLarge, color = color) },
      onClick = {},
      enabled = false)
}

// Tiny circular progress ring (0..100%)
@Composable
private fun SmallProgressRing(
    percent: Int,
    ringSize: Dp = 24.dp,
    stroke: Dp = 3.dp,
    tag: String? = null
) {
  val pct = percent.coerceIn(0, 100)
  val progress = pct / 100f
  val cs = MaterialTheme.colorScheme
  Canvas(modifier = Modifier.size(ringSize).let { m -> if (tag != null) m.testTag(tag) else m }) {
    val strokePx = stroke.toPx()
    val diameter = size.minDimension - strokePx
    val topLeft = Offset(strokePx / 2f, strokePx / 2f)
    drawArc(
        color = cs.onSurface.copy(alpha = 0.15f),
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
        size = Size(diameter, diameter),
        topLeft = topLeft)
    drawArc(
        color = if (pct == 100) cs.primary else cs.primary.copy(alpha = 0.9f),
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
        size = Size(diameter, diameter),
        topLeft = topLeft)
  }
}
