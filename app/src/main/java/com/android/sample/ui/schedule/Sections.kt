package com.android.sample.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SchedulePadding = 16.dp

@Composable
fun SectionHeader(text: String) {
  Text(
      text,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = SchedulePadding))
}

@Composable
fun FramedSection(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(24.dp)
  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .shadow(8.dp, shape)
              .border(1.dp, com.android.sample.ui.theme.PurplePrimary, shape)
              .background(com.android.sample.ui.theme.DarkBlue.copy(alpha = 0.85f), shape)
              .padding(contentPadding)) {
        Column(content = content)
      }
}

@Composable
fun ThemedTabRow(selected: Int, onSelected: (Int) -> Unit, labels: List<String>) {
  val cs = MaterialTheme.colorScheme
  TabRow(
      selectedTabIndex = selected,
      containerColor = Color.Transparent,
      indicator = { positions ->
        TabRowDefaults.Indicator(
            modifier = Modifier.tabIndicatorOffset(positions[selected]).height(3.dp),
            color = cs.primary)
      },
      divider = {}) {
        labels.forEachIndexed { i, label ->
          Tab(
              selected = selected == i,
              onClick = { onSelected(i) },
              selectedContentColor = cs.primary,
              unselectedContentColor = cs.onSurface.copy(alpha = 0.75f),
              text = {
                Text(
                    label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 10.dp))
              })
        }
      }
}
