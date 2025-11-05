package com.android.sample.ui.stats.repository

import com.android.sample.ui.stats.model.StudyStats
import kotlinx.coroutines.flow.StateFlow

/** Abstraction for providing StudyStats to the UI. */
interface StatsRepository {
  val stats: StateFlow<StudyStats>
  val selectedIndex: StateFlow<Int>
  val titles: List<String>

  fun loadScenario(index: Int)

  suspend fun refresh()

  suspend fun update(stats: StudyStats)
}
