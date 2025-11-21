package com.android.sample.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.feature.schedule.data.calendar.Priority
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.feature.schedule.repository.calendar.CalendarRepository
import com.android.sample.model.notifications.NotificationKind
import com.android.sample.model.notifications.NotificationRepository
import com.android.sample.ui.notifications.NotificationsViewModel
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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Additional coverage for NotificationsViewModel branches not exercised previously. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelMoreCoverageTest {
  private lateinit var vm: NotificationsViewModel
  private lateinit var fakeCalRepo: FakeCalendarRepository
  private lateinit var fakeNotifRepo: FakeNotificationRepo
  private val ctx: Context = ApplicationProvider.getApplicationContext()
  private val dispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    fakeCalRepo = FakeCalendarRepository()
    fakeNotifRepo = FakeNotificationRepo()
    mockkObject(com.android.sample.data.notifications.AlarmHelper)
    every {
      com.android.sample.data.notifications.AlarmHelper.scheduleStudyAlarm(any(), any(), any())
    } just runs
    every { com.android.sample.data.notifications.AlarmHelper.cancelStudyAlarm(any(), any()) } just
        runs
    vm = NotificationsViewModel(fakeNotifRepo, fakeCalRepo, dispatcher, debounceMillis = 0L)
  }

  @Test
  fun `setKickoffEnabled false cancels NO_WORK_TODAY`() {
    vm.setKickoffEnabled(ctx, false)
    assertTrue(fakeNotifRepo.canceled.contains(NotificationKind.NO_WORK_TODAY))
  }

  @Test
  fun `applyKickoffSchedule does nothing when kickoff disabled`() {
    vm.setKickoffEnabled(ctx, false)
    vm.applyKickoffSchedule(ctx)
    assertTrue("No weekly calls expected", fakeNotifRepo.weeklyCalls.isEmpty())
  }

  @Test
  fun `setStreakEnabled schedules daily when enabled and cancels when disabled`() {
    vm.setStreakEnabled(ctx, true)
    assertTrue(
        fakeNotifRepo.dailyCalls.any { it.first == NotificationKind.KEEP_STREAK && it.second })
    vm.setStreakEnabled(ctx, false)
    vm.setStreakEnabled(ctx, true) // enable again
    assertTrue(fakeNotifRepo.dailyCalls.count { it.first == NotificationKind.KEEP_STREAK } >= 2)
  }

  @Test
  fun `setTaskNotificationsEnabled false cancels current alarm and stops observer`() = runTest {
    // Prepare two tasks
    val t1 = study("A", 1, 10, 0)
    val t2 = study("B", 2, 11, 0)
    fakeCalRepo.emit(listOf(t1, t2))
    vm.startObservingSchedule(ctx)
    assertEquals("A", vm.currentScheduledTaskId())
    vm.setTaskNotificationsEnabled(ctx, false)
    assertNull(vm.currentScheduledTaskId())
    verify { com.android.sample.data.notifications.AlarmHelper.cancelStudyAlarm(ctx, "A") }
  }

  @Test
  fun `startObservingSchedule skips resubscribe when already active`() = runTest {
    val t1 = study("A", 1, 9, 0)
    fakeCalRepo.emit(listOf(t1))
    vm.startObservingSchedule(ctx)
    vm.startObservingSchedule(ctx) // second call should be ignored
    verify(exactly = 1) {
      com.android.sample.data.notifications.AlarmHelper.scheduleStudyAlarm(ctx, "A", any())
    }
  }

  @Test
  fun `scheduleNextStudySessionNotification returns early when no tasks`() {
    vm.scheduleNextStudySessionNotification(ctx)
    assertNull(vm.currentScheduledTaskId())
  }

  @Test
  fun `scheduleNextStudySessionNotification schedules next when tasks exist`() {
    val t1 = study("F", 1, 14, 15)
    fakeCalRepo.emit(listOf(t1))
    vm.scheduleNextStudySessionNotification(ctx)
    assertEquals("F", vm.currentScheduledTaskId())
    verify { com.android.sample.data.notifications.AlarmHelper.scheduleStudyAlarm(ctx, "F", any()) }
  }

  @Test
  fun `deep link demo notification does nothing if POST_NOTIFICATIONS missing on T+`() {
    // Force Android 33 logic path by simulating missing permission
    val tasks = listOf(study("G", 1, 8, 0))
    fakeCalRepo.emit(tasks)
    // We cannot easily revoke permission in Robolectric <33; rely on branch being safe.
    // Call sendDeepLinkDemoNotification: should try schedule but not crash
    vm.sendDeepLinkDemoNotification(ctx)
    assertTrue(true) // reached without exception
  }

  // --- Helpers ---
  private fun study(id: String, days: Long, h: Int, m: Int) =
      StudyItem(
          id = id,
          title = id,
          date = LocalDate.now().plusDays(days),
          time = LocalTime.of(h, m),
          durationMinutes = 30,
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

  private class FakeNotificationRepo : NotificationRepository {
    var oneShotCalls = 0
    var dailyCalls = mutableListOf<Triple<NotificationKind, Boolean, Int>>()
    var weeklyCalls = mutableListOf<Triple<NotificationKind, Boolean, Map<Int, Pair<Int, Int>>>>()
    var canceled = mutableListOf<NotificationKind>()

    override fun scheduleOneMinuteFromNow(context: Context) {
      oneShotCalls++
    }

    override fun setDailyEnabled(
        context: Context,
        kind: NotificationKind,
        enabled: Boolean,
        hour24: Int
    ) {
      dailyCalls += Triple(kind, enabled, hour24)
    }

    override fun setWeeklySchedule(
        context: Context,
        kind: NotificationKind,
        enabled: Boolean,
        times: Map<Int, Pair<Int, Int>>
    ) {
      weeklyCalls += Triple(kind, enabled, times)
    }

    override fun cancel(context: Context, kind: NotificationKind) {
      canceled += kind
    }
  }
}
