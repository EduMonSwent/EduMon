package com.android.sample

import android.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
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
import com.android.sample.data.UserProfile
import com.android.sample.feature.homeScreen.EduMonHomeRoute
import com.android.sample.feature.homeScreen.EduMonHomeScreen
import com.android.sample.feature.homeScreen.GlowCard
import com.android.sample.feature.homeScreen.HomeRepository
import com.android.sample.feature.homeScreen.HomeUiState
import com.android.sample.feature.homeScreen.HomeViewModel
import com.android.sample.ui.stats.model.StudyStats
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

  @get:Rule val composeRule = createComposeRule()

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
                            // one DONE + two pending to hit both branches
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
                        UserProfile(
                            streak = 7,
                            points = 1250,
                            studyStats = StudyStats(totalTimeMin = 45, dailyGoalMin = 180)),
                    quote = quote),
            // use platform drawables so tests don’t depend on app resources
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
    setHomeContent()

    composeRule.onNodeWithContentDescription("Menu").performClick()
    composeRule.onNodeWithText("EPFL Companion").assertIsDisplayed()
    composeRule.onNodeWithText("Edumon").assertIsDisplayed()
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
    composeRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.7f, 0f..1f, 0)))
        .assertExists()
  }

  @Test
  fun route_showsProgressWhileLoading() {
    val slowRepo =
        object : HomeRepository {
          override suspend fun fetchTodos(): List<ToDo> {
            kotlinx.coroutines.delay(5_000)
            return emptyList()
          }

          override suspend fun fetchCreatureStats(): CreatureStats {
            kotlinx.coroutines.delay(5_000)
            return CreatureStats()
          }

          override suspend fun fetchUserStats(): UserProfile {
            kotlinx.coroutines.delay(5_000)
            return UserProfile()
          }

          override fun dailyQuote(nowMillis: Long): String = "Slow"
        }

    val vm = HomeViewModel(repository = slowRepo)

    composeRule.setContent {
      MaterialTheme {
        EduMonHomeRoute(
            creatureResId = R.drawable.ic_menu_help,
            environmentResId = R.drawable.ic_menu_gallery,
            onNavigate = {},
            vm = vm)
      }
    }

    composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
  }

  @Test
  fun userStats_numbers_areRendered() {
    setHomeContent()
    composeRule.onNodeWithText("7d").assertExists()
    composeRule.onNodeWithText("1250").assertExists()
    composeRule.onNodeWithText("45m").assertExists()
    composeRule.onNodeWithText("180m").assertExists()
  }

  @Test
  fun todos_showsDoneAndPendingRows_andSeeAllIsClickable() {
    setHomeContent()
    // One done -> "Completed"; one pending -> verify by title
    composeRule.onNodeWithText("Completed").assertExists()
    composeRule.onNodeWithText("Math review: sequences").assertExists()

    composeRule.onNode(hasText("See all") and hasClickAction()).assertExists()
  }

  @Test
  fun chips_arePresentAndClickable() {
    setHomeContent()
    composeRule.onNodeWithText("Open Schedule").performScrollTo().assertHasClickAction()
    composeRule.onNodeWithText("Focus Mode").performScrollTo().assertHasClickAction()
  }

  @Test
  fun creatureHouse_showsLevelChipConsistently() {
    setHomeContent()
    composeRule.onNodeWithText("Lv 5").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun glowCard_rendersChildContent() {
    composeRule.setContent { MaterialTheme { GlowCard { Text("Inside Glow") } } }
    composeRule.onNodeWithText("Inside Glow").assertIsDisplayed()
  }
}
