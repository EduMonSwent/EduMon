package com.android.sample.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.theme.EduMonTheme
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/* ===================== DATA ===================== */

data class StudyStats(
    val totalTimeMin: Int = 0,
    val courseTimesMin: Map<String, Int> = emptyMap(),
    val completedGoals: Int = 0,
    val progressByDayMin: List<Int> = List(7) { 0 },
    val weeklyGoalMin: Int = 300
)

fun encouragement(stats: StudyStats): String {
  val ratio =
      if (stats.weeklyGoalMin == 0) 0f else stats.totalTimeMin.toFloat() / stats.weeklyGoalMin
  return when {
    ratio >= 1f -> "Objectif atteint 💪 Continue sur ta lancée !"
    ratio >= 0.75f -> "Tu y es presque ! Un dernier effort 🔥"
    ratio >= 0.5f -> "Belle progression, continue comme ça "
    stats.totalTimeMin > 0 -> "Bien joué, chaque minute compte 🙌"
    else -> "C’est parti ! Quelques minutes aujourd’hui et tu seras lancé 🚀"
  }
}

/* ===================== FAKE REPO (SCÉNARIOS) ===================== */

class FakeStatsRepository {
  private val scenarios: List<Pair<String, StudyStats>> =
      listOf(
          "Début de semaine" to
              StudyStats(
                  totalTimeMin = 0,
                  courseTimesMin =
                      linkedMapOf(
                          "Analyse I" to 0,
                          "Algèbre linéaire" to 0,
                          "Physique mécanique" to 0,
                          "AICC I" to 0),
                  completedGoals = 0,
                  progressByDayMin = listOf(0, 0, 0, 0, 0, 0, 0),
                  weeklyGoalMin = 300),
          "Semaine active" to
              StudyStats(
                  totalTimeMin = 145,
                  courseTimesMin =
                      linkedMapOf(
                          "Analyse I" to 60,
                          "Algèbre linéaire" to 45,
                          "Physique mécanique" to 25,
                          "AICC I" to 15),
                  completedGoals = 2,
                  progressByDayMin = listOf(0, 25, 30, 15, 50, 20, 5),
                  weeklyGoalMin = 300),
          "Objectif presque atteint" to
              StudyStats(
                  totalTimeMin = 235,
                  courseTimesMin =
                      linkedMapOf(
                          "Analyse I" to 80,
                          "Algèbre linéaire" to 70,
                          "Physique mécanique" to 40,
                          "AICC I" to 45),
                  completedGoals = 5,
                  progressByDayMin = listOf(20, 30, 45, 35, 50, 40, 15),
                  weeklyGoalMin = 300),
          "Objectif atteint" to
              StudyStats(
                  totalTimeMin = 320,
                  courseTimesMin =
                      linkedMapOf(
                          "Analyse I" to 110,
                          "Algèbre linéaire" to 95,
                          "Physique mécanique" to 60,
                          "AICC I" to 55),
                  completedGoals = 7,
                  progressByDayMin = listOf(40, 60, 55, 50, 45, 40, 30),
                  weeklyGoalMin = 300),
          "Full algèbre" to
              StudyStats(
                  totalTimeMin = 180,
                  courseTimesMin =
                      linkedMapOf(
                          "Analyse I" to 20,
                          "Algèbre linéaire" to 130,
                          "Physique mécanique" to 15,
                          "AICC I" to 15),
                  completedGoals = 3,
                  progressByDayMin = listOf(10, 25, 15, 60, 30, 20, 20),
                  weeklyGoalMin = 300))

  private val _stats = MutableStateFlow(scenarios.first().second)
  val stats: StateFlow<StudyStats> = _stats

  private val _selectedIndex = MutableStateFlow(0)
  val selectedIndex: StateFlow<Int> = _selectedIndex

  val titles: List<String>
    get() = scenarios.map { it.first }

  fun loadScenario(index: Int) {
    val i = index.coerceIn(0, scenarios.lastIndex)
    _selectedIndex.value = i
    _stats.value = scenarios[i].second
  }
}

/* ===================== VIEWMODEL ===================== */

class StatsViewModel(private val fakeRepo: FakeStatsRepository = FakeStatsRepository()) :
    ViewModel() {

  private val _stats = MutableStateFlow<StudyStats?>(fakeRepo.stats.value)
  val stats: StateFlow<StudyStats?> = _stats

  val scenarioTitles
    get() = fakeRepo.titles

  val scenarioIndex
    get() = fakeRepo.selectedIndex

  fun selectScenario(i: Int) = fakeRepo.loadScenario(i)

  init {
    viewModelScope.launch { fakeRepo.stats.collect { _stats.value = it } }
  }

  /** Optionnel : branche un flux Firestore (StateFlow<StudyStats?>) */
  fun attachFirestore(flow: StateFlow<StudyStats?>) {
    viewModelScope.launch { flow.collect { it?.let { s -> _stats.value = s } } }
  }
}

/* ===================== UI ===================== */

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {
  EduMonTheme {
    val stats by viewModel.stats.collectAsState()
    val selected by viewModel.scenarioIndex.collectAsState()
    val titles = viewModel.scenarioTitles

    val bg = MaterialTheme.colorScheme.background
    val onSurf = MaterialTheme.colorScheme.onSurface
    val cardBg = MaterialTheme.colorScheme.surface
    val scroll = rememberScrollState()

    if (stats == null) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
      return@EduMonTheme
    }
    val s = stats!!

    val colorMap = remember(s.courseTimesMin.keys.toList()) { buildColorMap(s.courseTimesMin) }

    Column(
        modifier = Modifier.fillMaxSize().background(bg).verticalScroll(scroll).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              "Tes statistiques de la semaine",
              style = MaterialTheme.typography.titleLarge,
              color = onSurf,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag(StatsTestTags.Title))

          Spacer(Modifier.height(8.dp))

          ScenarioSelector(
              titles = titles,
              selectedIndex = selected,
              onSelect = { viewModel.selectScenario(it) })

          Spacer(Modifier.height(12.dp))

          SummaryRow(
              totalTimeMin = s.totalTimeMin,
              completedGoals = s.completedGoals,
              weeklyGoalMin = s.weeklyGoalMin)

          Spacer(Modifier.height(16.dp))

          Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.background(cardBg).padding(16.dp)) {
              Text("Répartition par cours", fontWeight = FontWeight.SemiBold, color = onSurf)
              Spacer(Modifier.height(12.dp))
              PieChart(
                  data = s.courseTimesMin,
                  colors = colorMap,
                  modifier = Modifier.fillMaxWidth().height(220.dp).testTag(StatsTestTags.Donut))
              Spacer(Modifier.height(8.dp))
              Legend(
                  data = s.courseTimesMin,
                  colors = colorMap,
                  modifier = Modifier.testTag(StatsTestTags.Legend))
            }
          }

          Spacer(Modifier.height(16.dp))

          Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.background(cardBg).padding(16.dp)) {
              Text("Progression sur 7 jours", fontWeight = FontWeight.SemiBold, color = onSurf)
              Spacer(Modifier.height(12.dp))
              BarChart7Days(
                  values = s.progressByDayMin,
                  modifier = Modifier.fillMaxWidth().testTag(StatsTestTags.Bars),
                  barColor = MaterialTheme.colorScheme.primary,
                  gridColor = onSurf.copy(alpha = 0.08f),
                  axisColor = onSurf.copy(alpha = 0.2f),
                  unitLabel = "m",
                  perDayGoal = (s.weeklyGoalMin / 7))
            }
          }

          Spacer(Modifier.height(16.dp))
          EncouragementCard(
              text = encouragement(s),
              accent = MaterialTheme.colorScheme.primary,
              modifier = Modifier.testTag(StatsTestTags.Encouragement))
        }
  }
}

/* ===================== COMPOSABLES ===================== */

@Composable
private fun ScenarioSelector(titles: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
  val rowScroll = rememberScrollState()
  Row(
      modifier =
          Modifier.fillMaxWidth().horizontalScroll(rowScroll).testTag(StatsTestTags.Scenarios),
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
        title = "Total d’étude",
        value = formatMinutes(totalTimeMin),
        modifier = Modifier.weight(1f).testTag(StatsTestTags.SummaryTotal))
    SummaryCard(
        title = "Objectifs faits",
        value = completedGoals.toString(),
        modifier = Modifier.weight(1f).testTag(StatsTestTags.SummaryGoals))
    SummaryCard(
        title = "Objectif semaine",
        value = formatMinutes(weeklyGoalMin),
        modifier = Modifier.weight(1f).testTag(StatsTestTags.SummaryWeekly))
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

/* ---- Couleurs stables donut + légende ---- */

private val SliceColors =
    listOf(
        Color(0xFF8B5CF6),
        Color(0xFF22C55E),
        Color(0xFFF59E0B),
        Color(0xFF3B82F6),
        Color(0xFFE11D48),
        Color(0xFF06B6D4))

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
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(10.dp)
                    .background(colors[label] ?: Color.LightGray, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(8.dp))
            Text(
                "$label — $pct%",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
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
    unitLabel: String = "min",
    perDayGoal: Int? = null
) {
  val maxValRaw = (values.maxOrNull() ?: 0).coerceAtLeast(1)
  val step = niceStep(maxValRaw)
  val yMax = ((maxValRaw + step - 1) / step) * step

  val labelsX = listOf("J-6", "J-5", "J-4", "J-3", "J-2", "J-1", "Aujourd’hui")
  val todayIndex = values.lastIndex

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

      perDayGoal?.let { g ->
        val gy = origin.y - (g.coerceAtLeast(0) / yMax.toFloat()) * chartH
        drawLine(
            color = goalColor,
            start = Offset(leftPad, gy),
            end = Offset(size.width, gy),
            strokeWidth = 2.dp.toPx())
        drawContext.canvas.nativeCanvas.drawText(
            "Objectif/jour: ${g}$unitLabel",
            size.width - 140.dp.toPx(),
            gy - 4.dp.toPx(),
            textPaint)
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
        "Minutes d’étude par jour (7 derniers jours)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
  }
}

@Composable
private fun EncouragementCard(text: String, accent: Color, modifier: Modifier = Modifier) {
  Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(12.dp).background(accent, RoundedCornerShape(6.dp)))
          Spacer(Modifier.width(12.dp))
          Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
  }
}

/* ===================== UTILS ===================== */

private fun formatMinutes(totalMin: Int): String {
  val h = totalTimeHours(totalMin)
  val m = totalMin % 60
  return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun totalTimeHours(min: Int) = min / 60

object StatsTestTags {
  const val Title = "StatsTitle"
  const val SummaryTotal = "SummaryTotal"
  const val SummaryGoals = "SummaryGoals"
  const val SummaryWeekly = "SummaryWeekly"
  const val Donut = "DonutChart"
  const val Legend = "Legend"
  const val Bars = "BarChart7D"
  const val Encouragement = "Encouragement"
  const val Scenarios = "ScenarioRow"
}
