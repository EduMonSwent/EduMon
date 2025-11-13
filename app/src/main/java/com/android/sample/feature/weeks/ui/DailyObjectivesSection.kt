package com.android.sample.feature.weeks.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.PurplePrimary

@Composable
fun DailyObjectivesSection(
    modifier: Modifier = Modifier,
    viewModel: ObjectivesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
  val ui by viewModel.uiState.collectAsState()
  val objectives = ui.objectives
  val showWhy = ui.showWhy

  val cs = MaterialTheme.colorScheme
  GlassSurface(modifier = modifier, testTag = WeekProgDailyObjTags.OBJECTIVES_SECTION) {
    var objectivesExpanded by rememberSaveable { mutableStateOf(false) }
    val hasMultiple = objectives.size > 1

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .let { base ->
                  if (hasMultiple) base.clickable { objectivesExpanded = !objectivesExpanded }
                  else base
                }
                .let { base ->
                  if (hasMultiple) base.testTag(WeekProgDailyObjTags.OBJECTIVES_TOGGLE) else base
                },
        verticalAlignment = Alignment.CenterVertically) {
          // Icon leading the header instead of emoji
          Icon(
              imageVector = Icons.Outlined.Today,
              contentDescription = null,
              tint = cs.primary,
              modifier = Modifier.size(30.dp).padding(end = 8.dp))
          Text(
              if (!hasMultiple) "Today’s Objective" else "Today’s Objectives",
              style =
                  MaterialTheme.typography.titleMedium.copy(
                      color = cs.onSurface, fontWeight = FontWeight.SemiBold),
              modifier = Modifier.weight(1f))
          if (hasMultiple) {
            val rotation by
                animateFloatAsState(
                    targetValue = if (objectivesExpanded) 180f else 0f, label = "objective-chevron")
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription =
                    if (objectivesExpanded) "Collapse objectives" else "Expand objectives",
                tint = cs.primary,
                modifier = Modifier.rotate(rotation))
          }
        }

    Spacer(Modifier.height(8.dp))

    if (objectives.isEmpty()) {
      Text(
          "Fine Work! No objectives for today. Enjoy your learning journey!",
          color = cs.onSurface.copy(alpha = 0.6f),
          fontSize = 12.sp,
          modifier = Modifier.testTag(WeekProgDailyObjTags.OBJECTIVES_EMPTY))
    } else {
      ObjectiveRow(index = 0, objective = objectives.first(), showWhy = showWhy) {
        viewModel.startObjective(0)
      }

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
                  ObjectiveRow(index = idx + 1, objective = obj, showWhy = showWhy) {
                    viewModel.startObjective(idx + 1)
                  }
                }
              }
            }

        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = { objectivesExpanded = !objectivesExpanded },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.testTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON)) {
              Text(
                  if (objectivesExpanded) "Show less" else "Show all (${objectives.size})",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.testTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_LABEL))
            }
      }
    }
  }
}

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
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 14.dp)) {
          StartButton(
              onClick = onStart, tag = WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + index)
        }
  }
}

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

@Composable
private fun StartButton(onClick: () -> Unit, tag: String? = null) {
  val cs = MaterialTheme.colorScheme
  val gradient = Brush.linearGradient(listOf(AccentViolet, PurplePrimary))
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
