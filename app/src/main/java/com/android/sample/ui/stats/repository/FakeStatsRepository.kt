package com.android.sample.ui.stats.repository

// This file is deprecated and should be deleted.
// Use FakeUserStatsRepository instead.
/*
import com.android.sample.ui.stats.model.StudyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeStatsRepository(
    initialStats: StudyStats = StudyStats()
) : StatsRepository {
    override val stats: StateFlow<StudyStats?> = MutableStateFlow(initialStats)
    val selectedIndex = MutableStateFlow(0)
    val titles = listOf("Cloud")

    override fun start() {}
    override suspend fun update(stats: StudyStats) {}
    fun refresh() {}
    fun loadScenario(index: Int) {}
}
*/
