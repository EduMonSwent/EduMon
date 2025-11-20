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
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.homeScreen.EduMonHomeRoute
import com.android.sample.feature.homeScreen.EduMonHomeScreen
import com.android.sample.feature.homeScreen.GlowCard
import com.android.sample.feature.homeScreen.HomeRepository
import com.android.sample.feature.homeScreen.HomeUiState
import com.android.sample.feature.homeScreen.HomeViewModel
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

  @get:Rule val composeRule = createComposeRule()

  // --- Test helper repos ---

  private class FakeUserStatsRepository(initial: UserStats = UserStats()) : UserStatsRepository {
    private val _stats = MutableStateFlow(initial)
    override val stats: StateFlow<UserStats> = _stats

    override fun start() {
      // no-op
    }

    override suspend fun addStudyMinutes(extraMinutes: Int) {
      _stats.value =
          _stats.value.copy(totalStudyMinutes = _stats.value.totalStudyMinutes + extraMinutes)
    }

    override suspend fun updateCoins(delta: Int) {
      _stats.value = _stats.value.copy(coins = _stats.value.coins + delta)
    }

    override suspend fun setWeeklyGoal(goalMinutes: Int) {
      _stats.value = _stats.value.copy(weeklyGoal = goalMinutes)
    }

    override suspend fun addPoints(delta: Int) {
      _stats.value = _stats.value.copy(points = _stats.value.points + delta)
    }
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
                            lastUpdated = 0L),
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
    composeRule.onNodeWithContentDescription("Creature").performScrollTo().assertExists()
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
            delay(5_000)
            return emptyList()
          }

          override suspend fun fetchCreatureStats(): CreatureStats {
            delay(5_000)
            return CreatureStats()
          }

          override suspend fun fetchUserStats(): UserProfile {
            delay(5_000)
            return UserProfile()
          }

          override fun dailyQuote(nowMillis: Long): String = "Slow"
        }

    val vm =
        HomeViewModel(
            repository = slowRepo, userStatsRepository = FakeUserStatsRepository(UserStats()))

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
    composeRule.onNodeWithText("Completed").assertExists()
    composeRule.onNodeWithText("Math review: sequences").assertExists()
    composeRule.onNode(hasText("See all") and hasClickAction()).assertExists()
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

  @Test
  fun quickActions_allButtonsAreClickable() {
    setHomeContent()
    composeRule.onNodeWithText("Study 30m").performScrollTo().assertHasClickAction()
    composeRule.onNodeWithText("Take Break").performScrollTo().assertHasClickAction()
    composeRule.onNodeWithText("Exercise").performScrollTo().assertHasClickAction()
    composeRule.onNodeWithText("Social Time").performScrollTo().assertHasClickAction()
  }

  @Test
  fun bottomNav_showsAllItems() {
    setHomeContent()
    composeRule.onNodeWithContentDescription("Home").assertExists()
    composeRule.onNodeWithContentDescription("Calendar").assertExists()
    composeRule.onNodeWithContentDescription("Study").assertExists()
    composeRule.onNodeWithContentDescription("Profile").assertExists()
    composeRule.onNodeWithContentDescription("Games").assertExists()
  }

  @Test
  fun emptyTodos_stillRendersCard() {
    composeRule.setContent {
      MaterialTheme {
        EduMonHomeScreen(
            state =
                HomeUiState(
                    isLoading = false,
                    todos = emptyList(),
                    creatureStats = CreatureStats(level = 1),
                    userStats = UserStats(),
                    quote = "Empty state"),
            creatureResId = R.drawable.ic_menu_help,
            environmentResId = R.drawable.ic_menu_gallery,
            onNavigate = {})
      }
    }

    composeRule.onNodeWithText("Today").performScrollTo().assertIsDisplayed()
    composeRule.onNode(hasText("See all") and hasClickAction()).assertExists()
  }

  @Test
  fun navigation_callbacks_trigger() {
    var lastRoute = ""

    setHomeContent { route -> lastRoute = route }

    composeRule.onNodeWithText("Open Planner").performClick()
    assertEquals("planner", lastRoute)

    composeRule.onNode(hasText("See all")).performClick()
    assertEquals("planner", lastRoute)
  }

  @Test
  fun userStats_displaysAllFields() {
    setHomeContent()

    composeRule.onNodeWithText("Streak").performScrollTo().assertExists()
    composeRule.onNodeWithText("Points").performScrollTo().assertExists()
    composeRule.onNodeWithText("Study Today").performScrollTo().assertExists()
    composeRule.onNodeWithText("Weekly Goal").performScrollTo().assertExists()
  }

  @Test
  fun creatureStats_displaysAllFields() {
    setHomeContent()

    composeRule.onNodeWithText("Buddy Stats").performScrollTo().assertIsDisplayed()

    // Check for progress bars (happiness, health, energy)
    composeRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.85f, 0f..1f, 0)))
        .performScrollTo()
        .assertExists()
  }

  @Test
  fun todos_showCorrectStatus() {
    val today = LocalDate.now()

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
                                title = "Done Task",
                                dueDate = today,
                                priority = Priority.HIGH,
                                status = Status.DONE),
                            ToDo(
                                id = "2",
                                title = "Pending Task",
                                dueDate = today,
                                priority = Priority.LOW,
                                status = Status.IN_PROGRESS)),
                    creatureStats = CreatureStats(),
                    userStats = UserStats(),
                    quote = "Test"),
            creatureResId = R.drawable.ic_menu_help,
            environmentResId = R.drawable.ic_menu_gallery,
            onNavigate = {})
      }
    }

    composeRule.onNodeWithText("Done Task").assertExists()
    composeRule.onNodeWithText("Pending Task").assertExists()
    composeRule.onNodeWithText("Completed").assertExists()
  }

  @Test
  fun focusModeChip_exists() {
    setHomeContent()

    // Scroll to affirmation section
    composeRule.onNodeWithText("Affirmation").performScrollTo()

    composeRule.onNodeWithText("Focus Mode").assertIsDisplayed()
  }

  @Test
  fun affirmationCard_displaysQuote() {
    setHomeContent(quote = "Custom affirmation quote")

    composeRule.onNodeWithText("Affirmation").performScrollTo()
    composeRule.onNodeWithText("Custom affirmation quote").assertExists()
  }

  @Test
  fun todayCard_showsMaxThreeTodos() {
    val today = LocalDate.now()
    val manyTodos =
        (1..10).map { i ->
          ToDo(id = "todo_$i", title = "Task $i", dueDate = today, priority = Priority.LOW)
        }

    composeRule.setContent {
      MaterialTheme {
        EduMonHomeScreen(
            state =
                HomeUiState(
                    isLoading = false,
                    todos = manyTodos,
                    creatureStats = CreatureStats(),
                    userStats = UserStats(),
                    quote = "Test"),
            creatureResId = R.drawable.ic_menu_help,
            environmentResId = R.drawable.ic_menu_gallery,
            onNavigate = {})
      }
    }

    // Should show only first 3
    composeRule.onNodeWithText("Task 1").assertExists()
    composeRule.onNodeWithText("Task 2").assertExists()
    composeRule.onNodeWithText("Task 3").assertExists()
    composeRule.onNodeWithText("Task 4").assertDoesNotExist()
  }

  @Test
  fun quickActionsCard_allActionsPresent() {
    setHomeContent()

    composeRule.onNodeWithText("Quick Actions").performScrollTo()
    composeRule.onNodeWithText("Study 30m").assertExists()
    composeRule.onNodeWithText("Take Break").assertExists()
    composeRule.onNodeWithText("Exercise").assertExists()
    composeRule.onNodeWithText("Social Time").assertExists()
  }

  @Test
  fun userStatsCard_title_displayed() {
    setHomeContent()

    composeRule.onNodeWithText("Your Stats").performScrollTo().assertExists()
  }

  @Test
  fun creatureStatsCard_title_displayed() {
    setHomeContent()

    composeRule.onNodeWithText("Buddy Stats").performScrollTo().assertExists()
  }

  @Test
  fun navigation_studyButton_triggersCallback() {
    var navigationCalled = false
    var route = ""

    setHomeContent { r ->
      navigationCalled = true
      route = r
    }

    composeRule.onNodeWithText("Study 30m").performScrollTo().performClick()

    assert(navigationCalled)
    assertEquals("study", route)
  }
}
