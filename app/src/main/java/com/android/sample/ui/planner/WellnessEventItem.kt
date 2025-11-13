// file: com/android/sample/ui/planner/WellnessEventItem.kt
package com.android.sample.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.feature.schedule.data.planner.WellnessEventType

@Composable
fun WellnessEventItem(
    title: String,
    time: String,
    description: String,
    eventType: WellnessEventType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val cs = MaterialTheme.colorScheme
  val accent = eventType.primaryColor

  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable(onClick = onClick)
              .padding(vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically) {
        // Left badge (same style as Classes rows)
        Box(
            modifier =
                Modifier.size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = .16f))
                    .border(1.dp, accent.copy(alpha = .32f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center) {
              Icon(
                  painter = painterResource(id = eventType.iconRes),
                  contentDescription = null,
                  tint = accent,
                  modifier = Modifier.size(20.dp))
            }

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
          Text(
              text = title,
              color = cs.onSurface,
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp)
          Spacer(Modifier.height(2.dp))
          Text(text = time, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
          if (description.isNotBlank()) {
            Text(text = description, color = cs.onSurface.copy(alpha = 0.65f), fontSize = 12.sp)
          }
        }

        Spacer(Modifier.width(6.dp))

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = stringResource(R.string.details),
            tint = cs.onSurface.copy(alpha = 0.50f),
            modifier = Modifier.size(20.dp))
      }
}
