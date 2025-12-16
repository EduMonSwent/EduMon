package com.android.sample.ui.stats

import com.android.sample.data.FakeUserStatsRepository
import com.android.sample.ui.stats.model.StudyStats
import com.android.sample.ui.stats.repository.FakeStatsRepository
import com.android.sample.ui.stats.viewmodel.StatsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private lateinit var fakeStatsRepo: FakeStatsRepository
  private lateinit var fakeUserStatsRepo: FakeUserStatsRepository

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    fakeStatsRepo = FakeStatsRepository()
    fakeUserStatsRepo = FakeUserStatsRepository()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state emits stats from repository`() = runTest {
    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = com.android.sample.feature.weeks.repository.FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)
    advanceUntilIdle()

    val stats = vm.stats.value
    assertNotNull(stats)
    assertEquals(300, stats!!.weeklyGoalMin)
  }

  @Test
  fun `userStats flow is exposed from userStatsRepository`() = runTest {
    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = com.android.sample.feature.weeks.repository.FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)
    advanceUntilIdle()

    val userStats = vm.userStats.value
    assertNotNull(userStats)
    assertEquals(0, userStats.totalStudyMinutes)
    assertEquals(0, userStats.todayStudyMinutes)
  }

  @Test
  fun `titles are exposed from repository`() = runTest {
    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = com.android.sample.feature.weeks.repository.FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)
    advanceUntilIdle()

    val titles = vm.scenarioTitles
    assertTrue(titles.isNotEmpty())
    assertTrue(titles.contains("Début de semaine"))
    assertTrue(titles.contains("Semaine active"))
  }

  @Test
  fun `initial selected index is zero`() = runTest {
    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = com.android.sample.feature.weeks.repository.FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)
    advanceUntilIdle()

    assertEquals(0, vm.scenarioIndex.value)
  }

  @Test
  fun `selectScenario updates stats and index`() = runTest {
    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = com.android.sample.feature.weeks.repository.FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)
    advanceUntilIdle()

    val initial = vm.stats.value
    assertNotNull(initial)
    assertEquals(0, initial!!.totalTimeMin)

    vm.selectScenario(1)
    advanceUntilIdle()

    val updated = vm.stats.value
    assertNotNull(updated)
    assertEquals(145, updated!!.totalTimeMin)
    assertEquals(1, vm.scenarioIndex.value)
  }

  @Test
  fun `selectScenario clamps negative index to zero`() = runTest {
    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = com.android.sample.feature.weeks.repository.FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)
    advanceUntilIdle()

    vm.selectScenario(-10)
    advanceUntilIdle()

    assertEquals(0, vm.scenarioIndex.value)
  }

  @Test
  fun `selectScenario clamps index above max to last scenario`() = runTest {
    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = com.android.sample.feature.weeks.repository.FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)
    advanceUntilIdle()

    vm.selectScenario(999)
    advanceUntilIdle()

    assertEquals(4, vm.scenarioIndex.value)
    assertEquals(180, vm.stats.value!!.totalTimeMin)
  }

  @Test
  fun `multiple scenario selections update stats correctly`() = runTest {
    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = com.android.sample.feature.weeks.repository.FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)
    advanceUntilIdle()

    vm.selectScenario(3)
    advanceUntilIdle()
    assertEquals(320, vm.stats.value!!.totalTimeMin)

    vm.selectScenario(2)
    advanceUntilIdle()
    assertEquals(235, vm.stats.value!!.totalTimeMin)

    vm.selectScenario(0)
    advanceUntilIdle()
    assertEquals(0, vm.stats.value!!.totalTimeMin)
  }

  @Test
  fun `StudyStats data class can be created with default values`() {
    val stats =
        StudyStats(
            totalTimeMin = 0,
            courseTimesMin = emptyMap(),
            completedGoals = 2,
            progressByDayMin = emptyList())

    assertEquals(0, stats.totalTimeMin)
    assertEquals(0, stats.courseTimesMin.size)
    assertEquals(2, stats.completedGoals)
    assertTrue(stats.progressByDayMin.isEmpty())
  }

  @Test
  fun `stats flow updates when repository updates`() = runTest {
    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = com.android.sample.feature.weeks.repository.FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)
    advanceUntilIdle()

    val initial = vm.stats.value
    assertNotNull(initial)

    // Simulate repository update
    fakeStatsRepo.update(initial!!.copy(totalTimeMin = 999))
    advanceUntilIdle()

    assertEquals(999, vm.stats.value!!.totalTimeMin)
  }

  @Test
  fun `userStats updates when userStatsRepository updates`() = runTest {
    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = com.android.sample.feature.weeks.repository.FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)
    advanceUntilIdle()

    assertEquals(0, vm.userStats.value.totalStudyMinutes)

    // Simulate study session
    fakeUserStatsRepo.addStudyMinutes(25)
    advanceUntilIdle()

    assertEquals(25, vm.userStats.value.totalStudyMinutes)
  }
}
