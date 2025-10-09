package com.android.sample.ui.widgets

/*
 * WeekProgDailyObj
 * ------------------------------------------------------------
 * Public container composable that takes a ViewModel as the single
 * source of truth. UI-only ephemeral state stays in the composable.
 * All persistent data (weeks, progress, objectives, day statuses...)
 * lives in the ViewModel.
 *
 * Notes for future you:
 * - Find `// TODO(...)` comments to plug in your feature logic.
 * - Visual tweaks: subtle "glass" sections, animated chevron,
 *   modern list for weeks, and compact chips.
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.ui.theme.EduMonTheme
import com.android.sample.ui.viewmodel.DayStatus
import com.android.sample.ui.viewmodel.Objective
import com.android.sample.ui.viewmodel.WeekProgressItem
import com.android.sample.ui.viewmodel.WeekProgressViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// ------------------------------------------------------------
// Test Tags (for UI tests)
// ------------------------------------------------------------
object WeekProgDailyObjTags {
  const val ROOT_CARD = "WEEK_PROG_DAILY_OBJ_CARD"

  // Week progress section
  const val WEEK_PROGRESS_SECTION = "WEEK_PROGRESS_SECTION"
  const val WEEK_PROGRESS_TOGGLE = "WEEK_PROGRESS_TOGGLE"
  const val WEEK_PROGRESS_BAR = "WEEK_PROGRESS_BAR"
  const val WEEKS_LIST = "WEEKS_LIST"

  // Week rows prefixes (index appended)
  const val WEEK_ROW_PREFIX = "WEEK_ROW_"
  const val WEEK_RING_PREFIX = "WEEK_RING_"
  const val WEEK_PERCENT_PREFIX = "WEEK_PERCENT_"
  const val WEEK_STATUS_PREFIX = "WEEK_STATUS_"

  // Objectives section
  const val OBJECTIVES_SECTION = "OBJECTIVES_SECTION"
  const val OBJECTIVES_TOGGLE = "OBJECTIVES_TOGGLE"
  const val OBJECTIVES_EMPTY = "OBJECTIVES_EMPTY"
  const val OBJECTIVES_SHOW_ALL_BUTTON = "OBJECTIVES_SHOW_ALL_BUTTON"

  // Objective rows prefixes (index appended)
  const val OBJECTIVE_ROW_PREFIX = "OBJECTIVE_ROW_"
  const val OBJECTIVE_START_BUTTON_PREFIX = "OBJECTIVE_START_BUTTON_"

  // Footer dots
  const val WEEK_DOTS_ROW = "WEEK_DOTS_ROW"
  const val WEEK_DOT_PREFIX = "WEEK_DOT_" // DayOfWeek.name appended
}

// ------------------------------------------------------------
// Public Composable: ViewModel container (single source of truth)
// ------------------------------------------------------------
@Composable
fun WeekProgDailyObj(
    viewModel: WeekProgressViewModel,
    modifier: Modifier = Modifier,
    pendingIcon: String = "⌛" // UI customization that does not belong in VM
) {
  val state by viewModel.uiState.collectAsState()
  val cs = MaterialTheme.colorScheme

  Card(
      modifier =
          modifier
              .padding(horizontal = 16.dp, vertical = 10.dp)
              .wrapContentWidth()
              .widthIn(min = 320.dp, max = 600.dp) // dynamic width within bounds
              .testTag(WeekProgDailyObjTags.ROOT_CARD),
      colors = CardDefaults.cardColors(containerColor = cs.surface),
      shape = RoundedCornerShape(24.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
          // Local UI-only state (should not live in VM)
          var weeksExpanded by rememberSaveable { mutableStateOf(false) }
          val clampedPct =
              remember(state.weekProgressPercent) { state.weekProgressPercent.coerceIn(0, 100) }

          // ------------------------------------------------------------
          // Week Progress Section (glass surface + animated chevron)
          // ------------------------------------------------------------
          GlassSurface(
              modifier = Modifier.fillMaxWidth(),
              testTag = WeekProgDailyObjTags.WEEK_PROGRESS_SECTION) {
                val rotation by
                    animateFloatAsState(
                        targetValue = if (weeksExpanded) 180f else 0f, label = "chevron-rotation")

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { weeksExpanded = !weeksExpanded }
                            .testTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE)) {
                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Week progression",
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold, color = cs.onSurface),
                                modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription =
                                    if (weeksExpanded) "Collapse weeks" else "Expand weeks",
                                tint = cs.primary,
                                modifier = Modifier.rotate(rotation))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$clampedPct%",
                                style =
                                    MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium),
                                color = cs.primary)
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

                if (state.weeks.isNotEmpty()) {
                  AnimatedVisibility(
                      visible = weeksExpanded,
                      enter = fadeIn() + expandVertically(),
                      exit = fadeOut() + shrinkVertically()) {
                        WeeksExpandedList(
                            weeks = state.weeks,
                            selectedIndex = state.selectedWeekIndex,
                            onSelect = { item ->
                              val idx = state.weeks.indexOf(item)
                              if (idx >= 0) viewModel.selectWeek(idx)
                            },
                            pendingIcon = pendingIcon,
                            modifier =
                                Modifier.padding(top = 12.dp)
                                    .testTag(WeekProgDailyObjTags.WEEKS_LIST))
                      }
                }
              }

          Spacer(Modifier.height(18.dp))

          // ------------------------------------------------------------
          // Objectives Section (collapsible: show first, expand the rest)
          // ------------------------------------------------------------
          GlassSurface(
              modifier = Modifier.fillMaxWidth(),
              testTag = WeekProgDailyObjTags.OBJECTIVES_SECTION) {
                val objectives = state.objectives
                var objectivesExpanded by rememberSaveable { mutableStateOf(false) }
                val hasMultiple = objectives.size > 1

                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .let { base ->
                              if (hasMultiple)
                                  base.clickable { objectivesExpanded = !objectivesExpanded }
                              else base
                            }
                            .let { base ->
                              if (hasMultiple) base.testTag(WeekProgDailyObjTags.OBJECTIVES_TOGGLE)
                              else base
                            },
                    verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          if (!hasMultiple) "\uD83C\uDFAF Today’s Objective"
                          else "\uD83C\uDFAF Today’s Objectives",
                          style =
                              MaterialTheme.typography.titleMedium.copy(
                                  color = cs.onSurface, fontWeight = FontWeight.SemiBold),
                          modifier = Modifier.weight(1f))
                      if (hasMultiple) {
                        val rotation by
                            animateFloatAsState(
                                targetValue = if (objectivesExpanded) 180f else 0f,
                                label = "objective-chevron")
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription =
                                if (objectivesExpanded) "Collapse objectives"
                                else "Expand objectives",
                            tint = cs.primary,
                            modifier = Modifier.rotate(rotation))
                      }
                    }

                Spacer(Modifier.height(8.dp))

                if (objectives.isEmpty()) {
                  Text(
                      "Fine Work ! No objectives for today. Enjoy your learning journey! \uD83C\uDF89",
                      color = cs.onSurface.copy(alpha = 0.6f),
                      fontSize = 12.sp,
                      modifier = Modifier.testTag(WeekProgDailyObjTags.OBJECTIVES_EMPTY))
                } else {
                  // Always show the first objective
                  ObjectiveRow(
                      index = 0,
                      objective = objectives.first(),
                      showWhy = state.showWhy,
                      onStart = { viewModel.startObjective(0) })

                  val remaining = objectives.drop(1)
                  if (remaining.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = objectivesExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()) {
                          Column(modifier = Modifier.padding(top = 16.dp)) {
                            remaining.forEachIndexed { idx, obj ->
                              HorizontalDivider(
                                  modifier = Modifier.padding(vertical = 14.dp),
                                  color = cs.onSurface.copy(alpha = 0.08f))
                              ObjectiveRow(
                                  index = idx + 1,
                                  objective = obj,
                                  showWhy = state.showWhy,
                                  onStart = { viewModel.startObjective(idx + 1) })
                            }
                          }
                        }

                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = { objectivesExpanded = !objectivesExpanded },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier =
                            Modifier.testTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON)) {
                          Text(
                              if (objectivesExpanded) "Show less"
                              else "Show all (${objectives.size})",
                              style = MaterialTheme.typography.labelMedium,
                              fontWeight = FontWeight.Medium)
                        }
                  }
                }
              }
        }

        // ------------------------------------------------------------
        // Footer: weekly dots
        // ------------------------------------------------------------
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

// ============================================================
// Atoms / Molecules
// ============================================================
/** Displays a small rounded metadata chip (e.g., course code or minutes). */
@Composable
private fun MetaChip(text: String) {
  val cs = MaterialTheme.colorScheme
  Surface(
      color = cs.onSurface.copy(alpha = 0.08f),
      contentColor = cs.onSurface,
      shape = RoundedCornerShape(12.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium)
      }
}

/** Gradient primary button used to trigger starting an objective. */
@Composable
private fun StartButton(onClick: () -> Unit, tag: String? = null) {
  val cs = MaterialTheme.colorScheme
  val gradient = Brush.linearGradient(listOf(cs.primary, cs.primary.copy(alpha = 0.85f)))
  Button(
      onClick = onClick,
      colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
      shape = RoundedCornerShape(14.dp),
      modifier =
          Modifier.height(42.dp).background(gradient, RoundedCornerShape(14.dp)).let { m ->
            if (tag != null) m.testTag(tag) else m
          }) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text("Start", fontWeight = FontWeight.SemiBold)
      }
}

/** Weekly day status row with a small dot / check for each day of week. */
@Composable
private fun WeekDotsRow(dayStatuses: List<DayStatus>, modifier: Modifier = Modifier) {
  val cs = MaterialTheme.colorScheme
  val ordered =
      remember(dayStatuses) {
        val byDow = dayStatuses.associateBy { it.dayOfWeek }
        DayOfWeek.values().map { byDow[it] ?: DayStatus(it, false) }
      }
  Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        ordered.forEach { status ->
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier.size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (status.metTarget) cs.primary.copy(alpha = 0.35f)
                            else cs.onSurface.copy(alpha = 0.08f))
                        .testTag(WeekProgDailyObjTags.WEEK_DOT_PREFIX + status.dayOfWeek.name),
                contentAlignment = Alignment.Center) {
                  if (status.metTarget) {
                    Icon(
                        Icons.Default.Check,
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
                status.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                fontSize = 11.sp,
                color = cs.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp))
          }
        }
      }
}

/** Tiny circular progress ring (0..100%) used inside week list rows. */
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

/** Expanded list of weeks with selectable progress rows and status icon. */
@Composable
private fun WeeksExpandedList(
    weeks: List<WeekProgressItem>,
    selectedIndex: Int,
    onSelect: (WeekProgressItem) -> Unit,
    pendingIcon: String,
    modifier: Modifier = Modifier
) {
  val cs = MaterialTheme.colorScheme
  Column(modifier.fillMaxWidth().padding(top = 10.dp)) {
    weeks.forEachIndexed { index, item ->
      val selected = index == selectedIndex
      val bg = if (selected) cs.primary.copy(alpha = 0.12f) else cs.onSurface.copy(alpha = 0.04f)
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(bg)
                  .clickable { onSelect(item) }
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
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = cs.onSurface)
            }
            Text(
                "${item.percent}%",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurface.copy(alpha = 0.75f),
                modifier =
                    Modifier.padding(end = 8.dp)
                        .testTag(WeekProgDailyObjTags.WEEK_PERCENT_PREFIX + index))
            val finished = item.percent >= 100
            if (finished) {
              Icon(
                  Icons.Default.Check,
                  contentDescription = "Finished week",
                  tint = cs.primary,
                  modifier =
                      Modifier.size(20.dp).testTag(WeekProgDailyObjTags.WEEK_STATUS_PREFIX + index))
            } else {
              Text(
                  pendingIcon,
                  fontSize = 14.sp,
                  modifier = Modifier.testTag(WeekProgDailyObjTags.WEEK_STATUS_PREFIX + index))
            }
          }
    }
  }
}

/** Displays a single objective: title, meta chips, reason text and Start button. */
@Composable
private fun ObjectiveRow(index: Int, objective: Objective, showWhy: Boolean, onStart: () -> Unit) {
  val cs = MaterialTheme.colorScheme
  Column(Modifier.fillMaxWidth().testTag(WeekProgDailyObjTags.OBJECTIVE_ROW_PREFIX + index)) {
    Text(
        objective.title,
        style =
            MaterialTheme.typography.titleLarge.copy(
                color = cs.onSurface, fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 6.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          MetaChip(objective.course)
          if (objective.estimateMinutes > 0) MetaChip("${objective.estimateMinutes}m")
        }
    if (showWhy && objective.reason.isNotBlank()) {
      Text(
          objective.reason,
          color = cs.onSurface.copy(alpha = 0.7f),
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 6.dp))
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 14.dp)) {
          StartButton(
              onClick = onStart, tag = WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + index)
        }
  }
}

// ============================================================
// Preview
// ============================================================
@Preview(showBackground = true, backgroundColor = 0xFF0F0F1A)
@Composable
private fun WeekProgDailyObjPreview() {
  EduMonTheme { WeekProgDailyObj(viewModel = WeekProgressViewModel()) }
}

// ============================================================
// Glass Surface (modern translucent section)
// ============================================================
@Composable
private fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
  val cs = MaterialTheme.colorScheme
  val gradient =
      Brush.linearGradient(
          listOf(cs.surfaceVariant.copy(alpha = 0.20f), cs.surfaceVariant.copy(alpha = 0.10f)))
  Surface(
      color = Color.Transparent,
      shape = shape,
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
      modifier = modifier.let { m -> if (testTag != null) m.testTag(testTag) else m }) {
        Column(
            Modifier.clip(shape)
                .background(gradient)
                .border(1.dp, cs.onSurface.copy(alpha = 0.12f), shape)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            content = content)
      }
}
