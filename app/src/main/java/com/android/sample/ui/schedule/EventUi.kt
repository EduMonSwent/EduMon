package com.android.sample.ui.schedule

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.android.sample.R
import com.android.sample.model.schedule.EventKind

// Replace with your theme colors as needed
object EventUi {
  data class Style(@DrawableRes val icon: Int, val color: Color)

  // Default icon/color if not specified
  private val defaultStyle = Style(R.drawable.ic_event, Color(0xFF9E9E9E))

  private val map: Map<EventKind, Style> =
      mapOf(
          EventKind.CLASS_LECTURE to Style(R.drawable.ic_event, Color(0xFF64B5F6)),
          EventKind.CLASS_EXERCISE to Style(R.drawable.ic_event, Color(0xFF4DB6AC)),
          EventKind.CLASS_LAB to Style(R.drawable.ic_event, Color(0xFF81C784)),
          EventKind.STUDY to Style(R.drawable.ic_exercise, Color(0xFF9575CD)),
          EventKind.PROJECT to Style(R.drawable.ic_yoga, Color(0xFF7E57C2)),
          EventKind.EXAM_MIDTERM to Style(R.drawable.ic_yoga, Color(0xFFE57373)),
          EventKind.EXAM_FINAL to Style(R.drawable.ic_yoga, Color(0xFFEF5350)),
          EventKind.SUBMISSION_PROJECT to Style(R.drawable.ic_yoga, Color(0xFFFFB74D)),
          EventKind.SUBMISSION_MILESTONE to Style(R.drawable.ic_yoga, Color(0xFFFFA726)),
          EventKind.SUBMISSION_WEEKLY to Style(R.drawable.ic_yoga, Color(0xFFFFCC80)),
          EventKind.ACTIVITY_SPORT to Style(R.drawable.ic_yoga, Color(0xFF4FC3F7)),
          EventKind.ACTIVITY_ASSOCIATION to Style(R.drawable.ic_star, Color(0xFFFFD54F)),
      )

  fun styleFor(kind: EventKind): Style = map[kind] ?: defaultStyle
}
