package com.android.sample.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
  val cs = MaterialTheme.colorScheme
  val gradient =
      Brush.linearGradient(
          listOf(cs.surfaceVariant.copy(alpha = 0.20f), cs.surfaceVariant.copy(alpha = 0.10f)))
  Surface(color = Color.Transparent, shape = shape, tonalElevation = 0.dp, shadowElevation = 0.dp,
      modifier = modifier.let { m -> if (testTag != null) m.testTag(testTag) else m }) {
    Column(
        Modifier.clip(shape)
            .background(gradient)
            .border(1.dp, cs.onSurface.copy(alpha = 0.12f), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        content = content)
  }
}

