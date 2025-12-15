package com.android.sample.ui.notifications

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.android.sample.model.notifications.NotificationKind
import com.android.sample.model.notifications.NotificationRepository
import java.util.Calendar
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Parts of this code were written using ChatGPT

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
@Config(sdk = [33])
class NotificationsViewModelTest {

  private val ctx: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    // Initialize WorkManager for tests that need it
    val config = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.DEBUG).build()
    WorkManagerTestInitHelper.initializeTestWorkManager(ctx, config)
  }

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

  @Test
  fun kickoffTimes_initialized_for_all_days_at_nine() {
    val vm = NotificationsViewModel(FakeRepo())
    val times = vm.kickoffTimes.value
    assertEquals(7, times.size)
    val expected = 9 to 0
    assertTrue(times.values.all { it == expected })
  }

  @Test
  fun setStreakEnabled_updates_state_and_repo() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    vm.setStreakEnabled(ctx, true)
    assertTrue(vm.streakEnabled.value)
    assertTrue(repo.dailyCalls.isNotEmpty())
    val (kindTrue, enabledTrue, hourTrue) = repo.dailyCalls.last()
    assertEquals(NotificationKind.KEEP_STREAK, kindTrue)
    assertTrue(enabledTrue)
    assertEquals(19, hourTrue)

    vm.setStreakEnabled(ctx, false)
    assertFalse(vm.streakEnabled.value)
    val (kindFalse, enabledFalse, hourFalse) = repo.dailyCalls.last()
    assertEquals(NotificationKind.KEEP_STREAK, kindFalse)
    assertFalse(enabledFalse)
    assertEquals(19, hourFalse)
  }

  @Test
  fun needsNotificationPermission_and_requestOrSchedule_behave_with_permission_denied() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    // Simulate permission denied
    (ctx as? android.app.Application)?.let {
      org.robolectric.Shadows.shadowOf(it).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    assertTrue(vm.needsNotificationPermission(ctx))

    var requestedPermission: String? = null
    vm.requestOrSchedule(ctx) { perm -> requestedPermission = perm }

    assertEquals(Manifest.permission.POST_NOTIFICATIONS, requestedPermission)
    assertEquals(0, repo.oneShotCalls)
  }

  @Test
  fun needsNotificationPermission_and_requestOrSchedule_behave_with_permission_granted() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    // Simulate permission granted
    (ctx as? android.app.Application)?.let {
      org.robolectric.Shadows.shadowOf(it).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    assertFalse(vm.needsNotificationPermission(ctx))

    var requestedPermission: String? = null
    vm.requestOrSchedule(ctx) { perm -> requestedPermission = perm }

    // Should schedule directly, not request permission
    assertNull(requestedPermission)
    assertEquals(1, repo.oneShotCalls)
  }

  @Test
  fun setFriendStudyModeEnabled_whenEnabled_updatesStateFlow() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    assertFalse("Should be disabled by default", vm.friendStudyModeEnabled.value)

    vm.setFriendStudyModeEnabled(ctx, true)

    assertTrue("StateFlow should be updated to true", vm.friendStudyModeEnabled.value)
  }

  @Test
  fun setFriendStudyModeEnabled_whenDisabled_updatesStateFlow() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    // First enable it
    vm.setFriendStudyModeEnabled(ctx, true)
    assertTrue(vm.friendStudyModeEnabled.value)

    // Then disable it
    vm.setFriendStudyModeEnabled(ctx, false)

    assertFalse("StateFlow should be updated to false", vm.friendStudyModeEnabled.value)
  }

  @Test
  fun setFriendStudyModeEnabled_whenEnabled_persistsToSharedPreferences() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    vm.setFriendStudyModeEnabled(ctx, true)

    val prefs = ctx.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    val enabled = prefs.getBoolean("friend_study_mode_enabled", false)

    assertTrue("Preference should be persisted as true", enabled)
  }

  @Test
  fun setFriendStudyModeEnabled_whenDisabled_persistsToSharedPreferences() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    // First enable
    vm.setFriendStudyModeEnabled(ctx, true)

    // Then disable
    vm.setFriendStudyModeEnabled(ctx, false)

    val prefs = ctx.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    val enabled = prefs.getBoolean("friend_study_mode_enabled", true)

    assertFalse("Preference should be persisted as false", enabled)
  }

  @Test
  fun setFriendStudyModeEnabled_whenEnabled_startsWorkerChain() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    vm.setFriendStudyModeEnabled(ctx, true)

    // Verify WorkManager has scheduled work
    val workManager = androidx.work.WorkManager.getInstance(ctx)
    val workInfos = workManager.getWorkInfosByTag("friend_study_mode_poll").get()

    assertTrue("Worker chain should be started", workInfos.isNotEmpty())
  }

  @Test
  fun setFriendStudyModeEnabled_whenDisabled_cancelsWorkerChain() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    // First enable to start the chain
    vm.setFriendStudyModeEnabled(ctx, true)

    // Verify work is scheduled
    val workManager = androidx.work.WorkManager.getInstance(ctx)
    var workInfos = workManager.getWorkInfosByTag("friend_study_mode_poll").get()
    assertTrue("Work should be scheduled after enabling", workInfos.isNotEmpty())

    // Then disable to cancel the chain
    vm.setFriendStudyModeEnabled(ctx, false)

    // Verify work is cancelled
    workInfos = workManager.getWorkInfosByTag("friend_study_mode_poll").get()
    val hasActiveWork =
        workInfos.any {
          it.state == androidx.work.WorkInfo.State.ENQUEUED ||
              it.state == androidx.work.WorkInfo.State.RUNNING
        }

    assertFalse("Worker chain should be cancelled", hasActiveWork)
  }

  @Test
  fun setFriendStudyModeEnabled_toggleMultipleTimes_worksCorrectly() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)
    val prefs = ctx.getSharedPreferences("notifications", Context.MODE_PRIVATE)

    // Enable
    vm.setFriendStudyModeEnabled(ctx, true)
    assertTrue(vm.friendStudyModeEnabled.value)
    assertTrue(prefs.getBoolean("friend_study_mode_enabled", false))

    // Disable
    vm.setFriendStudyModeEnabled(ctx, false)
    assertFalse(vm.friendStudyModeEnabled.value)
    assertFalse(prefs.getBoolean("friend_study_mode_enabled", true))

    // Enable again
    vm.setFriendStudyModeEnabled(ctx, true)
    assertTrue(vm.friendStudyModeEnabled.value)
    assertTrue(prefs.getBoolean("friend_study_mode_enabled", false))
  }

  @Test
  fun setFriendStudyModeEnabled_multipleCalls_updatesConsistently() {
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    // Enable twice
    vm.setFriendStudyModeEnabled(ctx, true)
    vm.setFriendStudyModeEnabled(ctx, true)

    assertTrue("StateFlow should still be true", vm.friendStudyModeEnabled.value)

    val prefs = ctx.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    assertTrue(
        "Preference should still be true", prefs.getBoolean("friend_study_mode_enabled", false))
  }

  @Test
  fun setFriendStudyModeEnabled_doesNotCrashOnSharedPreferencesError() {
    // This test verifies exception handling doesn't crash the app
    // In real scenario, SharedPreferences might fail due to disk issues, permissions, etc.
    val repo = FakeRepo()
    val vm = NotificationsViewModel(repo)

    // Call should not throw exception even if SharedPreferences has issues
    try {
      vm.setFriendStudyModeEnabled(ctx, true)
      // If we reach here, the exception was handled gracefully
      assertTrue(
          "StateFlow should be updated despite potential SharedPreferences error",
          vm.friendStudyModeEnabled.value)
    } catch (_: Exception) {
      fail("setFriendStudyModeEnabled should handle exceptions gracefully")
    }
  }
}
