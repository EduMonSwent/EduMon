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
        val rotation by animateFloatAsState(
            targetValue = if (weeksExpanded) 180f else 0f,
            label = "chevron-rotation"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { weeksExpanded = !weeksExpanded }
                .testTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Week progression",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (weeksExpanded) "Collapse weeks" else "Expand weeks",
                    tint = cs.primary,
                    modifier = Modifier.rotate(rotation)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "$clampedPct%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = cs.primary,
                    modifier = Modifier.testTag(WeekProgDailyObjTags.WEEK_PROGRESS_PERCENT)
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { clampedPct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .testTag(WeekProgDailyObjTags.WEEK_PROGRESS_BAR),
                color = cs.primary,
                trackColor = cs.primary.copy(alpha = 0.15f)
            )
        }

        if (ui.weeks.isNotEmpty()) {
            AnimatedVisibility(
                visible = weeksExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                WeeksExpandedList(
                    weeks = ui.weeks,
                    selectedIndex = ui.selectedWeekIndex,
                    onSelect = { index -> viewModel.selectWeek(index) }, // assumes VM updates header + currentWeekContent
                    currentContent = ui.currentWeekContent,
                    isLoading = ui.isLoading,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .testTag(WeekProgDailyObjTags.WEEKS_LIST)
                )
            }
        }
    }
}

// Internal list for the expanded weeks
@Composable
private fun WeeksExpandedList(
    weeks: List<WeekProgressItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    currentContent: WeekContent,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(top = 10.dp)) {
        weeks.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val bg = if (selected) cs.primary.copy(alpha = 0.12f) else cs.onSurface.copy(alpha = 0.04f)
            var menuOpen by rememberSaveable(index) { mutableStateOf(false) }
            val chevronRotation by animateFloatAsState(
                targetValue = if (menuOpen) 180f else 0f,
                label = "row-dropdown-chevron"
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag(WeekProgDailyObjTags.WEEK_ROW_PREFIX + index),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallProgressRing(
                        percent = item.percent,
                        ringSize = 26.dp,
                        stroke = 4.dp,
                        tag = WeekProgDailyObjTags.WEEK_RING_PREFIX + index
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = cs.onSurface
                        )
                    }
                    Text(
                        "${item.percent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurface.copy(alpha = 0.75f),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag(WeekProgDailyObjTags.WEEK_PERCENT_PREFIX + index)
                    )
                    val finished = item.percent >= 100
                    if (finished) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Finished week",
                            tint = cs.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .testTag(WeekProgDailyObjTags.WEEK_STATUS_PREFIX + index)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.HourglassEmpty,
                            contentDescription = "Pending week",
                            tint = cs.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(20.dp)
                                .testTag(WeekProgDailyObjTags.WEEK_STATUS_PREFIX + index)
                        )
                    }

                    // Dropdown anchor (shows details for the SELECTED week using ui.currentWeekContent)
                    Box(Modifier.wrapContentSize(Alignment.Center)) {
                        IconButton(
                            onClick = {
                                // If tapping on a non-selected row, select it first.
                                if (!selected) onSelect(index)
                                menuOpen = !menuOpen
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("WEEK_DROPDOWN_BTN_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                contentDescription = if (menuOpen) "Hide details" else "Show details",
                                tint = cs.primary,
                                modifier = Modifier.rotate(chevronRotation)
                            )
                        }

                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier.testTag("WEEK_DROPDOWN_$index")
                        ) {
                            if (!selected) {
                                DropdownMenuItem(
                                    text = { Text("Select this week to view details") },
                                    onClick = {
                                        onSelect(index)
                                        // Keep menu open; content will update when VM publishes new state.
                                    }
                                )
                                return@DropdownMenu
                            }

                            if (isLoading) {
                                DropdownMenuItem(text = { Text("Loading…") }, onClick = {}, enabled = false)
                                return@DropdownMenu
                            }

                            // Exercises section
                            val exs = currentContent.exercises
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Exercises (${exs.count { it.done }}/${exs.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = cs.primary
                                    )
                                },
                                onClick = {},
                                enabled = false
                            )
                            exs.forEach { ex ->
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Outlined.FitnessCenter, contentDescription = null) },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(ex.title, modifier = Modifier.weight(1f))
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                imageVector = if (ex.done) Icons.Filled.Check else Icons.Outlined.HourglassEmpty,
                                                contentDescription = if (ex.done) "Done" else "Pending",
                                                tint = if (ex.done) cs.primary else cs.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    },
                                    onClick = {} // display-only
                                )
                            }

                            if (currentContent.courses.isNotEmpty() && exs.isNotEmpty()) {
                                Divider()
                            }

                            // Courses section
                            val courses = currentContent.courses
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Courses (${courses.count { it.read }}/${courses.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = cs.primary
                                    )
                                },
                                onClick = {},
                                enabled = false
                            )
                            courses.forEach { c ->
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Outlined.MenuBook, contentDescription = null) },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(c.title, modifier = Modifier.weight(1f))
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                imageVector = if (c.read) Icons.Filled.Check else Icons.Outlined.HourglassEmpty,
                                                contentDescription = if (c.read) "Read" else "Pending",
                                                tint = if (c.read) cs.primary else cs.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    },
                                    onClick = {} // display-only
                                )
                            }

                            if (exs.isEmpty() && courses.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No content for this week") },
                                    onClick = {},
                                    enabled = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
            topLeft = topLeft
        )
        drawArc(
            color = if (pct == 100) cs.primary else cs.primary.copy(alpha = 0.9f),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
            size = Size(diameter, diameter),
            topLeft = topLeft
        )
    }
}

// ---------- UI models for the dropdown content ----------
data class WeekContent(
    val exercises: List<Exercise>,
    val courses: List<CourseMaterial>
)

data class Exercise(
    val id: String,
    val title: String,
    val done: Boolean
)

data class CourseMaterial(
    val id: String,
    val title: String,
    val read: Boolean
)
