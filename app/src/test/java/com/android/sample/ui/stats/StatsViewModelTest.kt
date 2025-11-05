// app/src/test/java/com/android/sample/ui/stats/StatsViewModelTest.kt
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.android.sample.ui.stats

// Added fakes to avoid Firebase in unit tests
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import com.android.sample.ui.stats.model.StudyStats
import com.android.sample.ui.stats.repository.FakeStatsRepository
import com.android.sample.ui.stats.viewmodel.StatsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StatsViewModelTest {

  private val dispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun fake_scenarios_emit_into_state() {
    val vm = StatsViewModel(repo = FakeStatsRepository(), objectivesRepo = FakeObjectivesRepository)
    val s = vm.stats.value
    assertNotNull(s)
    // Default weekly goal in fake repo scenarios is 300
    assertEquals(300, s!!.weeklyGoalMin)
  }

  @Test
  fun titles_are_exposed_and_contain_known_labels() {
    val vm = StatsViewModel(repo = FakeStatsRepository(), objectivesRepo = FakeObjectivesRepository)
    val titles = vm.scenarioTitles
    assertTrue(titles.isNotEmpty())
    assertTrue(titles.contains("Début de semaine"))
    assertTrue(titles.contains("Semaine active"))
  }

  @Test
  fun initial_selected_index_is_zero() {
    val vm = StatsViewModel(repo = FakeStatsRepository(), objectivesRepo = FakeObjectivesRepository)
    assertEquals(0, vm.scenarioIndex.value)
  }

  @Test
  fun coversDefaultCtor() {
    // assume the last arg has a default
    StudyStats(0, emptyMap(), 2, emptyList())
  }

  @Test
  fun selectScenario_updates_stats_from_fake_repo() {
    val vm = StatsViewModel(repo = FakeStatsRepository(), objectivesRepo = FakeObjectivesRepository)
    val initial = vm.stats.value
    assertNotNull(initial)

    // In FakeStatsRepository, scenario index 1 is "Semaine active" with totalTimeMin = 145
    vm.selectScenario(1)

    val updated = vm.stats.value
    assertNotNull(updated)
    assertEquals(145, updated!!.totalTimeMin)
    assertEquals(1, vm.scenarioIndex.value)
  }

  @Test
  fun selectScenario_clamps_below_zero_to_zero() {
    val vm = StatsViewModel(repo = FakeStatsRepository(), objectivesRepo = FakeObjectivesRepository)
    vm.selectScenario(-10)
    assertEquals(0, vm.scenarioIndex.value)
  }

  @Test
  fun selectScenario_clamps_above_last_to_last() {
    val vm = StatsViewModel(repo = FakeStatsRepository(), objectivesRepo = FakeObjectivesRepository)
    vm.selectScenario(999)
    // Last scenario in FakeStatsRepository is index 4 with totalTimeMin = 180
    assertEquals(4, vm.scenarioIndex.value)
    assertEquals(180, vm.stats.value!!.totalTimeMin)
  }

  @Test
  fun multiple_selections_update_stats_each_time() {
    val vm = StatsViewModel(repo = FakeStatsRepository(), objectivesRepo = FakeObjectivesRepository)
    // 3 -> "Objectif atteint" totalTimeMin = 320
    vm.selectScenario(3)
    assertEquals(320, vm.stats.value!!.totalTimeMin)

    // 2 -> "Objectif presque atteint" totalTimeMin = 235
    vm.selectScenario(2)
    assertEquals(235, vm.stats.value!!.totalTimeMin)

    // back to 0 -> 0
    vm.selectScenario(0)
    assertEquals(0, vm.stats.value!!.totalTimeMin)
  }
}
