package com.android.sample.ui.stats.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.ui.stats.model.StudyStats
import com.android.sample.ui.stats.repository.FakeStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** ViewModel for StatsScreen. Bridges UI to the repository. */
class StatsViewModel(
    private val repo: FakeStatsRepository = FakeStatsRepository()
) : ViewModel() {

    private val _stats = MutableStateFlow<StudyStats?>(null)
    val stats: StateFlow<StudyStats?> = _stats

    val scenarioTitles: List<String> get() = repo.titles
    val scenarioIndex: StateFlow<Int> get() = repo.selectedIndex

    fun selectScenario(i: Int) = repo.loadScenario(i)

    init {
        viewModelScope.launch {
            repo.stats.collect { value -> _stats.value = value }
        }
    }

    /** Optional: attach a remote flow (e.g., Firestore) overriding local stats. */
    fun attachRemote(flow: StateFlow<StudyStats?>) {
        viewModelScope.launch {
            flow.collect { s -> if (s != null) _stats.value = s }
        }
    }
}
