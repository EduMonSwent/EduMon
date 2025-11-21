package com.android.sample.ui.notifications

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * JVM Compose UI tests for NotificationsScreen These run under Robolectric and contribute to JaCoCo
 * unit test coverage.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationsScreenUnitTest {

  @get:Rule val composeRule = createComposeRule()

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

    var requestOrScheduleCalled = false
    var sendDeepLinkCalled = false
    var startObservingCalled = false
    var setCampusEnabledLast: Boolean? = null

    override fun setKickoffEnabled(ctx: Context, on: Boolean) {
      _kickoffEnabled.value = on
    }

    override fun toggleKickoffDay(day: Int) {}

    override fun updateKickoffTime(day: Int, hour: Int, minute: Int) {}

    override fun applyKickoffSchedule(ctx: Context) {}

    override fun setStreakEnabled(ctx: Context, on: Boolean) {
      _streakEnabled.value = on
    }

    override fun scheduleTestNotification(ctx: Context) {}

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
      hasBgLocation = true
      _campusEnabled.value = true
      setCampusEnabledLast = true
    }

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
    composeRule.onNodeWithTag("notifications_title", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithTag("btn_test_1_min", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithTag("btn_demo_deep_link", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun test_notification_button_calls_requestOrSchedule() {
    val vm = FakeVm()
    composeRule.setContent { MaterialTheme { NotificationsScreen(vm = vm, testMode = true) } }
    composeRule.onNodeWithTag("btn_test_1_min", useUnmergedTree = true).performClick()
    composeRule.waitForIdle()
    assertTrue(vm.requestOrScheduleCalled)
  }

  @Test
  fun deep_link_button_calls_send_when_no_permission_needed() {
    val vm = FakeVm()
    vm.setNeedsNotif(false)
    composeRule.setContent { MaterialTheme { NotificationsScreen(vm = vm, testMode = true) } }
    composeRule.onNodeWithTag("btn_demo_deep_link", useUnmergedTree = true).performClick()
    composeRule.waitForIdle()
    assertTrue(vm.sendDeepLinkCalled)
  }

  @Test
  fun campus_toggle_shows_dialog_and_enables_after_confirm_and_grant() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val vm =
        FakeVm(campusEntryEnabledInit = false).apply {
          setNeedsBgLocation(true)
          setHasBgLocation(false)
        }
    composeRule.setContent { MaterialTheme { NotificationsScreen(vm = vm, testMode = true) } }

    composeRule.onNodeWithTag("campus_entry_switch", useUnmergedTree = true).performClick()
    composeRule
        .onNodeWithText(ctx.getString(R.string.background_location_dialog_title))
        .assertIsDisplayed()
    composeRule.onNodeWithText(ctx.getString(R.string.grant_permission)).performClick()
    composeRule.waitForIdle()
    assertTrue(vm.setCampusEnabledLast == true)
  }

  @Test
  fun startScheduleObserver_called_when_taskNotificationsEnabled_true_on_entry() {
    val vm = FakeVm(taskNotificationsEnabledInit = true)
    composeRule.setContent { MaterialTheme { NotificationsScreen(vm = vm, testMode = true) } }
    composeRule.waitForIdle()
    assertTrue(vm.startObservingCalled)
  }
}
