package com.android.sample.ui.notifications

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationsScreenCampusTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private class FakeNotificationsVm(
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

    private val _taskNotificationsEnabled = MutableStateFlow(taskNotificationsEnabledInit)
    override val taskNotificationsEnabled: StateFlow<Boolean> = _taskNotificationsEnabled

    private val _streakEnabled = MutableStateFlow(streakEnabledInit)
    override val streakEnabled: StateFlow<Boolean> = _streakEnabled

    private val _campusEntryEnabled = MutableStateFlow(campusEntryEnabledInit)
    override val campusEntryEnabled: StateFlow<Boolean> = _campusEntryEnabled

    // record last campus toggle request
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
      _taskNotificationsEnabled.value = on
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
  private fun ScreenWithStrings(vm: NotificationsUiModel) {
    NotificationsScreen(vm = vm)
  }

  @Test
  fun campusEntryToggle_isDisplayed() {
    val vm = FakeNotificationsVm()
    composeTestRule.setContent { ScreenWithStrings(vm) }
    val ctx = composeTestRule.activity
    composeTestRule
        .onNodeWithText(ctx.getString(R.string.campus_entry_toggle_title))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(ctx.getString(R.string.campus_entry_toggle_subtitle))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag("campus_entry_switch").assertIsDisplayed()
  }

  @Test
  fun campusEntryToggle_clickingCallsViewModel_enable() {
    val vm = FakeNotificationsVm(campusEntryEnabledInit = false)
    composeTestRule.setContent { ScreenWithStrings(vm) }
    composeTestRule.onNodeWithTag("campus_entry_switch").performClick()
    composeTestRule.runOnIdle { assert(vm.lastCampusToggle == true) { "Expected enable toggle" } }
  }

  @Test
  fun campusEntryToggle_reflectsEnabledState() {
    val vm = FakeNotificationsVm(campusEntryEnabledInit = true)
    composeTestRule.setContent { ScreenWithStrings(vm) }
    composeTestRule.onNodeWithTag("campus_entry_switch").assertIsOn()
  }

  @Test
  fun campusEntryToggle_reflectsDisabledState() {
    val vm = FakeNotificationsVm(campusEntryEnabledInit = false)
    composeTestRule.setContent { ScreenWithStrings(vm) }
    composeTestRule.onNodeWithTag("campus_entry_switch").assertIsOff()
  }

  @Test
  fun campusEntrySection_showsDescriptionText() {
    val vm = FakeNotificationsVm()
    composeTestRule.setContent { ScreenWithStrings(vm) }
    val ctx = composeTestRule.activity
    composeTestRule.onNodeWithText(ctx.getString(R.string.campus_entry_text)).assertIsDisplayed()
  }

  @Test
  fun campusEntryToggle_disablingCallsViewModelFalse() {
    val vm = FakeNotificationsVm(campusEntryEnabledInit = true)
    composeTestRule.setContent { ScreenWithStrings(vm) }
    composeTestRule.onNodeWithTag("campus_entry_switch").performClick()
    composeTestRule.runOnIdle { assert(vm.lastCampusToggle == false) { "Expected disable toggle" } }
  }
}
