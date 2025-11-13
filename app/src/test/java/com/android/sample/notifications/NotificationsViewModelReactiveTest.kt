package com.android.sample.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.feature.schedule.data.calendar.Priority
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.feature.schedule.repository.calendar.CalendarRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests de la logique réactive de NotificationsViewModel (scheduling d'alarmes). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NotificationsViewModelReactiveTest {

  private lateinit var fakeRepo: FakeCalendarRepository
  private lateinit var vm: com.android.sample.ui.notifications.NotificationsViewModel
  private val dispatcher = UnconfinedTestDispatcher()
  private val ctx: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    fakeRepo = FakeCalendarRepository()
    // Stub AlarmHelper pour éviter de programmer de vraies alarmes
    mockkObject(com.android.sample.data.notifications.AlarmHelper)
    every {
      com.android.sample.data.notifications.AlarmHelper.scheduleStudyAlarm(any(), any(), any())
    } just runs
    every { com.android.sample.data.notifications.AlarmHelper.cancelStudyAlarm(any(), any()) } just
        runs

    vm =
        com.android.sample.ui.notifications.NotificationsViewModel(
            calendarRepository = fakeRepo,
            observeDispatcher = dispatcher,
            debounceMillis = 0L // pas d'attente dans les tests
            )
  }

  @Test
  fun `schedules first upcoming study task`() = runTest {
    val futureTask = studyTask("t1", daysFromNow = 1, hour = 10, minute = 0)
    fakeRepo.emit(listOf(futureTask))
    vm.startObservingSchedule(ctx)
    // cycle
    assertEquals("t1", vm.currentScheduledTaskId())
    verify(exactly = 1) {
      com.android.sample.data.notifications.AlarmHelper.scheduleStudyAlarm(ctx, "t1", any())
    }
  }

  @Test
  fun `does not reschedule when same task remains next`() = runTest {
    val task = studyTask("same", daysFromNow = 1, hour = 9, minute = 0)
    fakeRepo.emit(listOf(task))
    vm.startObservingSchedule(ctx)
    assertEquals("same", vm.currentScheduledTaskId())
    // Emit again identical list
    fakeRepo.emit(listOf(task))
    assertEquals("same", vm.currentScheduledTaskId())
    verify(exactly = 1) {
      com.android.sample.data.notifications.AlarmHelper.scheduleStudyAlarm(ctx, "same", any())
    }
  }

  @Test
  fun `cancels alarm when no upcoming study task remains`() = runTest {
    val task = studyTask("gone", daysFromNow = 1, hour = 8, minute = 30)
    fakeRepo.emit(listOf(task))
    vm.startObservingSchedule(ctx)
    assertEquals("gone", vm.currentScheduledTaskId())
    // Remove all tasks
    fakeRepo.emit(emptyList())
    assertNull(vm.currentScheduledTaskId())
    verify { com.android.sample.data.notifications.AlarmHelper.cancelStudyAlarm(ctx, "gone") }
  }

  @Test
  fun `fallback one-shot schedules next task`() = runTest {
    val task = studyTask("one-shot", daysFromNow = 2, hour = 11, minute = 15)
    fakeRepo.emit(listOf(task))
    vm.scheduleNextStudySessionNotification(ctx)
    assertEquals("one-shot", vm.currentScheduledTaskId())
    verify {
      com.android.sample.data.notifications.AlarmHelper.scheduleStudyAlarm(ctx, "one-shot", any())
    }
  }

  // --- Helpers ---
  private fun studyTask(id: String, daysFromNow: Long, hour: Int, minute: Int): StudyItem =
      StudyItem(
          id = id,
          title = "Study $id",
          date = LocalDate.now().plusDays(daysFromNow),
          time = LocalTime.of(hour, minute),
          durationMinutes = 60,
          isCompleted = false,
          priority = Priority.MEDIUM,
          type = TaskType.STUDY)

  private class FakeCalendarRepository : CalendarRepository {
    private val flow = MutableStateFlow<List<StudyItem>>(emptyList())
    override val tasksFlow = flow

    override suspend fun getAllTasks(): List<StudyItem> = flow.value

    override suspend fun getTaskById(taskId: String): StudyItem? =
        flow.value.find { it.id == taskId }

    override suspend fun saveTask(task: StudyItem) {
      flow.value = flow.value + task
    }

    override suspend fun deleteTask(taskId: String) {
      flow.value = flow.value.filter { it.id != taskId }
    }

    fun emit(list: List<StudyItem>) {
      flow.value = list
    }
  }
}
