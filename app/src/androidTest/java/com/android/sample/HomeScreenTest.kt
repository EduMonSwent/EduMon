package com.android.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private fun setHomeContent(quote: String = "Keep going.", onNavigate: (String) -> Unit = {}) {
    composeRule.setContent {
      MaterialTheme {
        EduMonHomeScreen(
            state =
                HomeUiState(
                    isLoading = false,
                    todos =
                        listOf(
                            // both done and not-done to hit both branches
                            Todo("1", "CS-101: Finish exercise sheet", "Today 18:00", true),
                            Todo("2", "Math review: sequences", "Today 20:00"),
                            Todo("3", "Pack lab kit for tomorrow", "Tomorrow"),
                        ),
                    creatureStats =
                        CreatureStats(happiness = 85, health = 90, energy = 70, level = 5),
                    userStats =
                        UserStats(
                            streakDays = 7, points = 1250, studyTodayMin = 45, dailyGoalMin = 180),
                    quote = quote),
            // use platform drawables so tests don’t depend on app resources
            creatureResId = android.R.drawable.ic_menu_help,
            environmentResId = android.R.drawable.ic_menu_gallery,
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

    composeRule.onNodeWithContentDescription("Creature").performScrollTo().assertExists()

    composeRule.onNodeWithText("Lv 5").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun showsProvidedQuote_andCompletedLabel() {
    setHomeContent(quote = "Test Quote 123")
    composeRule.onNodeWithText("Test Quote 123").assertIsDisplayed()
    // Done item should show "Completed"
    composeRule.onNodeWithText("Completed").assertExists()
  }

  @Test
  fun drawer_opens_viaMenuButton() {
    setHomeContent()

    composeRule.onNodeWithContentDescription("Menu").performClick()
    // Drawer contents should become visible
    composeRule.onNodeWithText("EPFL Companion").assertIsDisplayed()
    composeRule.onNodeWithText("Edumon").assertIsDisplayed()
  }

  @Test
  fun creatureStats_progressIndicators_haveExpectedValues() {
    setHomeContent()

    // Expect 0.85f, 0.9f, 0.7f for Happiness, Health, Energy
    composeRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.85f, 0f..1f, 0)))
        .assertExists()
    composeRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.9f, 0f..1f, 0)))
        .assertExists()
    composeRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.7f, 0f..1f, 0)))
        .assertExists()
  }
}
