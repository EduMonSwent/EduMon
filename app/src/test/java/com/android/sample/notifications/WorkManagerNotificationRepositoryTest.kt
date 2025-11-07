package com.android.sample.data.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import com.android.sample.model.notifications.NotificationKind
import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeScheduler : WorkScheduler {
  val oneTime = mutableListOf<String>()
  val periodic = mutableListOf<String>()
  val canceled = mutableListOf<String>()

  override fun enqueueUniqueWork(
      name: String,
      policy: ExistingWorkPolicy,
      request: OneTimeWorkRequest
  ) {
    oneTime += name
  }

  override fun enqueueUniquePeriodicWork(
      name: String,
      policy: ExistingPeriodicWorkPolicy,
      request: PeriodicWorkRequest
  ) {
    periodic += name
  }

  override fun cancelUniqueWork(name: String) {
    canceled += name
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class WorkManagerNotificationRepositoryTest {

  private val ctx: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun `weekly schedules only provided days and cancels previous`() {
    val fake = FakeScheduler()
    val repo = WorkManagerNotificationRepository { fake }

    val days = mapOf(Calendar.MONDAY to (8 to 0), Calendar.WEDNESDAY to (10 to 15))
    repo.setWeeklySchedule(ctx, NotificationKind.NO_WORK_TODAY, true, days)

    assertTrue(fake.canceled.contains("kickoff_${Calendar.MONDAY}"))
    assertTrue(fake.periodic.contains("kickoff_${Calendar.MONDAY}"))
    assertTrue(fake.periodic.contains("kickoff_${Calendar.WEDNESDAY}"))
    assertFalse(fake.periodic.contains("kickoff_${Calendar.TUESDAY}"))
  }

  @Test
  fun `daily schedule enqueue then cancel`() {
    val fake = FakeScheduler()
    val repo = WorkManagerNotificationRepository { fake }

    repo.setDailyEnabled(ctx, NotificationKind.KEEP_STREAK, true, 19)
    assertTrue(fake.periodic.contains("daily_streak_ALL"))

    repo.setDailyEnabled(ctx, NotificationKind.KEEP_STREAK, false, 19)
    assertTrue(fake.canceled.contains("daily_streak_ALL"))
  }

  @Test
  fun `one-minute test notification uses unique work`() {
    val fake = FakeScheduler()
    val repo = WorkManagerNotificationRepository { fake }
    repo.scheduleOneMinuteFromNow(ctx)
    assertTrue(fake.oneTime.contains("one_min_test"))
  }
}
