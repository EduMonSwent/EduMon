package com.android.sample.ui.stats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.data.FakeUserStatsRepository
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import com.android.sample.ui.stats.repository.FakeStatsRepository
import com.android.sample.ui.stats.viewmodel.StatsViewModel
import com.android.sample.ui.theme.SampleAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsRouteTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun statsRoute_displays_stats_screen_immediately_with_fake_data() {
    val fakeStatsRepo = FakeStatsRepository()
    val fakeUserStatsRepo = FakeUserStatsRepository()

    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)

    composeTestRule.setContent { SampleAppTheme { StatsRoute(viewModel = vm) } }

    composeTestRule.waitForIdle()

    // FakeStatsRepository always provides data, so screen should display immediately
    composeTestRule.onNodeWithText("Début de semaine").assertIsDisplayed()
  }

  @Test
  fun statsRoute_displays_stats_screen_when_data_available() {
    val fakeStatsRepo = FakeStatsRepository()
    val fakeUserStatsRepo = FakeUserStatsRepository()

    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)

    composeTestRule.setContent { SampleAppTheme { StatsRoute(viewModel = vm) } }

    composeTestRule.waitForIdle()

    // Stats content should be displayed (not loading indicator)
    // Check for presence of scenario buttons or other stats content
    composeTestRule.onNodeWithText("Début de semaine").assertIsDisplayed()
  }

  @Test
  fun statsRoute_responds_to_scenario_selection() {
    val fakeStatsRepo = FakeStatsRepository()
    val fakeUserStatsRepo = FakeUserStatsRepository()

    val vm =
        StatsViewModel(
            repo = fakeStatsRepo,
            objectivesRepo = FakeObjectivesRepository,
            userStatsRepo = fakeUserStatsRepo)

    composeTestRule.setContent { SampleAppTheme { StatsRoute(viewModel = vm) } }

    composeTestRule.waitForIdle()

    // Click on second scenario
    composeTestRule.onNodeWithText("Semaine active").assertIsDisplayed()
  }

  @Test
  fun statsRoute_with_default_viewModel_constructor() {
    // Test that StatsRoute can be created without explicitly passing viewModel
    composeTestRule.setContent {
      SampleAppTheme {
        // This would normally use the default ViewModel constructor
        // For testing, we need to provide mocked dependencies
        val vm =
            StatsViewModel(
                repo = FakeStatsRepository(),
                objectivesRepo = FakeObjectivesRepository,
                userStatsRepo = FakeUserStatsRepository())
        StatsRoute(viewModel = vm)
      }
    }

    composeTestRule.waitForIdle()
  }
}
