package com.android.sample.ui.stats

// This code has been written partially using A.I (LLM).

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.ui.stats.model.StudyStats
import com.android.sample.ui.stats.viewmodel.StatsViewModel
import kotlin.math.pow
import kotlin.math.roundToInt

// --- Route: wires ViewModel and passes data to the pure UI ---
@Composable
fun StatsRoute(viewModel: StatsViewModel = viewModel()) {
  val stats by viewModel.stats.collectAsState()
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
      selectedIndex = selected,
      titles = titles,
      onSelectScenario = viewModel::selectScenario,
  )
}

// --- Pure UI screen: stateless except for local UI animations ---
@Composable
fun StatsScreen(
    stats: StudyStats,
    selectedIndex: Int,
    titles: List<String>,
    onSelectScenario: (Int) -> Unit,
) {
  val onSurf = MaterialTheme.colorScheme.onSurface
  val cardBg = MaterialTheme.colorScheme.surface
  val scroll = rememberScrollState()

  // Stable "label -> color" mapping shared by donut + legend
  val colorMap =
      remember(stats.courseTimesMin.keys.toList()) { buildColorMap(stats.courseTimesMin) }

  val unitLabelShort = stringResource(R.string.stats_unit_minutes_short)

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

        ScenarioSelector(
            titles = titles, selectedIndex = selectedIndex, onSelect = onSelectScenario)

        Spacer(Modifier.height(12.dp))

        SummaryRow(
            totalTimeMin = stats.totalTimeMin,
            completedGoals = stats.completedGoals,
            weeklyGoalMin = stats.weeklyGoalMin)

        Spacer(Modifier.height(16.dp))

        // --- Time per subject card: donut on the left, legend on the right ---
        Card(shape = RoundedCornerShape(16.dp)) {
          Column(Modifier.background(cardBg).padding(16.dp)) {
            Text(
                text = stringResource(R.string.stats_section_subject_distribution),
                fontWeight = FontWeight.SemiBold,
                color = onSurf)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  PieChart(
                      data = stats.courseTimesMin,
                      colors = colorMap,
                      modifier = Modifier.weight(1f).aspectRatio(1f).padding(end = 16.dp),
                  )

                  Legend(
                      data = stats.courseTimesMin,
                      colors = colorMap,
                      modifier = Modifier.weight(1f))
                }
          }
        }

        Spacer(Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(16.dp)) {
          Column(Modifier.background(cardBg).padding(16.dp)) {
            Text(
                text = stringResource(R.string.stats_section_progress_7_days),
                fontWeight = FontWeight.SemiBold,
                color = onSurf)
            Spacer(Modifier.height(12.dp))
            BarChart7Days(
                values = stats.progressByDayMin,
                modifier = Modifier.fillMaxWidth(),
                barColor = MaterialTheme.colorScheme.primary,
                gridColor = onSurf.copy(alpha = 0.08f),
                axisColor = onSurf.copy(alpha = 0.2f),
                unitLabel = unitLabelShort,
                perDayGoal = (stats.weeklyGoalMin / 7))
          }
        }

        Spacer(Modifier.height(16.dp))
        EncouragementCard(stats = stats, accent = MaterialTheme.colorScheme.primary)
      }
}

// --- UI helpers below ---

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
        value = formatMinutes(totalTimeMin),
        modifier = Modifier.weight(1f))
    SummaryCard(
        title = stringResource(R.string.stats_summary_completed_goals),
        value = completedGoals.toString(),
        modifier = Modifier.weight(1f))
    SummaryCard(
        title = stringResource(R.string.stats_summary_weekly_goal),
        value = formatMinutes(weeklyGoalMin),
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
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
      Spacer(Modifier.height(6.dp))
      Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
  }
}

// Existing palette
private val SliceColors =
    listOf(
        Color(0xFF8B5CF6),
        Color(0xFF22C55E),
        Color(0xFFF59E0B),
        Color(0xFF3B82F6),
        Color(0xFFE11D48),
        Color(0xFF06B6D4),
    )

private fun buildColorMap(data: Map<String, Int>): Map<String, Color> {
  val map = LinkedHashMap<String, Color>()
  data.keys.forEachIndexed { idx, label -> map[label] = SliceColors[idx % SliceColors.size] }
  return map
}

@Composable
private fun PieChart(
    data: Map<String, Int>,
    colors: Map<String, Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 28.dp
) {
  val total = data.values.sum().coerceAtLeast(1)
  val sweepFractions = data.values.map { it.toFloat() / total }
  val animatedFractions = sweepFractions.map { animateFloatAsState(targetValue = it, label = "") }
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

/**
 * Legend shows: label + percentage + total time per subject. Example: "Algorithms 1 — 60% (1h
 * 15m)". Text now wraps instead of being truncated.
 */
@Composable
private fun Legend(
    data: Map<String, Int>,
    colors: Map<String, Color>,
    modifier: Modifier = Modifier
) {
  val total = data.values.sum().coerceAtLeast(1)
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    data.entries
        .sortedByDescending { it.value }
        .forEach { (label, value) ->
          val pct = (value * 100f / total).roundToInt()
          val timeFormatted = formatMinutes(value)
          val line =
              stringResource(id = R.string.stats_legend_entry_with_time, label, pct, timeFormatted)

          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(10.dp)
                    .background(colors[label] ?: Color.LightGray, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(8.dp))
            Text(
                line,
                style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
  }
}

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
    barSpacing: Dp = 8.dp,
    barColor: Color,
    gridColor: Color,
    axisColor: Color,
    unitLabel: String,
    perDayGoal: Int? = null
) {
  val maxValRaw = (values.maxOrNull() ?: 0).coerceAtLeast(1)
  val step = niceStep(maxValRaw)
  val yMax = ((maxValRaw + step - 1) / step) * step

  val labelsX =
      listOf(
          stringResource(R.string.stats_day_6_days_ago),
          stringResource(R.string.stats_day_5_days_ago),
          stringResource(R.string.stats_day_4_days_ago),
          stringResource(R.string.stats_day_3_days_ago),
          stringResource(R.string.stats_day_2_days_ago),
          stringResource(R.string.stats_day_1_day_ago),
          stringResource(R.string.stats_day_today),
      )
  val todayIndex = values.lastIndex

  // Precompute goal label text (must be outside Canvas)
  val goalText: String? =
      perDayGoal?.let { g -> stringResource(R.string.stats_goal_per_day_format, g, unitLabel) }

  Column(modifier = modifier) {
    val goalColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
      val leftPad = 36.dp.toPx()
      val bottomPad = 18.dp.toPx()
      val topPad = 8.dp.toPx()

      val chartW = size.width - leftPad
      val chartH = size.height - bottomPad - topPad
      val origin = Offset(leftPad, size.height - bottomPad)

      val textPaint =
          android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 11.dp.toPx()
            color = android.graphics.Color.argb(180, 220, 225, 235)
          }
      val tickCount = (yMax / step)
      for (i in 0..tickCount) {
        val yVal = i * step
        val y = origin.y - (yVal / yMax.toFloat()) * chartH
        drawLine(
            color = gridColor,
            start = Offset(leftPad, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx())

        drawContext.canvas.nativeCanvas.drawText("$yVal", 6.dp.toPx(), y + 4.dp.toPx(), textPaint)
      }

      val n = values.size
      val spacingPx = barSpacing.toPx()
      val barWidth = (chartW - (spacingPx * (n + 1))) / n
      values.forEachIndexed { i, v ->
        val h = (v.coerceAtLeast(0) / yMax.toFloat()) * chartH
        val left = leftPad + spacingPx + i * (barWidth + spacingPx)
        val top = origin.y - h

        drawRect(
            color = gridColor,
            topLeft = Offset(left, origin.y - chartH),
            size = Size(barWidth, chartH))

        drawRect(color = barColor, topLeft = Offset(left, top), size = Size(barWidth, h))

        if (i == todayIndex) {
          drawRect(
              color = axisColor,
              topLeft = Offset(left, origin.y - chartH),
              size = Size(barWidth, chartH),
              style = Stroke(width = 1.5.dp.toPx()))
        }

        if (v > 0) {
          drawContext.canvas.nativeCanvas.drawText(
              "${v}$unitLabel", left + barWidth / 2 - 12.dp.toPx(), top - 4.dp.toPx(), textPaint)
        }
      }

      if (perDayGoal != null && goalText != null) {
        val g = perDayGoal
        val gy = origin.y - (g.coerceAtLeast(0) / yMax.toFloat()) * chartH
        drawLine(
            color = goalColor,
            start = Offset(leftPad, gy),
            end = Offset(size.width, gy),
            strokeWidth = 2.dp.toPx())
        drawContext.canvas.nativeCanvas.drawText(
            goalText, size.width - 140.dp.toPx(), gy - 4.dp.toPx(), textPaint)
      }

      drawLine(
          color = axisColor,
          start = Offset(leftPad, origin.y),
          end = Offset(size.width, origin.y),
          strokeWidth = 1.dp.toPx())
    }

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
        text = stringResource(R.string.stats_footer_7days),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
  }
}

@Composable
private fun EncouragementCard(stats: StudyStats, accent: Color) {
  val ratio =
      if (stats.weeklyGoalMin == 0) 0f else stats.totalTimeMin.toFloat() / stats.weeklyGoalMin

  val textId =
      when {
        ratio >= 1f -> R.string.stats_encouragement_goal_reached
        ratio >= 0.75f -> R.string.stats_encouragement_almost_there
        ratio >= 0.5f -> R.string.stats_encouragement_keep_going
        stats.totalTimeMin > 0 -> R.string.stats_encouragement_good_start
        else -> R.string.stats_encouragement_lets_start
      }

  val text = stringResource(textId)

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

private fun formatMinutes(totalMin: Int): String {
  val h = totalTimeHours(totalMin)
  val m = totalMin % 60
  return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun totalTimeHours(min: Int) = min / 60
