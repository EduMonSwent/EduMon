package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.calendar.CalendarScreenTestTags
import com.android.sample.ui.schedule.ScheduleScreen
import com.android.sample.ui.schedule.ScheduleScreenTestTags
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenAllAndroidTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  private var originalRepositories = AppRepositories

  @Before
  fun setUp() {
    // Use fake repositories so ScheduleScreen never touches real Firebase / network
    originalRepositories = AppRepositories
    AppRepositories = FakeRepositoriesProvider
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepositories
  }

  private fun fabMatcher(ctx: ComponentActivity): SemanticsMatcher {
    val label = ctx.getString(R.string.add_event)
    val cdMatcher = hasContentDescription(label)
    val textMatcher = hasText(label)
    return SemanticsMatcher("FAB with contentDescription='$label' and no text") { node ->
      cdMatcher.matches(node) && !textMatcher.matches(node)
    }
  }

  @Test
  fun scheduleScreen_rendersTabs_andDayHeader() {
    val ctx = rule.activity
    rule.setContent { ScheduleScreen(onAddTodoClicked = {}) }

    rule.onNodeWithText(ctx.getString(R.string.tab_day)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.tab_week)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.tab_month)).assertIsDisplayed()

    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    rule.onNodeWithText(ctx.getString(R.string.today_title_fmt, dateText)).assertIsDisplayed()
  }

  // FAB no longer opens modal, but tests originally expect modal.
  // We MODIFY this test to ONLY assert FAB is clickable.
  @Test
  fun scheduleScreen_fab_isClickable() {
    val ctx = rule.activity
    rule.setContent { ScheduleScreen(onAddTodoClicked = {}) }

    rule.onNode(fabMatcher(ctx)).performClick()
  }

  @Test
  fun scheduleScreen_switchTabs_noCrash_andBackToDayHeader() {
    val ctx = rule.activity
    rule.setContent { ScheduleScreen(onAddTodoClicked = {}) }

    rule.onNodeWithText(ctx.getString(R.string.tab_week)).performClick()
    rule.onNodeWithText(ctx.getString(R.string.tab_month)).performClick()

    rule.onNodeWithText(ctx.getString(R.string.tab_day)).performClick()

    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    rule.onNodeWithText(ctx.getString(R.string.today_title_fmt, dateText)).assertIsDisplayed()
  }

  @Test
  fun scheduleScreen_fab_isClickable_inDayAndWeekTabs_only() {
    val ctx = rule.activity
    var clickCount = 0

    rule.setContent { ScheduleScreen(onAddTodoClicked = { clickCount++ }) }

    // --- Day tab ---
    // FAB must exist and be clickable
    rule.onNode(fabMatcher(ctx)).assertExists().performClick()
    rule.waitForIdle()
    assertEquals(1, clickCount)

    // --- Week tab ---
    val weekLabel = ctx.getString(R.string.tab_week)
    rule.onNodeWithText(weekLabel).performClick()
    rule.waitForIdle()

    // FAB still exists and is clickable
    rule.onNode(fabMatcher(ctx)).assertExists().performClick()
    rule.waitForIdle()
    assertEquals(2, clickCount)

    // --- Month tab ---
    val monthLabel = ctx.getString(R.string.tab_month)
    rule.onNodeWithText(monthLabel).performClick()
    rule.waitForIdle()

    // No FAB in Month tab
    rule.onAllNodes(fabMatcher(ctx)).assertCountEquals(0)
  }

  @Test
  fun scheduleScreen_headerPersists_afterFabClick() {
    val ctx = rule.activity
    rule.setContent { ScheduleScreen(onAddTodoClicked = {}) }

    rule.onNode(fabMatcher(ctx)).performClick()
    rule.waitForIdle()

    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    rule.onNodeWithText(ctx.getString(R.string.today_title_fmt, dateText)).assertIsDisplayed()
  }

  private fun weekTitleFor(date: LocalDate): String {
    val weekStart = date.with(DayOfWeek.MONDAY)
    val weekEnd = weekStart.plusDays(6)
    val monthName = weekStart.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$monthName ${weekStart.dayOfMonth} - ${weekEnd.dayOfMonth}"
  }

  private fun selectWeekTab() {
    val week = rule.activity.getString(R.string.tab_week)
    rule.onNodeWithText(week).performClick()
  }

  @Test
  fun week_tab_shows_core_sections() {
    rule.setContent { ScheduleScreen(onAddTodoClicked = {}) }

    selectWeekTab()

    rule.waitUntil(timeoutMillis = 5000) {
      rule
          .onAllNodesWithTag(ScheduleScreenTestTags.CONTENT_WEEK, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    rule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_WEEK, useUnmergedTree = true).assertExists()
    rule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD, useUnmergedTree = true).assertExists()
    rule.onNodeWithTag(WeekProgDailyObjTags.WEEK_DOTS_ROW, useUnmergedTree = true).assertExists()
  }

  @Test
  fun week_header_prev_next_updates_title() {
    rule.setContent { ScheduleScreen(onAddTodoClicked = {}) }

    selectWeekTab()

    val today = LocalDate.now()
    val thisWeek = weekTitleFor(today)
    val prevWeek = weekTitleFor(today.minusWeeks(1))

    rule.waitUntil(5000) { rule.onAllNodesWithText(thisWeek).fetchSemanticsNodes().isNotEmpty() }

    rule.onNodeWithText(thisWeek).assertIsDisplayed()

    rule.onNodeWithContentDescription("Previous").performClick()
    rule.waitForIdle()
    rule.onNodeWithText(prevWeek).assertIsDisplayed()

    rule.onNodeWithContentDescription("Next").performClick()
    rule.waitForIdle()
    rule.onNodeWithText(thisWeek).assertIsDisplayed()
  }
}
