package com.android.sample

import android.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.data.CreatureStats
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.data.UserStats
import com.android.sample.feature.homeScreen.EduMonHomeScreen
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.feature.homeScreen.HomeUiState
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.theme.EduMonTheme
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
    // Use fake repositories so we never hit real Firebase-based UserStatsRepository
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
  fun home_showsCoreSections_andCreature() {
    setHomeContent()

    composeRule.onNodeWithText("Affirmation").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Today").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Quick Actions").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Buddy Stats").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Your Stats").performScrollTo().assertIsDisplayed()

    composeRule
        .onNodeWithContentDescription("Creature environment")
        .performScrollTo()
        .assertExists()

    composeRule.onNodeWithText("Lv 5").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun showsProvidedQuote_andCompletedLabel() {
    setHomeContent(quote = "Test Quote 123")

    composeRule.onNodeWithText("Test Quote 123").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Completed").performScrollTo().assertExists()
  }

  @Test
  fun drawer_opens_viaMenuButton() {
    // Drawer is now owned by EduMonNavHost (not EduMonHomeScreen directly)
    composeRule.setContent { EduMonTheme { EduMonNavHost() } }

    // Wait for the Home menu button to appear
    composeRule.waitUntil(timeoutMillis = 20_000) {
      runCatching {
            composeRule
                .onAllNodes(hasTestTag(HomeTestTags.MENU_BUTTON), useUnmergedTree = true)
                .fetchSemanticsNodes()
          }
          .getOrNull()
          ?.isNotEmpty() == true
    }

    // Open the drawer
    composeRule
        .onNode(hasTestTag(HomeTestTags.MENU_BUTTON), useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    // Drawer content (from EduMonDrawerContent)
    composeRule.onNodeWithText("Edumon").assertIsDisplayed()
    composeRule.onNodeWithText("EPFL Companion").assertIsDisplayed()
  }

  @Test
  fun creatureStats_progressIndicators_haveExpectedValues() {
    setHomeContent()

    composeRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.85f, 0f..1f, 0)))
        .assertExists()
    composeRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.9f, 0f..1f, 0)))
        .assertExists()
  }
}
