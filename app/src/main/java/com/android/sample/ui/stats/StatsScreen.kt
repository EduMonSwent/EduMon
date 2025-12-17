package com.android.sample.ui.stats

// This code has been written partially using A.I (LLM).

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.ui.stats.model.StudyStats
import com.android.sample.ui.stats.viewmodel.StatsViewModel
import com.android.sample.ui.theme.AccentBlue
import com.android.sample.ui.theme.AccentMagenta
import com.android.sample.ui.theme.AccentMint
import com.android.sample.ui.theme.EventColorDefault
import com.android.sample.ui.theme.EventViolet
import com.android.sample.ui.theme.StudyGreen
import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.roundToInt

private const val DAYS_IN_WEEK = 7
private const val GRID_ALPHA = 0.08f
private const val AXIS_ALPHA = 0.2f
private const val SECONDARY_TEXT_ALPHA = 0.8f
private val DEFAULT_BAR_SPACING = 8.dp

// --- Route: ViewModel wiring -------------------------------------------------

@Composable
fun StatsRoute(viewModel: StatsViewModel = viewModel()) {
  val stats by viewModel.stats.collectAsState()
  val userStats by viewModel.userStats.collectAsState()
  val selected by viewModel.scenarioIndex.collectAsState()
  val titles = viewModel.scenarioTitles

  val bg = MaterialTheme.colorScheme.background

  if (stats == null) {
    Box(Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  StatsScreen(
      stats = stats!!,
      totalStudyMinutes = userStats.totalStudyMinutes,
      selectedIndex = selected,
      titles = titles,
      onSelectScenario = viewModel::selectScenario,
  )
}

// --- Main screen -------------------------------------------------------------

@Composable
fun StatsScreen(
    stats: StudyStats,
    totalStudyMinutes: Int,
    selectedIndex: Int,
    titles: List<String>,
    onSelectScenario: (Int) -> Unit,
) {
  val onSurf = MaterialTheme.colorScheme.onSurface
  val cardBg = MaterialTheme.colorScheme.surface
  val scroll = rememberScrollState()

  val colorMap =
      remember(stats.courseTimesMin.keys.toList()) { buildColorMap(stats.courseTimesMin) }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .verticalScroll(scroll)
              .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.stats_title_week),
            style = MaterialTheme.typography.titleLarge,
            color = onSurf,
            fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(8.dp))

        if (titles.size > 1) {
          ScenarioSelector(
              titles = titles, selectedIndex = selectedIndex, onSelect = onSelectScenario)
        }

        Spacer(Modifier.height(12.dp))

        SummaryRow(
            totalTimeMin = totalStudyMinutes,
            completedGoals = stats.completedGoals,
            weeklyGoalMin = stats.weeklyGoalMin)

        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)) {
              Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.stats_section_subject_distribution),
                    fontWeight = FontWeight.SemiBold,
                    color = onSurf)

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.stats_subjects_this_week_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY_TEXT_ALPHA),
                )

                Spacer(Modifier.height(12.dp))

                PieChart(
                    data = stats.courseTimesMin,
                    colors = colorMap,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
                Spacer(Modifier.height(8.dp))
                Legend(data = stats.courseTimesMin, colors = colorMap)
              }
            }

        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)) {
              Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.stats_section_progress_7_days),
                    fontWeight = FontWeight.SemiBold,
                    color = onSurf)
                Spacer(Modifier.height(12.dp))
                BarChart7Days(
                    values = stats.progressByDayMin,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        BarColors(
                            bar = MaterialTheme.colorScheme.primary,
                            grid = onSurf.copy(alpha = GRID_ALPHA),
                            axis = onSurf.copy(alpha = AXIS_ALPHA)),
                    unitLabel = stringResource(R.string.stats_unit_minutes_short),
                    perDayGoal = stats.weeklyGoalMin / DAYS_IN_WEEK,
                    todayIndex = LocalDate.now().dayOfWeek.value - 1,
                )
              }
            }

        Spacer(Modifier.height(16.dp))
        EncouragementCard(
            text = encouragementText(stats), accent = MaterialTheme.colorScheme.primary)
      }
}

// --- Header / summary helpers ------------------------------------------------

@Composable
private fun ScenarioSelector(titles: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
  val rowScroll = rememberScrollState()
  Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rowScroll),
      horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        titles.forEachIndexed { i, label ->
          val selected = i == selectedIndex
          val colors =
              if (selected)
                  ButtonDefaults.filledTonalButtonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary)
              else ButtonDefaults.filledTonalButtonColors()
          FilledTonalButton(
              onClick = { onSelect(i) },
              colors = colors,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Text(label)
              }
        }
      }
}

@Composable
private fun SummaryRow(totalTimeMin: Int, completedGoals: Int, weeklyGoalMin: Int) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    SummaryCard(
        title = stringResource(R.string.stats_summary_total_study),
        value = formatMinutesLabel(totalTimeMin),
        modifier = Modifier.weight(1f))
    SummaryCard(
        title = stringResource(R.string.stats_summary_completed_goals),
        value = completedGoals.toString(),
        modifier = Modifier.weight(1f))
    SummaryCard(
        title = stringResource(R.string.stats_summary_weekly_goal),
        value = formatMinutesLabel(weeklyGoalMin),
        modifier = Modifier.weight(1f))
  }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
  Card(modifier, shape = RoundedCornerShape(16.dp)) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(14.dp)) {
      Text(
          title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY_TEXT_ALPHA))
      Spacer(Modifier.height(6.dp))
      Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
  }
}

// --- Colors for pie chart (Edumon theme colors) ------------------------------

private val SliceColors =
    listOf(
        EventViolet,
        AccentMint,
        AccentBlue,
        AccentMagenta,
        StudyGreen,
        EventColorDefault,
    )

private fun buildColorMap(data: Map<String, Int>): Map<String, Color> {
  val map = LinkedHashMap<String, Color>()
  data.keys.forEachIndexed { idx, label -> map[label] = SliceColors[idx % SliceColors.size] }
  return map
}

// --- Pie chart ---------------------------------------------------------------

@Composable
private fun PieChart(
    data: Map<String, Int>,
    colors: Map<String, Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 28.dp
) {
  val isInspectionMode = LocalInspectionMode.current
  val total = data.values.sum().coerceAtLeast(1)
  val sweepFractions = data.values.map { it.toFloat() / total }

  // Disable animations during test/inspection mode to prevent compose from never being idle
  val animatedFractions =
      if (!isInspectionMode) {
        sweepFractions.map { animateFloatAsState(targetValue = it, label = "") }
      } else {
        sweepFractions.map { remember { mutableStateOf(it) } }
      }

  Canvas(modifier = modifier) {
    val diameter = size.minDimension
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val chartSize = Size(diameter, diameter)
    var startAngle = -90f
    data.entries.forEachIndexed { index, (label, _) ->
      val sweep = animatedFractions[index].value * 360f
      drawArc(
          color = colors[label] ?: SliceColors[index % SliceColors.size],
          startAngle = startAngle,
          sweepAngle = sweep,
          useCenter = false,
          topLeft = topLeft,
          size = chartSize,
          style = Stroke(width = strokeWidth.toPx()))
      startAngle += sweep
    }
  }
}

@Composable
private fun Legend(data: Map<String, Int>, colors: Map<String, Color>) {
  val total = data.values.sum().coerceAtLeast(1)
  val fallbackColor = MaterialTheme.colorScheme.outlineVariant

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    data.entries
        .sortedByDescending { it.value }
        .forEach { (label, value) ->
          val pct = (value * 100f / total).roundToInt()
          val formattedTime = formatMinutesLabel(value)
          val line =
              stringResource(R.string.stats_legend_entry_with_time, label, pct, formattedTime)

          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(10.dp)
                    .background(colors[label] ?: fallbackColor, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(8.dp))
            Text(
                line,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
          }
        }
  }
}

// --- Bar chart utilities -----------------------------------------------------

private fun niceStep(maxVal: Int, ticks: Int = 4): Int {
  if (maxVal <= 0) return 10
  val raw = maxVal / ticks.toFloat()
  val pow = kotlin.math.floor(kotlin.math.log10(raw)).toInt()
  val base = raw / 10f.pow(pow)
  val niceBase =
      when {
        base <= 1f -> 1f
        base <= 2f -> 2f
        base <= 5f -> 5f
        else -> 10f
      }
  return (niceBase * 10f.pow(pow)).toInt().coerceAtLeast(1)
}

@Composable
private fun BarChart7Days(
    values: List<Int>,
    modifier: Modifier = Modifier,
    colors: BarColors,
    unitLabel: String,
    perDayGoal: Int? = null,
    barSpacing: Dp = DEFAULT_BAR_SPACING,
    todayIndex: Int,
) {
  val maxValRaw = (values.maxOrNull() ?: 0).coerceAtLeast(1)
  val step = niceStep(maxValRaw)
  val yMax = ((maxValRaw + step - 1) / step) * step

  val labelsX =
      listOf(
          stringResource(R.string.stats_label_day_mon),
          stringResource(R.string.stats_label_day_tue),
          stringResource(R.string.stats_label_day_wed),
          stringResource(R.string.stats_label_day_thu),
          stringResource(R.string.stats_label_day_fri),
          stringResource(R.string.stats_label_day_sat),
          stringResource(R.string.stats_label_day_sun),
      )

  val clampedTodayIndex = if (values.isEmpty()) 0 else todayIndex.coerceIn(0, values.lastIndex)

  val goalColor = MaterialTheme.colorScheme.tertiary
  val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

  val goalLabel: String? =
      perDayGoal?.let { stringResource(R.string.stats_goal_per_day_format, it, unitLabel) }

  Column(modifier = modifier) {
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
      val layout = createBarChartLayout(labelColor)

      drawYAxisGrid(
          yMax = yMax,
          step = step,
          gridColor = colors.grid,
          layout = layout,
      )

      drawBarsForValues(
          BarDrawingParams(
              values = values,
              yMax = yMax,
              barSpacing = barSpacing,
              unitLabel = unitLabel,
              todayIndex = clampedTodayIndex,
              colors = colors,
              layout = layout,
          ))

      if (perDayGoal != null && goalLabel != null) {
        drawGoalLine(
            perDayGoal = perDayGoal,
            yMax = yMax,
            goalColor = goalColor,
            goalLabel = goalLabel,
            layout = layout,
        )
      }

      drawBottomAxis(axisColor = colors.axis, layout = layout)
    }

    BarChartDayLabels(labelsX = labelsX, todayIndex = clampedTodayIndex)
  }
}

// layout + parameter holders

private data class BarChartLayout(
    val leftPad: Float,
    val bottomPad: Float,
    val topPad: Float,
    val chartWidth: Float,
    val chartHeight: Float,
    val origin: Offset,
    val textPaint: android.graphics.Paint,
)

private data class BarColors(
    val bar: Color,
    val grid: Color,
    val axis: Color,
)

private data class BarDrawingParams(
    val values: List<Int>,
    val yMax: Int,
    val barSpacing: Dp,
    val unitLabel: String,
    val todayIndex: Int,
    val colors: BarColors,
    val layout: BarChartLayout,
)

// --- DrawScope helpers -------------------------------------------------------

private fun DrawScope.createBarChartLayout(labelColor: Color): BarChartLayout {
  val leftPad = 36.dp.toPx()
  val bottomPad = 18.dp.toPx()
  val topPad = 8.dp.toPx()

  val chartWidth = size.width - leftPad
  val chartHeight = size.height - bottomPad - topPad
  val origin = Offset(leftPad, size.height - bottomPad)

  val paint =
      android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 11.dp.toPx()
        color = labelColor.toArgb()
      }

  return BarChartLayout(
      leftPad = leftPad,
      bottomPad = bottomPad,
      topPad = topPad,
      chartWidth = chartWidth,
      chartHeight = chartHeight,
      origin = origin,
      textPaint = paint,
  )
}

private fun DrawScope.drawYAxisGrid(
    yMax: Int,
    step: Int,
    gridColor: Color,
    layout: BarChartLayout,
) {
  val tickCount = yMax / step
  val leftPad = layout.leftPad
  val origin = layout.origin
  val chartHeight = layout.chartHeight

  for (i in 0..tickCount) {
    val yVal = i * step
    val y = origin.y - (yVal / yMax.toFloat()) * chartHeight

    drawLine(
        color = gridColor,
        start = Offset(leftPad, y),
        end = Offset(size.width, y),
        strokeWidth = 1.dp.toPx(),
    )

    drawContext.canvas.nativeCanvas.drawText(
        "$yVal",
        6.dp.toPx(),
        y + 4.dp.toPx(),
        layout.textPaint,
    )
  }
}

private fun DrawScope.drawBarsForValues(params: BarDrawingParams) {
  val values = params.values
  val yMax = params.yMax
  val spacingPx = params.barSpacing.toPx()
  val barColor = params.colors.bar
  val gridColor = params.colors.grid
  val axisColor = params.colors.axis
  val unitLabel = params.unitLabel
  val todayIndex = params.todayIndex
  val layout = params.layout

  val n = values.size
  if (n == 0) return

  val barWidth = (layout.chartWidth - (spacingPx * (n + 1))) / n

  values.forEachIndexed { index, value ->
    val safeValue = value.coerceAtLeast(0)
    val barHeight = (safeValue / yMax.toFloat()) * layout.chartHeight
    val left = layout.leftPad + spacingPx + index * (barWidth + spacingPx)
    val top = layout.origin.y - barHeight

    drawRect(
        color = gridColor,
        topLeft = Offset(left, layout.origin.y - layout.chartHeight),
        size = Size(barWidth, layout.chartHeight),
    )

    drawRect(
        color = barColor,
        topLeft = Offset(left, top),
        size = Size(barWidth, barHeight),
    )

    if (index == todayIndex) {
      drawRect(
          color = axisColor,
          topLeft = Offset(left, layout.origin.y - layout.chartHeight),
          size = Size(barWidth, layout.chartHeight),
          style = Stroke(width = 1.5.dp.toPx()),
      )
    }

    if (safeValue > 0) {
      val labelX = left + barWidth / 2 - 12.dp.toPx()
      val labelY = top - 4.dp.toPx()
      drawContext.canvas.nativeCanvas.drawText(
          "${safeValue}$unitLabel",
          labelX,
          labelY,
          layout.textPaint,
      )
    }
  }
}

private fun DrawScope.drawGoalLine(
    perDayGoal: Int,
    yMax: Int,
    goalColor: Color,
    goalLabel: String,
    layout: BarChartLayout,
) {
  val origin = layout.origin
  val chartHeight = layout.chartHeight
  val leftPad = layout.leftPad

  val clampedGoal = perDayGoal.coerceAtLeast(0)
  val gy = origin.y - (clampedGoal / yMax.toFloat()) * chartHeight

  drawLine(
      color = goalColor,
      start = Offset(leftPad, gy),
      end = Offset(size.width, gy),
      strokeWidth = 2.dp.toPx(),
  )

  drawContext.canvas.nativeCanvas.drawText(
      goalLabel,
      size.width - 140.dp.toPx(),
      gy - 4.dp.toPx(),
      layout.textPaint,
  )
}

private fun DrawScope.drawBottomAxis(axisColor: Color, layout: BarChartLayout) {
  drawLine(
      color = axisColor,
      start = Offset(layout.leftPad, layout.origin.y),
      end = Offset(size.width, layout.origin.y),
      strokeWidth = 1.dp.toPx(),
  )
}

@Composable
private fun BarChartDayLabels(labelsX: List<String>, todayIndex: Int) {
  Spacer(Modifier.height(6.dp))
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    labelsX.forEachIndexed { i, l ->
      val tone = if (i == todayIndex) 1f else 0.7f
      Text(
          l,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = tone))
    }
  }

  Spacer(Modifier.height(4.dp))
  Text(
      text = stringResource(R.string.stats_bar_chart_caption),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY_TEXT_ALPHA))
}

// --- Encouragement + formatting ----------------------------------------------

@Composable
private fun encouragementText(stats: StudyStats): String {
  val ratio =
      if (stats.weeklyGoalMin == 0) 0f else stats.totalTimeMin.toFloat() / stats.weeklyGoalMin

  val resId =
      when {
        ratio >= 1f -> R.string.stats_encouragement_goal_reached
        ratio >= 0.75f -> R.string.stats_encouragement_almost_there
        ratio >= 0.5f -> R.string.stats_encouragement_keep_going
        stats.totalTimeMin > 0 -> R.string.stats_encouragement_good_start
        else -> R.string.stats_encouragement_lets_start
      }
  return stringResource(resId)
}

@Composable
private fun formatMinutesLabel(totalMin: Int): String {
  val minutesLabel = stringResource(R.string.stats_unit_minutes_short)
  val hoursLabel = stringResource(R.string.stats_unit_hours_short)
  val h = totalTimeHours(totalMin)
  val m = totalMin % 60
  return if (h > 0) "${h}$hoursLabel ${m}$minutesLabel" else "${m}$minutesLabel"
}

@Composable
private fun EncouragementCard(text: String, accent: Color) {
  Card(shape = RoundedCornerShape(16.dp)) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(12.dp).background(accent, RoundedCornerShape(6.dp)))
          Spacer(Modifier.width(12.dp))
          Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
  }
}

private fun totalTimeHours(min: Int) = min / 60
