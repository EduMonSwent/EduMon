package com.android.sample.notifications

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.sample.ui.notifications.NotificationsScreen
import com.android.sample.ui.notifications.NotificationsUiModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Instrumentation Compose test: verify that clicking the demo button calls the ViewModel
// method that would post the deep-link notification. We avoid device NotificationManager
// assertions here and instead assert the VM action, which is reliable in androidTest.
@RunWith(AndroidJUnit4::class)
@SmallTest
class NotificationsScreenComposeTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private class SpyNotificationsViewModel : NotificationsUiModel {
    // State flows with sensible defaults mirroring NotificationsViewModel behavior
    private val _kickoffEnabled = kotlinx.coroutines.flow.MutableStateFlow(true)
    override val kickoffEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = _kickoffEnabled

    private val _kickoffDays = kotlinx.coroutines.flow.MutableStateFlow(emptySet<Int>())
    override val kickoffDays: kotlinx.coroutines.flow.StateFlow<Set<Int>> = _kickoffDays

    private val _kickoffTimes =
        kotlinx.coroutines.flow.MutableStateFlow((1..7).associateWith { 9 to 0 })
    override val kickoffTimes: kotlinx.coroutines.flow.StateFlow<Map<Int, Pair<Int, Int>>> =
        _kickoffTimes

    private val _taskNotificationsEnabled = kotlinx.coroutines.flow.MutableStateFlow(true)
    override val taskNotificationsEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> =
        _taskNotificationsEnabled

    private val _streakEnabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val streakEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = _streakEnabled

    // spy flag
    var demoCalled = false

    override fun setKickoffEnabled(ctx: android.content.Context, on: Boolean) {
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

    override fun applyKickoffSchedule(ctx: android.content.Context) {
      // no-op for spy
    }

    override fun setStreakEnabled(ctx: android.content.Context, on: Boolean) {
      _streakEnabled.value = on
    }

    override fun scheduleTestNotification(ctx: android.content.Context) {
      // no-op
    }

    override fun needsNotificationPermission(ctx: android.content.Context): Boolean = false

    override fun requestOrSchedule(
        ctx: android.content.Context,
        permissionLauncher: (String) -> Unit
    ) {
      // directly schedule
    }

    override fun sendDeepLinkDemoNotification(ctx: android.content.Context) {
      demoCalled = true
    }

    override fun setTaskNotificationsEnabled(ctx: android.content.Context, on: Boolean) {
      _taskNotificationsEnabled.value = on
    }

    override fun startObservingSchedule(ctx: android.content.Context) {
      // no-op for spy
    }
  }

  @Test
  fun clicking_demo_button_calls_viewmodel_demo() {
    val vm = SpyNotificationsViewModel()

    composeRule.setContent { NotificationsScreen(vm = vm, onBack = {}, onGoHome = {}) }

    // Click the demo deep-link button and assert VM was invoked
    composeRule.onNodeWithTag("btn_demo_deep_link").performClick()
    composeRule.runOnIdle { assertTrue(vm.demoCalled) }
  }
}
