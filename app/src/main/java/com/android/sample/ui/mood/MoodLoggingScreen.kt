package com.android.sample.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.data.MoodEntry
import com.android.sample.data.MoodRepository
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// This code has been written partially using A.I (LLM).
@Composable
fun MoodLoggingRoute() {
    val context = LocalContext.current
    val repo = remember { MoodRepository(context) }

    val vm: MoodLoggingViewModel =
        viewModel(
            factory =
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return MoodLoggingViewModel(repo) as T
                    }
                })

    val state by vm.ui.collectAsState()

    MoodLoggingScreen(
        state = state,
        onSelectMood = vm::onMoodSelected,
        onNoteChanged = vm::onNoteChanged,
        onSave = vm::saveToday,
        onChartMode = vm::onChartMode)
}

@Composable
fun MoodLoggingScreen(
    state: MoodUiState,
    onSelectMood: (Int) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit,
    onChartMode: (ChartMode) -> Unit
) {
    val emojis = listOf("😞", "🙁", "😐", "🙂", "😄")
    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize().background(colors.background).padding(16.dp)) {

        // ─────────────── Mood + Note ───────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("How do you feel today?", color = colors.onSurface)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    (1..5).forEach { mood ->
                        val selected = state.selectedMood == mood
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier.size(if (selected) 56.dp else 48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) colors.primary.copy(alpha = 0.2f)
                                        else Color.Transparent)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) colors.primary else colors.outlineVariant,
                                        shape = CircleShape)
                                    .clickable(enabled = state.canEditToday) { onSelectMood(mood) }
                                    .testTag("mood_$mood")) {
                            Text(emojis[mood - 1])
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChanged,
                    enabled = state.canEditToday,
                    label = { Text("Short note (max 140 chars)", color = colors.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth().testTag("noteField"),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface,
                            focusedLabelColor = colors.primary,
                            cursorColor = colors.primary))

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSave,
                    enabled = state.canEditToday,
                    modifier = Modifier.testTag("save_button"),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.primary, contentColor = colors.onPrimary)) {
                    Text(
                        if (state.existingToday == null) "Save today’s mood"
                        else "Update today’s mood")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ─────────────── Past 7 days ───────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Past 7 days", color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    state.last7Days.forEach { e ->
                        val d = LocalDate.ofEpochDay(e.dateEpochDay)
                        val emoji =
                            when (e.mood) {
                                1 -> "😞"
                                2 -> "🙁"
                                3 -> "😐"
                                4 -> "🙂"
                                5 -> "😄"
                                else -> "—"
                            }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                color = colors.primary,
                                style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(emoji, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ─────────────── Charts ───────────────
        ChartCard(
            weekly = state.last7Days,
            monthly = state.monthEntries,
            mode = state.chartMode,
            onModeChange = onChartMode)
    }
}

@Composable
private fun ChartCard(
    weekly: List<MoodEntry>,
    monthly: List<MoodEntry>,
    mode: ChartMode,
    onModeChange: (ChartMode) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text("Mood trend", color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                ChartTabs(mode = mode, onModeChange = onModeChange)
            }
            Spacer(Modifier.height(12.dp))
            val entries = if (mode == ChartMode.WEEK) weekly else monthly
            MoodLineChart(entries = entries, modifier = Modifier.fillMaxWidth().height(180.dp))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "1 = low",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall)
                Text(
                    "5 = high",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ChartTabs(mode: ChartMode, onModeChange: (ChartMode) -> Unit) {
    val tabs = listOf(ChartMode.WEEK to "Week", ChartMode.MONTH to "Month")
    val colors = MaterialTheme.colorScheme

    Row(
        modifier =
            Modifier.clip(RoundedCornerShape(50)).background(colors.surfaceVariant).padding(2.dp)) {
        tabs.forEach { (value, title) ->
            val selected = mode == value
            Box(
                modifier =
                    Modifier.clip(RoundedCornerShape(50))
                        .background(if (selected) colors.primary else Color.Transparent)
                        .clickable { onModeChange(value) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("tab_${title.lowercase()}")) {
                Text(
                    title,
                    color = if (selected) colors.onPrimary else colors.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private const val MAX_MOOD = 5f
private const val HORIZONTAL_INSET_PX = 16f

// Keep your existing stroke/radii values (don’t change existing magic numbers),
// but add insets that are big enough to prevent clipping when y == 0 or y == height.
private const val EXTRA_TOP_INSET_PX = 8f
private const val EXTRA_BOTTOM_INSET_PX = 8f

@Composable
private fun MoodLineChart(entries: List<MoodEntry>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val stroke = 3f
    val pointRadius = 6f
    val emptyPointRadius = 4f

    val points = entries.mapIndexed { idx, e -> idx to e.mood.coerceIn(0, 5) }
    val n = points.size.coerceAtLeast(2)

    Canvas(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant)) {
        val topInset = pointRadius + stroke + EXTRA_TOP_INSET_PX
        val bottomInset = emptyPointRadius + stroke + EXTRA_BOTTOM_INSET_PX

        val drawableWidth = size.width - 2f * HORIZONTAL_INSET_PX
        val drawableHeight = (size.height - topInset - bottomInset).coerceAtLeast(1f)

        val stepX = drawableWidth / (n - 1).coerceAtLeast(1)

        // Grid lines inside drawable area (so they also don’t touch the clipped corners)
        val gridLines = MAX_MOOD.toInt()
        val stepYGrid = drawableHeight / MAX_MOOD
        repeat(gridLines) { i ->
            val y = topInset + drawableHeight - (i + 1) * stepYGrid
            drawLine(
                color = colors.outlineVariant,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f)
        }

        fun xFor(idx: Int): Float = HORIZONTAL_INSET_PX + idx * stepX

        fun yFor(mood: Int): Float {
            val normalized = mood / MAX_MOOD // 0..1
            return topInset + (1f - normalized) * drawableHeight
        }

        var lastPoint: Offset? = null

        points.forEach { (idx, mood) ->
            val x = xFor(idx)

            if (mood == 0) {
                val y = topInset + drawableHeight
                drawCircle(color = colors.onSurfaceVariant, radius = emptyPointRadius, center = Offset(x, y))
                lastPoint = null
            } else {
                val cur = Offset(x, yFor(mood))
                lastPoint?.let { drawLine(color = colors.primary, start = it, end = cur, strokeWidth = stroke) }
                drawCircle(color = colors.primary, radius = pointRadius, center = cur)
                lastPoint = cur
            }
        }
    }
}
