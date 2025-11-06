package com.android.sample.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.model.notifications.NotificationKind
import com.android.sample.model.notifications.NotificationRepository
import com.android.sample.ui.notifications.NotificationsViewModel
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class CapturingRepo : NotificationRepository {
  var oneShot = 0
  val weekly = mutableListOf<Triple<NotificationKind, Boolean, Map<Int, Pair<Int, Int>>>>()

  override fun scheduleOneMinuteFromNow(context: Context) {
    oneShot++
  }

  override fun setDailyEnabled(
      context: Context,
      kind: NotificationKind,
      enabled: Boolean,
      hour24: Int
  ) {}

  override fun setWeeklySchedule(
      context: Context,
      kind: NotificationKind,
      enabled: Boolean,
      times: Map<Int, Pair<Int, Int>>
  ) {
    weekly += Triple(kind, enabled, times)
  }

  override fun cancel(context: Context, kind: NotificationKind) {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NotificationsScreenTest {

  private val ctx: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun renders_updates_time_and_applies_schedule() {
    val repo = CapturingRepo()
    val vm = NotificationsViewModel(repo)

    vm.setKickoffEnabled(ctx, true)
    vm.toggleKickoffDay(Calendar.MONDAY)
    vm.updateKickoffTime(Calendar.MONDAY, 7, 30)

    // Applique la planification
    vm.applyKickoffSchedule(ctx)

    assertTrue(repo.weekly.isNotEmpty())
    val (kind, enabled, times) = repo.weekly.last()
    assertEquals(NotificationKind.NO_WORK_TODAY, kind)
    assertTrue(enabled)
    assertEquals(1, times.size)
    assertEquals(7 to 30, times[Calendar.MONDAY])
  }

  @Test
  fun test_button_triggers_one_shot() {
    val repo = CapturingRepo()
    val vm = NotificationsViewModel(repo)

    // Equivalent du bouton “Send notification in 1 min”
    vm.scheduleTestNotification(ctx)

    assertEquals(1, repo.oneShot)
  }
}
