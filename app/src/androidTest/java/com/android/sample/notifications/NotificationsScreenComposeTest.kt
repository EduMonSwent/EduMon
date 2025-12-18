package com.android.sample.notifications

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.sample.EduMonNavHost
import com.android.sample.NavigationTestTags
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.notifications.NotificationsScreen
import com.android.sample.ui.notifications.NotificationsUiModel
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Parts of this code were written with ChatGPT assistance

// Instrumentation Compose test: verify that clicking the demo button calls the ViewModel
// method that would post the deep-link notification. We avoid device NotificationManager
// assertions here and instead assert the VM action, which is reliable in androidTest.
@RunWith(AndroidJUnit4::class)
@SmallTest
class NotificationsScreenComposeTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private var originalRepositories = AppRepositories

  @Before
  fun setUp() {
    // Use fake repositories to avoid Firestore crashes in CI (no logged-in user)
    AppRepositories = FakeRepositoriesProvider
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepositories
  }

  private class SpyNotificationsViewModel : NotificationsUiModel {
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

    private val _friendStudyModeEnabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val friendStudyModeEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> =
        _friendStudyModeEnabled

    private val _campusEntryEnabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val campusEntryEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> =
        _campusEntryEnabled

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

    override fun setCampusEntryEnabled(ctx: android.content.Context, on: Boolean) {
      _campusEntryEnabled.value = on
    }

    override fun setFriendStudyModeEnabled(ctx: android.content.Context, on: Boolean) {
      _friendStudyModeEnabled.value = on
    }

    override fun startObservingSchedule(ctx: android.content.Context) {
      // no-op for spy
    }
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun clicking_demo_button_calls_viewmodel_demo() {
    val vm = SpyNotificationsViewModel()

    composeRule.setContent { NotificationsScreen(vm = vm, onBack = {}, onGoHome = {}) }

    // Wait until the button actually appears and is clickable
    composeRule.waitUntilExactlyOneExists(hasTestTag("btn_demo_deep_link"), timeoutMillis = 5_000)

    // Scroll into view if it's inside a LazyColumn
    composeRule
        .onNodeWithTag("btn_demo_deep_link", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    // Wait until recomposition completes
    composeRule.waitForIdle()

    // Assert after Compose settles
    composeRule.runOnIdle { assertTrue("Demo button should trigger ViewModel call", vm.demoCalled) }
  }

  @Test
  fun renders_study_route_with_event_id() {
    // Use startDestination to navigate directly to study route instead of calling navigate()
    // This avoids the "Navigation graph has not been set" error
    composeRule.setContent { EduMonNavHost(startDestination = "study/test-event") }

    // Wait for composition
    composeRule.waitForIdle()

    // Verify top bar appears (so Scaffold + StudySessionScreen executed)
    composeRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertExists("Expected study top bar to exist")

    // Optionally assert back button too
    composeRule
        .onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON)
        .assertExists("Expected back button to exist")
  }
}
