package com.android.sample.ui.notifications

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.android.sample.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotificationsScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private class FakeVm(
      kickoffEnabledInit: Boolean = false,
      kickoffDaysInit: Set<Int> = emptySet(),
      kickoffTimesInit: Map<Int, Pair<Int, Int>> = emptyMap(),
      taskNotificationsEnabledInit: Boolean = false,
      streakEnabledInit: Boolean = false,
      campusEntryEnabledInit: Boolean = false,
      private var needsNotifPerm: Boolean = false,
      private var needsBgLocation: Boolean = false,
      private var hasBgLocation: Boolean = false,
  ) : NotificationsUiModel {
    private val _kickoffEnabled = MutableStateFlow(kickoffEnabledInit)
    override val kickoffEnabled: StateFlow<Boolean> = _kickoffEnabled
    private val _kickoffDays = MutableStateFlow(kickoffDaysInit)
    override val kickoffDays: StateFlow<Set<Int>> = _kickoffDays
    private val _kickoffTimes = MutableStateFlow(kickoffTimesInit)
    override val kickoffTimes: StateFlow<Map<Int, Pair<Int, Int>>> = _kickoffTimes
    private val _taskEnabled = MutableStateFlow(taskNotificationsEnabledInit)
    override val taskNotificationsEnabled: StateFlow<Boolean> = _taskEnabled
    private val _streakEnabled = MutableStateFlow(streakEnabledInit)
    override val streakEnabled: StateFlow<Boolean> = _streakEnabled
    private val _campusEnabled = MutableStateFlow(campusEntryEnabledInit)
    override val campusEntryEnabled: StateFlow<Boolean> = _campusEnabled

    // Call capture flags
    var requestOrScheduleCalled = false
    var scheduleTestNotificationCalled = false
    var sendDeepLinkCalled = false
    var startObservingCalled = false
    var setCampusEnabledLast: Boolean? = null

    override fun setKickoffEnabled(ctx: Context, on: Boolean) {
      _kickoffEnabled.value = on
    }

    override fun toggleKickoffDay(day: Int) {
      _kickoffDays.value =
          if (_kickoffDays.value.contains(day)) _kickoffDays.value - day
          else _kickoffDays.value + day
    }

    override fun updateKickoffTime(day: Int, hour: Int, minute: Int) {
      _kickoffTimes.value = _kickoffTimes.value.toMutableMap().apply { this[day] = hour to minute }
    }

    override fun applyKickoffSchedule(ctx: Context) {}

    override fun setStreakEnabled(ctx: Context, on: Boolean) {
      _streakEnabled.value = on
    }

    override fun scheduleTestNotification(ctx: Context) {
      scheduleTestNotificationCalled = true
    }

    override fun needsNotificationPermission(ctx: Context): Boolean = needsNotifPerm

    override fun requestOrSchedule(ctx: Context, permissionLauncher: (String) -> Unit) {
      requestOrScheduleCalled = true
    }

    override fun sendDeepLinkDemoNotification(ctx: Context) {
      sendDeepLinkCalled = true
    }

    override fun setTaskNotificationsEnabled(ctx: Context, on: Boolean) {
      _taskEnabled.value = on
    }

    override fun setCampusEntryEnabled(ctx: Context, on: Boolean) {
      _campusEnabled.value = on
      setCampusEnabledLast = on
    }

    override fun startObservingSchedule(ctx: Context) {
      startObservingCalled = true
    }

    override fun needsBackgroundLocationPermission(ctx: Context): Boolean = needsBgLocation

    override fun hasBackgroundLocationPermission(ctx: Context): Boolean = hasBgLocation

    override fun requestBackgroundLocationIfNeeded(ctx: Context, launcher: (String) -> Unit) {
      // Simulate immediate grant and enabling Campus feature in test environment
      hasBgLocation = true
      _campusEnabled.value = true
      setCampusEnabledLast = true
    }

    // Test helpers to flip internal flags
    fun setNeedsBgLocation(on: Boolean) {
      needsBgLocation = on
    }

    fun setHasBgLocation(on: Boolean) {
      hasBgLocation = on
    }

    fun setNeedsNotif(on: Boolean) {
      needsNotifPerm = on
    }
  }

  @Test
  fun title_and_buttons_render() {
    val vm = FakeVm()
    composeRule.setContent { MaterialTheme { NotificationsScreen(vm = vm, testMode = true) } }
    composeRule.waitUntil(5000) {
      composeRule.onAllNodes(hasTestTag("notifications_title")).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("notifications_title").assertIsDisplayed()
    composeRule.waitUntil(5000) {
      composeRule.onAllNodes(hasTestTag("btn_test_1_min")).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("btn_test_1_min").assertIsDisplayed()
    composeRule.waitUntil(5000) {
      composeRule.onAllNodes(hasTestTag("btn_demo_deep_link")).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("btn_demo_deep_link").assertIsDisplayed()
  }

  @Test
  fun test_notification_button_calls_requestOrSchedule() {
    val vm = FakeVm()
    composeRule.setContent { MaterialTheme { NotificationsScreen(vm = vm, testMode = true) } }
    composeRule.waitUntil(5000) {
      composeRule.onAllNodes(hasTestTag("btn_test_1_min")).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("btn_test_1_min").performClick()
    composeRule.runOnIdle { assertTrue(vm.requestOrScheduleCalled) }
  }

  @Test
  fun deep_link_button_calls_send_when_no_permission_needed() {
    val vm = FakeVm()
    vm.setNeedsNotif(false)
    composeRule.setContent { MaterialTheme { NotificationsScreen(vm = vm, testMode = true) } }
    composeRule.waitUntil(5000) {
      composeRule.onAllNodes(hasTestTag("btn_demo_deep_link")).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("btn_demo_deep_link").performClick()
    composeRule.runOnIdle { assertTrue(vm.sendDeepLinkCalled) }
  }

  @Test
  fun campus_toggle_shows_dialog_and_enables_after_confirm_and_grant() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val vm = FakeVm(campusEntryEnabledInit = false)
    vm.setNeedsBgLocation(true)
    vm.setHasBgLocation(false)

    composeRule.setContent { MaterialTheme { NotificationsScreen(vm = vm, testMode = true) } }

    composeRule.waitUntil(5000) {
      composeRule.onAllNodes(hasTestTag("campus_entry_switch")).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("campus_entry_switch").performClick()

    composeRule
        .onNodeWithText(ctx.getString(R.string.background_location_dialog_title))
        .assertIsDisplayed()

    composeRule.onNodeWithText(ctx.getString(R.string.grant_permission)).performClick()

    composeRule.runOnIdle { assertTrue(vm.setCampusEnabledLast == true) }
  }

  @Test
  fun startScheduleObserver_called_when_taskNotificationsEnabled_true_on_entry() {
    val vm = FakeVm(taskNotificationsEnabledInit = true)
    composeRule.setContent { MaterialTheme { NotificationsScreen(vm = vm, testMode = true) } }
    composeRule.runOnIdle { assertTrue(vm.startObservingCalled) }
  }
}
