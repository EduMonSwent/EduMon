// app/src/test/java/com/android/sample/ui/stats/StatsViewModelTest.kt
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.android.sample.ui.stats

import com.android.sample.data.FakeUserStatsRepository
import com.android.sample.data.UserStats
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import com.android.sample.ui.stats.viewmodel.StatsViewModel
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
  fun `stats flow maps UserStats to StudyStats`() = runTest {
    val userStatsRepo =
        FakeUserStatsRepository(
            UserStats(totalStudyMinutes = 120, weeklyGoal = 300, todayCompletedPomodoros = 3))
    val vm =
        StatsViewModel(userStatsRepo = userStatsRepo, objectivesRepo = FakeObjectivesRepository)

    val studyStats = vm.stats.first()
    assertNotNull(studyStats)
    assertEquals(120, studyStats!!.totalTimeMin)
    assertEquals(300, studyStats.weeklyGoalMin)
  }

  @Test
  fun `selectScenario updates scenarioIndex`() {
    val vm =
        StatsViewModel(
            userStatsRepo = FakeUserStatsRepository(), objectivesRepo = FakeObjectivesRepository)
    vm.selectScenario(1)
    assertEquals(1, vm.scenarioIndex.value)
  }

  @Test
  fun `syncCompletedGoalsFromObjectives updates repository`() = runTest {
    val userStatsRepo = FakeUserStatsRepository()
    val objectivesRepo = FakeObjectivesRepository
    objectivesRepo.setObjectives(
        listOf(
            Objective("Test 1", "Course A", 60, true, DayOfWeek.MONDAY),
            Objective("Test 2", "Course B", 30, false, DayOfWeek.TUESDAY),
            Objective("Test 3", "Course C", 45, true, DayOfWeek.WEDNESDAY),
        ))
    val vm = StatsViewModel(userStatsRepo = userStatsRepo, objectivesRepo = objectivesRepo)

    vm.syncCompletedGoalsFromObjectives()

    assertEquals(2, userStatsRepo.stats.value.completedGoals)
  }
}
