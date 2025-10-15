// app/src/test/java/com/android/sample/ui/stats/StatsViewModelTest.kt
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.android.sample.ui.stats

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class StatsViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    System.setProperty("SKIP_FIRESTORE", "true")
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    System.clearProperty("SKIP_FIRESTORE")
  }

  @Test
  fun `fake scenarios emit into state`() = runTest {
    val vm = StatsViewModel()
    dispatcher.scheduler.advanceUntilIdle()

    val s = vm.stats.value
    assertNotNull(s)
    assertEquals(300, s!!.weeklyGoalMin)
  }

  @Test
  fun `attachFirestore overrides state from external flow`() = runTest {
    val vm = StatsViewModel()
    val external = MutableStateFlow(StudyStats(totalTimeMin = 999, weeklyGoalMin = 100))

    vm.attachFirestore(external)
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(999, vm.stats.value!!.totalTimeMin)

    external.value = StudyStats(totalTimeMin = 123, weeklyGoalMin = 200)
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(123, vm.stats.value!!.totalTimeMin)
  }
}
