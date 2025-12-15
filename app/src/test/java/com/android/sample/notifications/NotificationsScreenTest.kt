package com.android.sample.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.android.sample.data.notifications.NotificationUtils
import com.android.sample.model.notifications.NotificationKind
import com.android.sample.model.notifications.NotificationRepository
import com.android.sample.ui.notifications.NotificationsViewModel
import java.util.Calendar
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

class CapturingRepo : NotificationRepository {
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

  @Before
  fun setUp() {
    // Initialize WorkManager for tests
    val config = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.DEBUG).build()
    WorkManagerTestInitHelper.initializeTestWorkManager(ctx, config)
  }

  @Test
  fun renders_updates_time_and_applies_schedule() {
    val repo = CapturingRepo()
    val vm = NotificationsViewModel(repo)

    vm.setKickoffEnabled(ctx, true)
    vm.toggleKickoffDay(Calendar.MONDAY)
    vm.updateKickoffTime(Calendar.MONDAY, 7, 30)
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

    vm.scheduleTestNotification(ctx)
    assertEquals(1, repo.oneShot)
  }

  @Test
  fun demo_deep_link_posts_notification() {
    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadow = Shadows.shadowOf(nm)
    val before = shadow.allNotifications.size

    // Ensure channel exists
    NotificationUtils.ensureChannel(ctx)

    val vm = NotificationsViewModel()
    vm.sendDeepLinkDemoNotification(ctx)

    val after = shadow.allNotifications.size
    assertEquals(before + 1, after)
  }

  @Test
  fun friend_study_mode_toggle_enables() {
    val repo = CapturingRepo()
    val vm = NotificationsViewModel(repo)

    assertFalse("Should be disabled by default", vm.friendStudyModeEnabled.value)

    vm.setFriendStudyModeEnabled(ctx, true)

    assertTrue("Toggle should enable friend study mode", vm.friendStudyModeEnabled.value)
  }

  @Test
  fun friend_study_mode_toggle_disables() {
    val repo = CapturingRepo()
    val vm = NotificationsViewModel(repo)

    // First enable it
    vm.setFriendStudyModeEnabled(ctx, true)
    assertTrue(vm.friendStudyModeEnabled.value)

    // Then toggle off
    vm.setFriendStudyModeEnabled(ctx, false)

    assertFalse("Toggle should disable friend study mode", vm.friendStudyModeEnabled.value)
  }

  @Test
  fun friend_study_mode_toggle_persists_state() {
    val repo = CapturingRepo()
    val vm = NotificationsViewModel(repo)

    vm.setFriendStudyModeEnabled(ctx, true)

    val prefs = ctx.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    val enabled = prefs.getBoolean("friend_study_mode_enabled", false)

    assertTrue("Toggle state should be persisted", enabled)
  }
}
