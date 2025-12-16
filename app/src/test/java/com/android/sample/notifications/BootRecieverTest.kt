package com.android.sample.notifications

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.android.sample.data.notifications.AlarmHelper
import com.android.sample.data.notifications.BootReceiver
import com.android.sample.feature.schedule.data.calendar.Priority
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@OptIn(ExperimentalCoroutinesApi::class)
class BootReceiverTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun beforeEach() {
    try {
      unmockkObject(AlarmHelper)
    } catch (_: Exception) {}
  }

  // Fake minimal controllable CalendarRepository for this test
  private class FakeCalendarRepository :
      com.android.sample.feature.schedule.repository.calendar.CalendarRepository {
    private val _flow = MutableStateFlow<List<StudyItem>>(emptyList())
    override val tasksFlow = _flow

    override suspend fun getAllTasks(): List<StudyItem> = _flow.value

    override suspend fun getTaskById(taskId: String): StudyItem? =
        _flow.value.find { it.id == taskId }

    override suspend fun saveTask(task: StudyItem) {
      _flow.value = _flow.value + task
    }

    override suspend fun deleteTask(taskId: String) {
      _flow.value = _flow.value.filter { it.id != taskId }
    }

    fun setAll(list: List<StudyItem>) {
      _flow.value = list
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `bootReceiver schedules next study task alarm 15min before`() = runTest {
    val repo = FakeCalendarRepository()
    val futureDate = LocalDate.now().plusDays(1)
    val futureTime = LocalTime.of(10, 0)
    val eventId = "evt1234"
    val task =
        StudyItem(
            id = eventId,
            title = "Study Session",
            date = futureDate,
            time = futureTime,
            durationMinutes = 60,
            isCompleted = false,
            priority = Priority.MEDIUM,
            type = TaskType.STUDY)
    repo.setAll(listOf(task))

    val testScope = TestScope(UnconfinedTestDispatcher())
    val receiver = BootReceiver(repo, testScope)

    // Mock AlarmHelper to capture trigger millis deterministically
    mockkObject(AlarmHelper)
    var capturedMillis: Long? = null
    every { AlarmHelper.scheduleStudyAlarm(context, eventId, any()) } answers
        {
          capturedMillis = arg(2)
        }

    receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
    testScope.advanceUntilIdle()

    // Verify called once with approx 15 minutes before
    io.mockk.verify(exactly = 1) { AlarmHelper.scheduleStudyAlarm(context, eventId, any()) }

    val zone = ZoneId.systemDefault()
    val expected =
        futureDate.atTime(futureTime).atZone(zone).toInstant().toEpochMilli() -
            java.time.Duration.ofMinutes(15).toMillis()
    val actual = capturedMillis ?: error("No trigger captured")
    val delta = kotlin.math.abs(actual - expected)
    assertTrue("Trigger should be ~15min before (delta=$delta)", delta < 5_000L)
  }

  @Test
  fun `onReceive ignores non boot action`() {
    val repo = FakeCalendarRepository()
    // No async scheduling needed; default scope is fine
    val receiver = BootReceiver(repo)
    val intent = Intent("SOME_OTHER_ACTION")
    receiver.onReceive(context, intent)
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val shadowAm = Shadows.shadowOf(am)
    assertNull(shadowAm.nextScheduledAlarm)
  }

  @Test
  fun `onReceive starts campus chain unconditionally on boot`() {
    // Campus chain should start regardless of preference setting
    WorkManagerTestInitHelper.initializeTestWorkManager(context)
    val repo = FakeCalendarRepository()
    val receiver = BootReceiver(repo) // default scope sufficient
    val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
    receiver.onReceive(context, intent)
    val workInfos = WorkManager.getInstance(context).getWorkInfosByTag("campus_entry_poll").get()
    assertTrue("Campus entry polling should be scheduled after boot", workInfos.isNotEmpty())
  }

  @Test
  fun `onReceive handles SecurityException from AlarmHelper gracefully`() = runTest {
    val repo = FakeCalendarRepository()
    val futureDate = LocalDate.now().plusDays(1)
    val futureTime = LocalTime.of(9, 0)
    val task =
        StudyItem(
            id = "secEx",
            title = "Study",
            date = futureDate,
            time = futureTime,
            durationMinutes = 60,
            isCompleted = false,
            priority = Priority.MEDIUM,
            type = TaskType.STUDY)
    repo.setAll(listOf(task))

    mockkObject(AlarmHelper)
    every { AlarmHelper.scheduleStudyAlarm(any(), any(), any()) } throws SecurityException("denied")

    val testScope = TestScope(UnconfinedTestDispatcher())
    val receiver = BootReceiver(repo, scope = testScope)
    val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
    receiver.onReceive(context, intent)
    testScope.advanceUntilIdle()

    // Should not crash; verify attempted call
    verify { AlarmHelper.scheduleStudyAlarm(context, "secEx", any()) }

    // Clean up mock to avoid leaking into other tests
    unmockkObject(AlarmHelper)
  }
}
