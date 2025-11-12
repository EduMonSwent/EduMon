package com.android.sample.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.feature.weeks.ui.GlassSurface

@Composable
private fun GlassSurfaceCompact(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
  val cs = MaterialTheme.colorScheme
  val gradient =
      Brush.linearGradient(
          listOf(cs.surfaceVariant.copy(alpha = 0.20f), cs.surfaceVariant.copy(alpha = 0.10f)))
  Surface(color = Color.Transparent, shape = shape) {
    Column(
        Modifier.clip(shape)
            .background(gradient)
            .border(1.dp, cs.onSurface.copy(alpha = 0.12f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        content = content)
  }
}

@Composable
fun ThemedTabRow(selected: Int, onSelected: (Int) -> Unit, labels: List<String>) {
  val cs = MaterialTheme.colorScheme
  val chipShape = RoundedCornerShape(14.dp)
  val density = LocalDensity.current

  GlassSurfaceCompact(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          labels.forEachIndexed { i, label ->
            val isSelected = i == selected
            var textPxWidth by remember { mutableStateOf(0) }

            Surface(
                onClick = { onSelected(i) },
                shape = chipShape,
                color = if (isSelected) cs.primary else Color.Transparent,
                modifier = Modifier.height(36.dp), // denser
                tonalElevation = if (isSelected) 2.dp else 0.dp) {
                  Box(
                      modifier =
                          Modifier.fillMaxHeight().padding(horizontal = 16.dp), // chip padding
                      contentAlignment = Alignment.CenterStart) {
                        // Use BasicText (or Text) with onTextLayout PARAMETER
                        androidx.compose.foundation.text.BasicText(
                            text = label,
                            style =
                                androidx.compose.ui.text.TextStyle(
                                    fontSize = 16.sp, // a bit bigger
                                    fontWeight = FontWeight.SemiBold,
                                    color =
                                        if (isSelected) cs.onPrimary
                                        else cs.onSurface.copy(alpha = 0.85f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start),
                            onTextLayout = { layoutResult ->
                              textPxWidth = layoutResult.size.width
                            })

                        if (isSelected && textPxWidth > 0) {
                          val underlineWidth = with(density) { textPxWidth.toDp() }
                          Box(
                              modifier =
                                  Modifier.align(Alignment.BottomStart)
                                      .padding(bottom = 5.dp) // lift from bottom
                                      .height(3.dp)
                                      .width(underlineWidth) // exactly under the word
                                      .clip(RoundedCornerShape(50))
                                      .background(Color.White.copy(alpha = 0.96f)))
                        }
                      }
                }
          }
        }
  }
}

/** Sub-card with rounded border (looks like the inner rounded section in the mock) */
@Composable
fun SectionBox(
    title: String? = null,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
  val cs = MaterialTheme.colorScheme
  GlassSurface(shape = RoundedCornerShape(16.dp)) {
    when {
      header != null -> header()
      title != null ->
          Text(
              title,
              style =
                  MaterialTheme.typography.titleMedium.copy(
                      fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface),
              modifier = Modifier.padding(bottom = 8.dp))
    }
    Column(content = content)
  }
}
