package com.android.sample.ui.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.notifications.NotificationKind
import com.android.sample.model.notifications.NotificationRepository
import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

private class FakeRepo : NotificationRepository {
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

@RunWith(AndroidJUnit4::class)
class NotificationsViewModelTest {

  private val ctx: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun schedule_test_notification_calls_repo() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)
    vm.scheduleTestNotification(ctx)
    assertEquals(1, repo.oneShotCalls)
  }

  @Test
  fun enabling_kickoff_schedules_weekly_with_selected_days_and_times() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    // Par défaut: aucun jour sélectionné → on choisit explicitement Monday
    vm.toggleKickoffDay(Calendar.MONDAY)
    vm.updateKickoffTime(Calendar.MONDAY, 7, 30)

    // Active la fonctionnalité puis applique la planification
    vm.setKickoffEnabled(ctx, true)
    vm.applyKickoffSchedule(ctx)

    assertTrue(vm.kickoffEnabled.value)

    val (kind, enabled, times) = repo.weeklyCalls.last()
    assertEquals(NotificationKind.NO_WORK_TODAY, kind)
    assertTrue(enabled)

    // Uniquement Monday planifié à 07:30
    assertEquals(1, times.size)
    assertTrue(times.containsKey(Calendar.MONDAY))
    assertFalse(times.containsKey(Calendar.SATURDAY))
    assertFalse(times.containsKey(Calendar.SUNDAY))
    assertEquals(7 to 30, times[Calendar.MONDAY])
  }

  @Test
  fun disabling_kickoff_cancels() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)
    vm.setKickoffEnabled(ctx, false)
    vm.applyKickoffSchedule(ctx)
    assertTrue(repo.canceled.contains(NotificationKind.NO_WORK_TODAY))
  }
}
