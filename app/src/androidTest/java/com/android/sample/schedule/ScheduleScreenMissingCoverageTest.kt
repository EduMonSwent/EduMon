package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.schedule.ScheduleScreen
import com.android.sample.ui.schedule.ScheduleScreenTestTags
import java.time.LocalDate
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenMissingCoverageTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  private var originalRepos = AppRepositories

  @Before
  fun setUp() {
    originalRepos = AppRepositories
    AppRepositories = FakeRepositoriesProvider
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepos
  }

  private fun isProgressBar() =
      SemanticsMatcher("is progress") {
        it.config.contains(SemanticsProperties.ProgressBarRangeInfo)
      }

  // ------------------------------------------
  // 2. MONTH TAB triggers setMonthMode()
  // ------------------------------------------
  @Test
  fun monthTab_rendersMonthContent() {
    rule.setContent { ScheduleScreen() }

    val month = rule.activity.getString(R.string.tab_month)
    rule.onNodeWithText(month).performClick()

    rule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_MONTH, useUnmergedTree = true).assertExists()
  }
  // ------------------------------------------
  // 5. FAB WEEK uses startOfWeek()
  // ------------------------------------------
  @Test
  fun fab_week_passesStartOfWeekToCallback() {
    var clickedDate: LocalDate? = null

    rule.setContent { ScheduleScreen(onAddTodoClicked = { clickedDate = it }) }

    // Switch to WEEK
    rule.onNodeWithText(rule.activity.getString(R.string.tab_week)).performClick()

    val expected = LocalDate.now().with(java.time.DayOfWeek.MONDAY)

    rule.onNodeWithTag(ScheduleScreenTestTags.FAB_ADD).performClick()

    Assert.assertEquals(expected, clickedDate)
  }
}
