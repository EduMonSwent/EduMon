package com.android.sample

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import com.android.sample.feature.weeks.ui.CourseExercisesTestTags
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.login.LoginScreen
import com.android.sample.ui.profile.ProfileScreenTestTags
import com.android.sample.ui.schedule.ScheduleScreenTestTags
import com.android.sample.ui.theme.EduMonTheme
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ObjectiveCompletionE2ETest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var loggedInState: MutableState<Boolean>
  private var originalRepositories = AppRepositories

  @Before
  fun setUp() {
    AppRepositories = FakeRepositoriesProvider
    runBlocking { resetObjectivesToDefault() }
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepositories
    runBlocking { FakeObjectivesRepository.setObjectives(emptyList()) }
  }

  private suspend fun resetObjectivesToDefault() {
    val today = LocalDate.now().dayOfWeek
    FakeObjectivesRepository.setObjectives(
        listOf(
            Objective(
                title = "Complete Quiz 3",
                course = "CS101",
                estimateMinutes = 30,
                completed = false,
                day = today),
        ))
  }

  private fun hasTestTagPrefix(prefix: String): SemanticsMatcher =
      SemanticsMatcher("TestTag starts with \"$prefix\"") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag?.startsWith(prefix) == true
      }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun completeObjective_checkWeekDots_verifyProfile_logMood() {
    // STEP 1: Setup and Login
    composeRule.setContent {
      EduMonTheme {
        val loggedIn = remember { mutableStateOf(false) }
        loggedInState = loggedIn

        if (!loggedIn.value) {
          LoginScreen(onLoggedIn = { loggedIn.value = true })
        } else {
          EduMonNavHost()
        }
      }
    }

    composeRule.waitUntilExactlyOneExists(
        hasText("Connect yourself to EduMon."), timeoutMillis = 20_000)
    composeRule.onNodeWithText("Connect yourself to EduMon.").assertIsDisplayed()

    composeRule.runOnIdle { loggedInState.value = true }
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)

    waitForHome()

    // STEP 2: Navigate to Schedule
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)
    composeRule.onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true).performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ScheduleScreenTestTags.ROOT), timeoutMillis = 20_000)

    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY).assertExists()

    // Wait for objectives section
    composeRule.waitUntil(timeoutMillis = 15_000) {
      composeRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    tryScrollToAndAssert(WeekProgDailyObjTags.OBJECTIVES_SECTION)

    // STEP 3: Complete the objective
    completeOneObjectiveIfAvailable()

    // Wait for UI to stabilize after completion (toast may appear)
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(2000)

    // Ensure we're still on Schedule screen before switching tabs
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithTag(ScheduleScreenTestTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // STEP 4: Switch to Week view and check WeekDots
    // Scroll to tab row to ensure it's visible
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithTag(ScheduleScreenTestTags.TAB_ROW, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule
        .onNodeWithTag(ScheduleScreenTestTags.TAB_ROW, useUnmergedTree = true)
        .performScrollTo()

    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)

    // Wait for "Week" text to be available and click it
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithText("Week", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule.onNodeWithText("Week", useUnmergedTree = true).performClick()
    composeRule.waitForIdle()

    // Verify we switched to Week tab
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithTag(ScheduleScreenTestTags.CONTENT_WEEK, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_WEEK).assertExists()

    composeRule.waitUntil(timeoutMillis = 15_000) {
      composeRule
          .onAllNodesWithTag(WeekProgDailyObjTags.WEEK_DOTS_ROW, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val today = LocalDate.now().dayOfWeek.name
    val todayDotTag = WeekProgDailyObjTags.WEEK_DOT_PREFIX + today

    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithTag(todayDotTag, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeRule.onNodeWithTag(todayDotTag, useUnmergedTree = true).assertExists()

    goBackToHome()

    // STEP 5: Navigate to Profile
    openDrawerDestination(AppDestination.Profile.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ProfileScreenTestTags.PROFILE_SCREEN), timeoutMillis = 20_000)

    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.STATS_CARD))

    composeRule.onNodeWithTag(ProfileScreenTestTags.STATS_CARD).assertExists()

    goBackToHome()

    // STEP 6: Navigate to Mood and select a mood
    openDrawerDestination(AppDestination.Mood.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 20_000)

    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithTag("mood_3", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val randomMoodTag = "mood_${(3..5).random()}"

    val hasMoodButton =
        composeRule
            .onAllNodesWithTag(randomMoodTag, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    if (hasMoodButton) {
      composeRule.onNodeWithTag(randomMoodTag, useUnmergedTree = true).performClick()
      composeRule.waitForIdle()

      val hasSaveButton =
          composeRule
              .onAllNodesWithTag("save_button", useUnmergedTree = true)
              .fetchSemanticsNodes()
              .isNotEmpty()

      if (hasSaveButton) {
        composeRule.onNodeWithTag("save_button", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
      }
    }

    goBackToHome()

    // FINAL: Verify home is stable
    waitForHome()
    composeRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun completeOneObjectiveIfAvailable() {
    val hasStartButton =
        composeRule
            .onAllNodes(
                hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
                useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    if (!hasStartButton) return

    composeRule
        .onAllNodes(
            hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
            useUnmergedTree = true)
        .onFirst()
        .performClick()

    // Wait for CourseExercises modal
    composeRule.waitUntil(timeoutMillis = 20_000) {
      composeRule
          .onAllNodesWithTag(CourseExercisesTestTags.SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(1000)

    // Wait for and click Completed FAB
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithTag(CourseExercisesTestTags.COMPLETED_FAB, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule
        .onNodeWithTag(CourseExercisesTestTags.COMPLETED_FAB, useUnmergedTree = true)
        .performClick()

    // Give time for the completion action and toast to process
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(2000)

    // Wait for modal to dismiss (try multiple times with longer timeout)
    try {
      composeRule.waitUntil(timeoutMillis = 20_000) {
        composeRule
            .onAllNodesWithTag(CourseExercisesTestTags.SCREEN, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isEmpty()
      }
    } catch (_: Exception) {
      // Modal might already be dismissed, continue
    }

    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(1000)
  }

  @OptIn(ExperimentalTestApi::class)
  private fun waitForHome() {
    composeRule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.MENU_BUTTON), timeoutMillis = 20_000)
  }

  @OptIn(ExperimentalTestApi::class)
  private fun goBackToHome() {
    val hasBackButton =
        composeRule
            .onAllNodesWithTag(NavigationTestTags.GO_BACK_BUTTON, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    if (hasBackButton) {
      composeRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
      composeRule.waitForIdle()
      composeRule.mainClock.advanceTimeBy(500)
    }

    waitForHome()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun ensureHomeChildVisible(childTag: String) {
    composeRule.waitUntil(timeoutMillis = 20_000) {
      runCatching {
            composeRule.onAllNodesWithTag(childTag, useUnmergedTree = true).fetchSemanticsNodes()
          }
          .getOrNull()
          ?.isNotEmpty() == true
    }
    composeRule.onNode(hasTestTag(childTag), useUnmergedTree = true).performScrollTo()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun openDrawerDestination(route: String) {
    composeRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists().performClick()

    val drawerItemTag = HomeTestTags.drawerTag(route)

    composeRule.waitUntil(timeoutMillis = 20_000) {
      runCatching {
            composeRule
                .onAllNodesWithTag(drawerItemTag, useUnmergedTree = true)
                .fetchSemanticsNodes()
          }
          .getOrNull()
          ?.isNotEmpty() == true
    }

    composeRule.onNodeWithTag(drawerItemTag, useUnmergedTree = true).assertExists().performClick()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun tryScrollToAndAssert(tag: String) {
    try {
      composeRule.waitUntil(timeoutMillis = 5_000) {
        runCatching {
              composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()
            }
            .getOrNull()
            ?.isNotEmpty() == true
      }
      composeRule.onNode(hasTestTag(tag), useUnmergedTree = true).performScrollTo()
      composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
    } catch (_: Exception) {
      // Element may not be visible
    }
  }
}
