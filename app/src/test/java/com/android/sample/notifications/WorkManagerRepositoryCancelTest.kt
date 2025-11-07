package com.android.sample.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import com.android.sample.data.notifications.WorkManagerNotificationRepository
import com.android.sample.data.notifications.WorkScheduler
import com.android.sample.data.notifications.cancelKeysFor
import com.android.sample.model.notifications.NotificationKind
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class CapturingScheduler : WorkScheduler {
  val canceled = mutableListOf<String>()

  override fun cancelUniqueWork(name: String) {
    canceled += name
  }

  override fun enqueueUniqueWork(
      name: String,
      policy: ExistingWorkPolicy,
      request: OneTimeWorkRequest
  ) {}

  override fun enqueueUniquePeriodicWork(
      name: String,
      policy: ExistingPeriodicWorkPolicy,
      request: PeriodicWorkRequest
  ) {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class WorkManagerNotificationRepositoryCancelTest {

  private val ctx: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun cancel_no_work_today_cancels_7_days_plus_daily_kickoff() {
    val fake = CapturingScheduler()
    val repo = WorkManagerNotificationRepository(schedulerProvider = { fake })

    repo.cancel(ctx, NotificationKind.NO_WORK_TODAY)

    val expected =
        listOf(
            "kickoff_${Calendar.MONDAY}",
            "kickoff_${Calendar.TUESDAY}",
            "kickoff_${Calendar.WEDNESDAY}",
            "kickoff_${Calendar.THURSDAY}",
            "kickoff_${Calendar.FRIDAY}",
            "kickoff_${Calendar.SATURDAY}",
            "kickoff_${Calendar.SUNDAY}",
            "daily_kickoff_ALL")
    assertEquals(expected, fake.canceled)
    assertEquals(expected, cancelKeysFor(NotificationKind.NO_WORK_TODAY))
  }

  @Test
  fun cancel_keep_streak_cancels_7_days_plus_daily_streak() {
    val fake = CapturingScheduler()
    val repo = WorkManagerNotificationRepository(schedulerProvider = { fake })

    repo.cancel(ctx, NotificationKind.KEEP_STREAK)

    val expected =
        listOf(
            "streak_${Calendar.MONDAY}",
            "streak_${Calendar.TUESDAY}",
            "streak_${Calendar.WEDNESDAY}",
            "streak_${Calendar.THURSDAY}",
            "streak_${Calendar.FRIDAY}",
            "streak_${Calendar.SATURDAY}",
            "streak_${Calendar.SUNDAY}",
            "daily_streak_ALL")
    assertEquals(expected, fake.canceled)
    assertEquals(expected, cancelKeysFor(NotificationKind.KEEP_STREAK))
  }
}
