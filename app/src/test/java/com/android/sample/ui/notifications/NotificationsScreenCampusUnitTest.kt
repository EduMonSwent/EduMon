package com.android.sample.ui.notifications

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
@Ignore("Runs only in debug variant as Compose UI unit test")
class NotificationsScreenCampusUnitTest {

  @get:Rule val composeRule = createComposeRule()

  private class FakeVm(
      kickoffEnabledInit: Boolean = false,
      kickoffDaysInit: Set<Int> = emptySet(),
      kickoffTimesInit: Map<Int, Pair<Int, Int>> = emptyMap(),
      taskNotificationsEnabledInit: Boolean = false,
      streakEnabledInit: Boolean = false,
      campusEntryEnabledInit: Boolean = false
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
    private val _campusEntryEnabled = MutableStateFlow(campusEntryEnabledInit)
    override val campusEntryEnabled: StateFlow<Boolean> = _campusEntryEnabled
    var lastCampusToggle: Boolean? = null

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

    override fun scheduleTestNotification(ctx: Context) {}

    override fun needsNotificationPermission(ctx: Context): Boolean = false

    override fun requestOrSchedule(ctx: Context, permissionLauncher: (String) -> Unit) {}

    override fun sendDeepLinkDemoNotification(ctx: Context) {}

    override fun setTaskNotificationsEnabled(ctx: Context, on: Boolean) {
      _taskEnabled.value = on
    }

    override fun setCampusEntryEnabled(ctx: Context, on: Boolean) {
      _campusEntryEnabled.value = on
      lastCampusToggle = on
    }

    override fun startObservingSchedule(ctx: Context) {}

    override fun needsBackgroundLocationPermission(ctx: Context): Boolean = false

    override fun hasBackgroundLocationPermission(ctx: Context): Boolean = true

    override fun requestBackgroundLocationIfNeeded(ctx: Context, launcher: (String) -> Unit) {}
  }

  @Composable
  private fun Screen(vm: NotificationsUiModel) = NotificationsScreen(vm = vm, testMode = true)

  @Test
  fun campusSwitch_shows() {
    val vm = FakeVm()
    composeRule.setContent { Screen(vm) }
    val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
    composeRule
        .onNodeWithText(ctx.getString(R.string.campus_entry_toggle_title))
        .assertIsDisplayed()
    composeRule.onNodeWithTag("campus_entry_switch").assertIsOff()
  }

  @Test
  fun campusSwitch_enableClick_updatesVm() {
    val vm = FakeVm(campusEntryEnabledInit = false)
    composeRule.setContent { Screen(vm) }
    composeRule.onNodeWithTag("campus_entry_switch").performClick()
    composeRule.runOnIdle { assert(vm.lastCampusToggle == true) }
    composeRule.onNodeWithTag("campus_entry_switch").assertIsOn()
  }

  @Test
  fun campusSwitch_disableClick_updatesVm() {
    val vm = FakeVm(campusEntryEnabledInit = true)
    composeRule.setContent { Screen(vm) }
    composeRule.onNodeWithTag("campus_entry_switch").assertIsOn()
    composeRule.onNodeWithTag("campus_entry_switch").performClick()
    composeRule.runOnIdle { assert(vm.lastCampusToggle == false) }
    composeRule.onNodeWithTag("campus_entry_switch").assertIsOff()
  }
}
