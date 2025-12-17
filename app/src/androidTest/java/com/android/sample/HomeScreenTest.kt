package com.android.sample

import android.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.data.CreatureStats
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.data.UserStats
import com.android.sample.feature.homeScreen.EduMonHomeScreen
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.feature.homeScreen.HomeUiState
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.theme.EduMonTheme
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private var originalRepositories = AppRepositories

  @Before
  fun setUp() {
    originalRepositories = AppRepositories
    AppRepositories = FakeRepositoriesProvider
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepositories
  }

  private fun setHomeContent(quote: String = "Keep going.", onNavigate: (String) -> Unit = {}) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    composeRule.setContent {
      MaterialTheme {
        EduMonHomeScreen(
            state =
                HomeUiState(
                    isLoading = false,
                    todos =
                        listOf(
                            // DONE should NOT appear in "To-dos (Pending)" slide
                            ToDo(
                                id = "1",
                                title = "CS-101: Finish exercise sheet",
                                dueDate = today,
                                priority = Priority.HIGH,
                                status = Status.DONE),
                            ToDo(
                                id = "2",
                                title = "Math review: sequences",
                                dueDate = today,
                                priority = Priority.MEDIUM),
                            ToDo(
                                id = "3",
                                title = "Pack lab kit for tomorrow",
                                dueDate = tomorrow,
                                priority = Priority.LOW)),
                    objectives =
                        listOf(
                            Objective(
                                title = "Revise Week 3 – Calculus",
                                course = "Math",
                                estimateMinutes = 45,
                                completed = false,
                                day = DayOfWeek.MONDAY),
                            Objective(
                                title = "Quiz practice – Algorithms basics",
                                course = "CS-101",
                                estimateMinutes = 25,
                                completed = false,
                                day = DayOfWeek.WEDNESDAY),
                            // completed=true should NOT appear (filtered out)
                            Objective(
                                title = "Write resume draft",
                                course = "Career",
                                estimateMinutes = 30,
                                completed = true,
                                day = DayOfWeek.FRIDAY)),
                    creatureStats =
                        CreatureStats(happiness = 85, health = 90, energy = 70, level = 5),
                    userStats =
                        UserStats(
                            totalStudyMinutes = 100,
                            todayStudyMinutes = 45,
                            streak = 7,
                            weeklyGoal = 180,
                            coins = 0,
                            points = 1250,
                            lastStudyDateEpochDay = LocalDate.now().toEpochDay()),
                    quote = quote),
            creatureResId = R.drawable.ic_menu_help,
            environmentResId = R.drawable.ic_menu_gallery,
            onNavigate = onNavigate)
      }
    }
  }

  @Test
  fun home_showsCoreSections_andCreature_andCarouselFirstPage() {
    setHomeContent()

    // Core sections
    composeRule.onNodeWithText("Affirmation").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Your Stats").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Quick Actions").performScrollTo().assertIsDisplayed()

    // Creature (depends on CreatureHouseCard semantics)
    composeRule
        .onNodeWithContentDescription("Creature environment")
        .performScrollTo()
        .assertExists()

    // Level text (depends on CreatureHouseCard rendering)
    composeRule.onNodeWithText("Lv 5").performScrollTo().assertIsDisplayed()

    // Carousel exists + first page header
    composeRule.onNodeWithTag(HomeTestTags.CAROUSEL_CARD).performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("To-dos (Pending)").assertIsDisplayed()

    // Pending todos shown (2 items)
    composeRule.onNodeWithText("Math review: sequences").assertIsDisplayed()
    composeRule.onNodeWithText("Pack lab kit for tomorrow").assertIsDisplayed()

    // DONE todo must not be displayed in pending list
    composeRule.onNodeWithText("CS-101: Finish exercise sheet").assertDoesNotExist()
  }

  @Test
  fun carousel_swipe_showsObjectives_andFiltersCompletedObjectives() {
    setHomeContent()

    // ✅ Swipe the pager, not the card
    composeRule
        .onNodeWithTag(HomeTestTags.CAROUSEL_PAGER, useUnmergedTree = true)
        .performScrollTo()
        .performTouchInput { swipeLeft() }

    composeRule.waitForIdle()

    // Objectives page visible
    composeRule.onNodeWithText("Objectives").assertIsDisplayed()

    // Two pending objectives displayed
    composeRule.onNodeWithText("Revise Week 3 – Calculus").assertIsDisplayed()
    composeRule.onNodeWithText("Quiz practice – Algorithms basics").assertIsDisplayed()

    // Completed objective filtered out
    composeRule.onNodeWithText("Write resume draft").assertDoesNotExist()
  }

  @Test
  fun showsProvidedQuote_andAffirmationChips() {
    setHomeContent(quote = "Test Quote 123")

    composeRule.onNodeWithText("Test Quote 123").performScrollTo().assertIsDisplayed()

    // Chips exist via tags
    composeRule.onNode(hasTestTag(HomeTestTags.CHIP_OPEN_PLANNER)).assertExists()
    composeRule.onNode(hasTestTag(HomeTestTags.CHIP_MOOD)).assertExists()
  }

  @Test
  fun userStatsCard_showsExpectedValues() {
    setHomeContent()

    composeRule.onNodeWithText("7d").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("1250").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("45m").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("180m").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun drawer_opens_viaMenuButton() {
    composeRule.setContent { EduMonTheme { EduMonNavHost() } }

    composeRule.waitUntil(timeoutMillis = 20_000) {
      runCatching {
            composeRule
                .onAllNodes(hasTestTag(HomeTestTags.MENU_BUTTON), useUnmergedTree = true)
                .fetchSemanticsNodes()
          }
          .getOrNull()
          ?.isNotEmpty() == true
    }

    composeRule
        .onNode(hasTestTag(HomeTestTags.MENU_BUTTON), useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeRule.onNodeWithText("Edumon").assertIsDisplayed()
    composeRule.onNodeWithText("EPFL Companion").assertIsDisplayed()
  }

  @Test
  fun creatureCard_showsEnvironment_andLevel() {
    // Replaces the old progress-bar test (your Home screen no longer exposes those bars directly)
    setHomeContent()

    composeRule
        .onNodeWithContentDescription("Creature environment")
        .performScrollTo()
        .assertExists()

    composeRule.onNodeWithText("Lv 5").performScrollTo().assertIsDisplayed()
  }
}
