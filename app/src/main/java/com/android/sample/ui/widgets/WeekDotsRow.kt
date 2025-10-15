package com.android.sample.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.ui.viewmodel.DayStatus
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeekDotsRow(dayStatuses: List<DayStatus>, modifier: Modifier = Modifier) {
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
                status.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                fontSize = 11.sp,
                color = cs.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Clip)
          }
        }
      }
}
