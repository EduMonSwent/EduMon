package com.android.sample.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.model.calendar.StudyItem
import com.android.sample.ui.theme.PurpleBorder
import com.android.sample.ui.theme.PurpleBottom
import com.android.sample.ui.theme.PurpleSoft
import com.android.sample.ui.theme.PurpleTop
import com.android.sample.ui.theme.VioletSoft
import java.time.LocalDate

@Composable
fun DayCell(
    date: LocalDate,
    tasks: List<StudyItem>,
    isSelected: Boolean,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
  val hasTasks = tasks.isNotEmpty()

  val selectedBrush = Brush.verticalGradient(colors = listOf(PurpleTop, PurpleBottom))

  Box(
      modifier =
          modifier
              .aspectRatio(1f)
              .size(size)
              .clip(RoundedCornerShape(12.dp))
              .background(
                  brush =
                      if (isSelected) selectedBrush
                      else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
              .border(
                  width = 1.dp,
                  color = if (isSelected) PurpleBorder else PurpleSoft.copy(alpha = 0.4f),
                  shape = RoundedCornerShape(12.dp))
              .clickable { onDateClick(date) },
      contentAlignment = Alignment.Center) {
        if (isSelected) {
          Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.Transparent,
              modifier =
                  Modifier.size(size)
                      .shadow(8.dp, RoundedCornerShape(12.dp))
                      .background(selectedBrush, RoundedCornerShape(12.dp))) {}
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Day number
              Text(
                  text = date.dayOfMonth.toString(),
                  style =
                      MaterialTheme.typography.bodyLarge.copy(
                          color =
                              if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                          fontSize = 16.sp),
                  textAlign = TextAlign.Center)

              // Small dots for tasks
              if (hasTasks) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)) {
                      repeat(tasks.size.coerceAtMost(3)) {
                        Canvas(modifier = Modifier.size(4.dp).padding(horizontal = 1.dp)) {
                          drawCircle(color = VioletSoft.copy(alpha = 0.9f))
                        }
                      }
                    }
              }
            }
      }
}
