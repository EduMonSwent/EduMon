package com.android.sample.ui.notifications

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationsUnifiedTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  // Fake ViewModel used for flows tests
  private class FakeVm : NotificationsUiModel {
    val _kickoffEnabled = MutableStateFlow(true)
    val _kickoffDays = MutableStateFlow(setOf<Int>())
    val _kickoffTimes = MutableStateFlow(mapOf<Int, Pair<Int, Int>>())
    val _taskEnabled = MutableStateFlow(false)
    val _streakEnabled = MutableStateFlow(false)
    val _campusEnabled = MutableStateFlow(false)
    var needsBg = true
    var hasBg = false
    var needsNotif = false
    var lastUpdateKickoff: Pair<Int, Pair<Int, Int>>? = null
    var requestedBgPermission: String? = null
    var scheduleObservedCalls = 0
    var scheduleObservedThrows = false
    var deepLinkDemoCalls = 0
    var testNotificationCalls = 0
    var campusToggleCalls = mutableListOf<Boolean>()
    override val kickoffEnabled: StateFlow<Boolean> = _kickoffEnabled
    override val kickoffDays: StateFlow<Set<Int>> = _kickoffDays
    override val kickoffTimes: StateFlow<Map<Int, Pair<Int, Int>>> = _kickoffTimes
    override val taskNotificationsEnabled: StateFlow<Boolean> = _taskEnabled
    override val streakEnabled: StateFlow<Boolean> = _streakEnabled
    override val campusEntryEnabled: StateFlow<Boolean> = _campusEnabled

    override fun setKickoffEnabled(ctx: Context, enabled: Boolean) {
      _kickoffEnabled.value = enabled
    }

    override fun toggleKickoffDay(day: Int) {
      _kickoffDays.value =
          if (_kickoffDays.value.contains(day)) _kickoffDays.value - day
          else _kickoffDays.value + day
    }

    override fun updateKickoffTime(day: Int, hour: Int, minute: Int) {
      _kickoffTimes.value = _kickoffTimes.value + (day to (hour to minute))
      lastUpdateKickoff = day to (hour to minute)
    }

    override fun applyKickoffSchedule(ctx: Context) {}

    override fun setStreakEnabled(ctx: Context, enabled: Boolean) {
      _streakEnabled.value = enabled
    }

    override fun setTaskNotificationsEnabled(ctx: Context, enabled: Boolean) {
      _taskEnabled.value = enabled
    }

    override fun setCampusEntryEnabled(ctx: Context, enabled: Boolean) {
      _campusEnabled.value = enabled
      campusToggleCalls.add(enabled)
    }

    override fun needsBackgroundLocationPermission(ctx: Context): Boolean = needsBg

    override fun hasBackgroundLocationPermission(ctx: Context): Boolean = hasBg

    override fun requestBackgroundLocationIfNeeded(ctx: Context, request: (String) -> Unit) {
      request("android.permission.ACCESS_BACKGROUND_LOCATION")
    }

    override fun needsNotificationPermission(ctx: Context): Boolean = needsNotif

    override fun requestOrSchedule(ctx: Context, request: (String) -> Unit) {
      testNotificationCalls++
    }

    override fun scheduleTestNotification(ctx: Context) {
      testNotificationCalls++
    }

    override fun sendDeepLinkDemoNotification(ctx: Context) {
      deepLinkDemoCalls++
    }

    override fun startObservingSchedule(ctx: Context) {
      scheduleObservedCalls++
      if (scheduleObservedThrows) throw IllegalStateException("boom")
    }
  }

  /* ---------------- KickoffSection tests ---------------- */
  @Test
  fun kickoff_empty_shows_hint_hides_apply() {
    composeRule.setContent {
      KickoffSection(
          kickoffEnabled = true,
          kickoffDays = emptySet(),
          kickoffTimes = emptyMap(),
          onToggleKickoff = {},
          onToggleDay = {},
          onPickRequest = {},
          onApply = {})
    }
    val hintText = composeRule.activity.getString(R.string.select_days_set_times)
    composeRule.onNodeWithTag("kickoff_empty_hint").assertIsDisplayed()
    composeRule.onNodeWithText(hintText).assertIsDisplayed()
    composeRule.onNodeWithTag("btn_apply_kickoff").assertDoesNotExist()
  }

  @Test
  fun kickoff_with_days_shows_time_chips_and_apply() {
    composeRule.setContent {
      val times = mapOf(Calendar.MONDAY to (7 to 30), Calendar.WEDNESDAY to (9 to 0))
      KickoffSection(
          kickoffEnabled = true,
          kickoffDays = setOf(Calendar.MONDAY, Calendar.WEDNESDAY),
          kickoffTimes = times,
          onToggleKickoff = {},
          onToggleDay = {},
          onPickRequest = {},
          onApply = {})
    }
    val applyText = composeRule.activity.getString(R.string.apply_kickoff_schedule)
    composeRule.onNodeWithTag("btn_apply_kickoff").assertIsDisplayed()
    composeRule.onNodeWithText(applyText).assertIsDisplayed()
    composeRule.onNodeWithText("07:30", substring = true).assertIsDisplayed()
  }

  /* ---------------- CampusEntrySection / dialog flows ---------------- */
  @Test
  fun campusEntry_banner_and_dialog_flow_requests_permission() {
    val vm =
        FakeVm().apply {
          _campusEnabled.value = true
          needsBg = true
          hasBg = false
        }
    var permissionRequested = false
    val initialEnabled = vm.campusEntryEnabled.value
    composeRule.setContent {
      CampusEntrySection(
          enabled = initialEnabled,
          onToggle = { enabled -> vm.setCampusEntryEnabled(composeRule.activity, enabled) },
          vm = vm,
          ctx = composeRule.activity,
          requestBackgroundPermission = { permission ->
            permissionRequested = true
            vm.requestedBgPermission = permission
          })
    }
    val neededText = composeRule.activity.getString(R.string.campus_background_location_needed)
    composeRule.onNodeWithText(neededText).assertIsDisplayed()
    val grantText = composeRule.activity.getString(R.string.grant_permission)
    // If multiple nodes have the same text (banner + dialog), choose nodes explicitly
    val grantNodes = composeRule.onAllNodesWithText(grantText)
    // Click the banner's grant button (first occurrence)
    grantNodes[0].assertIsDisplayed().performClick()

    // Dialog should appear; wait and click the dialog's grant button (second occurrence)
    composeRule.waitForIdle()
    if (grantNodes.fetchSemanticsNodes().size > 1) {
      grantNodes[1].assertIsDisplayed().performClick()
    }

    assertTrue(permissionRequested)
    assertEquals("android.permission.ACCESS_BACKGROUND_LOCATION", vm.requestedBgPermission)
  }

  @Test
  fun campusEntry_effect_enables_toggle_when_permission_present() {
    val vm =
        FakeVm().apply {
          _campusEnabled.value = false
          needsBg = true
          hasBg = true
        }
    var enabledResult = false
    val initialEnabled = vm.campusEntryEnabled.value
    composeRule.setContent {
      CampusEntrySection(
          enabled = initialEnabled,
          onToggle = { enabled -> enabledResult = enabled },
          vm = vm,
          ctx = composeRule.activity,
          requestBackgroundPermission = { _ -> })
    }
    composeRule.onNodeWithTag("campus_entry_switch").performClick()
    val title = composeRule.activity.getString(R.string.background_location_dialog_title)
    composeRule.onNodeWithText(title).assertIsDisplayed()
    composeRule.waitForIdle()
    assertTrue(enabledResult)
  }

  /* ---------------- StartScheduleObserver error path ---------------- */
  @Test
  fun startScheduleObserver_error_shows_banner() {
    val vm =
        FakeVm().apply {
          scheduleObservedThrows = true
          _taskEnabled.value = true
        }
    composeRule.setContent {
      var startupError: String? by remember { mutableStateOf<String?>(null) }
      StartScheduleObserver(true, vm, composeRule.activity) { startupError = it }
      StartupErrorBanner(startupError)
    }
    // Wait for Compose to run effects and update UI
    composeRule.waitForIdle()
    val expectedPrefix = composeRule.activity.getString(R.string.notification_setup_error_fmt, "")
    composeRule.onNodeWithText(expectedPrefix, substring = true).assertIsDisplayed()
  }

  /* ---------------- Deep link demo button ---------------- */
  @Test
  fun deepLink_demo_button_calls_send_no_permission_needed() {
    val vm = FakeVm().apply { needsNotif = false }
    composeRule.setContent { DeepLinkDemoButton(vm, composeRule.activity) { _ -> } }
    val text = composeRule.activity.getString(R.string.send_deep_link_demo)
    composeRule.onNodeWithText(text).assertIsDisplayed().performClick()
    assertEquals(1, vm.deepLinkDemoCalls)
  }

  /* ---------------- TimePickerDialog via NotificationsScreen ---------------- */
  @Test
  fun timePicker_forced_day_updates_on_confirm() {
    val vm =
        FakeVm().apply {
          _kickoffEnabled.value = true
          _kickoffDays.value = setOf(Calendar.MONDAY)
        }
    composeRule.setContent {
      NotificationsScreen(vm = vm, testMode = true, forceDialogForDay = Calendar.MONDAY)
    }
    val okText = composeRule.activity.getString(R.string.ok)
    composeRule.onNodeWithText(okText).assertIsDisplayed().performClick()
    assertEquals(Calendar.MONDAY, vm.lastUpdateKickoff?.first)
  }

  /* ---------------- formatDayTimeLabelLocalized tests (label logic) ---------------- */
  @Test
  fun formatDayTimeLabelLocalized_usesProvidedTime_andLocalizedDay() {
    val times = mapOf(Calendar.MONDAY to (7 to 5))
    var label = ""
    composeRule.setContent { label = formatDayTimeLabelLocalized(Calendar.MONDAY, times) }
    composeRule.waitForIdle()
    val expectedDay = composeRule.activity.getString(R.string.day_short_mon)
    assertEquals("$expectedDay 07:05", label)
  }

  @Test
  fun formatDayTimeLabelLocalized_fallbacksToDefaultNineAM_whenMissing() {
    val times = emptyMap<Int, Pair<Int, Int>>()
    var label = ""
    composeRule.setContent { label = formatDayTimeLabelLocalized(Calendar.FRIDAY, times) }
    composeRule.waitForIdle()
    val expectedDay = composeRule.activity.getString(R.string.day_short_fri)
    assertEquals("$expectedDay 09:00", label)
  }

  @Test
  fun formatDayTimeLabelLocalized_coercesHourAndMinuteIntoBounds() {
    val times = mapOf(Calendar.SUNDAY to (99 to -5))
    var label = ""
    composeRule.setContent { label = formatDayTimeLabelLocalized(Calendar.SUNDAY, times) }
    composeRule.waitForIdle()
    val expectedDay = composeRule.activity.getString(R.string.day_short_sun)
    assertEquals("$expectedDay 23:00", label)
  }

  /* ---------------- StartupErrorBanner direct tests ---------------- */
  @Test
  fun startupErrorBanner_shows_error_text() {
    composeRule.setContent { StartupErrorBanner(startupError = "Cannot init notifications") }
    val expected =
        composeRule.activity.getString(
            R.string.notification_setup_error_fmt, "Cannot init notifications")
    composeRule.onNodeWithText(expected).assertIsDisplayed()
  }

  @Test
  fun startupErrorBanner_shows_nothing_when_null() {
    composeRule.setContent { StartupErrorBanner(startupError = null) }
    val unexpected =
        composeRule.activity.getString(R.string.notification_setup_error_fmt, "irrelevant")
    composeRule.onNodeWithText(unexpected, substring = true).assertDoesNotExist()
  }
}
