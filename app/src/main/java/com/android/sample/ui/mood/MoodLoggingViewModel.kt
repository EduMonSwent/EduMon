package com.android.sample.ui.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.MoodEntry
import com.android.sample.data.MoodRepositoryInterface
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MoodUiState(
    val today: LocalDate = LocalDate.now(),
    val selectedMood: Int = 3,
    val note: String = "",
    val existingToday: MoodEntry? = null,
    val last7Days: List<MoodEntry> = emptyList(),
    val monthEntries: List<MoodEntry> = emptyList(),
    val canEditToday: Boolean = true,
    val chartMode: ChartMode = ChartMode.WEEK
)

enum class ChartMode {
  WEEK,
  MONTH
}

/**
 * ✅ Refactored ViewModel — no Application dependency The repository is injected, so we can use a
 * fake or in-memory one in tests.
 */
class MoodLoggingViewModel(
    private val repo: MoodRepositoryInterface,
    private val clock: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

  private val _ui = MutableStateFlow(MoodUiState())
  val ui: StateFlow<MoodUiState> = _ui

  init {
    refreshAll()
  }

  fun refreshAll() {
    viewModelScope.launch {
      val today = clock()
      val existing = repo.getForDate(today)
      val startWeek = today.minusDays(6)
      val week = repo.getRange(startWeek, today)
      val startMonth = today.minusDays(29)
      val month = repo.getRange(startMonth, today)

      _ui.value =
          _ui.value.copy(
              today = today,
              existingToday = existing,
              selectedMood = existing?.mood ?: 3,
              note = existing?.note ?: "",
              last7Days = fillGaps(startWeek, today, week),
              monthEntries = fillGaps(startMonth, today, month))
    }
  }

  fun onMoodSelected(mood: Int) {
    _ui.value = _ui.value.copy(selectedMood = mood.coerceIn(1, 5))
  }

  fun onNoteChanged(text: String) {
    _ui.value = _ui.value.copy(note = text.take(140))
  }

  fun onChartMode(mode: ChartMode) {
    _ui.value = _ui.value.copy(chartMode = mode)
  }

  fun saveToday() {
    viewModelScope.launch {
      val s = _ui.value
      repo.upsertForDate(s.today, s.selectedMood, s.note)
      refreshAll()
    }
  }

  /** Optional helper for tests to simulate arbitrary days */
  fun saveForDate(date: LocalDate, mood: Int, note: String) {
    viewModelScope.launch { repo.upsertForDate(date, mood.coerceIn(1, 5), note.take(140)) }
  }

  private fun fillGaps(
      start: LocalDate,
      end: LocalDate,
      entries: List<MoodEntry>
  ): List<MoodEntry> {
    val map = entries.associateBy { LocalDate.ofEpochDay(it.dateEpochDay) }
    val result = ArrayList<MoodEntry>()
    var d = start
    while (!d.isAfter(end)) {
      result += map[d] ?: MoodEntry(d.toEpochDay(), 0, "")
      d = d.plusDays(1)
    }
    return result
  }
}
