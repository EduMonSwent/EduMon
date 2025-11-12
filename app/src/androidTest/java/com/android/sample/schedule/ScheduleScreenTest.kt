package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
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
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import com.android.sample.ui.calendar.CalendarScreenTestTags
import com.android.sample.ui.schedule.ScheduleScreen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenAllAndroidTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  // Helper that finds the FAB specifically:
  // - It has contentDescription "Add Event"
  // - It does NOT draw text "Add Event" (unlike the inline button in UpcomingEventsSection)
  private fun fabMatcher(ctx: ComponentActivity): SemanticsMatcher {
    val label = ctx.getString(R.string.add_event)
    val cdMatcher = hasContentDescription(label)
    val textMatcher = hasText(label)

    return SemanticsMatcher("FAB with contentDescription='$label' and no text") { node ->
      cdMatcher.matches(node) && !textMatcher.matches(node)
    }
  }

  /** Renders all tabs + the Day header with today's date. */
  @Test
  fun scheduleScreen_rendersTabs_andDayHeader() {
    val ctx = rule.activity
    rule.setContent { ScheduleScreen() }

    // Tabs
    rule.onNodeWithText(ctx.getString(R.string.tab_day)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.tab_week)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.tab_month)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.tab_agenda)).assertIsDisplayed()

    // Day header "Today • <date>"
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    rule.onNodeWithText(ctx.getString(R.string.today_title_fmt, dateText)).assertIsDisplayed()
  }

  /** FAB opens the Add Study Task modal in Day tab. */
  @Test
  fun scheduleScreen_fab_opensAddTaskModal() {
    val ctx = rule.activity
    rule.setContent { ScheduleScreen() }

    // Click the FAB (not the inline button)
    rule.onNode(fabMatcher(ctx)).performClick()

    // Modal title
    rule.onNodeWithText(ctx.getString(R.string.add_study_task_modal_title)).assertIsDisplayed()
  }

  /** Switching tabs does not crash; Day header still visible when we return. */
  @Test
  fun scheduleScreen_switchTabs_noCrash_andBackToDayHeader() {
    val ctx = rule.activity
    rule.setContent { ScheduleScreen() }

    // Click through tabs
    rule.onNodeWithText(ctx.getString(R.string.tab_week)).performClick()
    rule.onNodeWithText(ctx.getString(R.string.tab_month)).performClick()
    rule.onNodeWithText(ctx.getString(R.string.tab_agenda)).performClick()

    // Back to Day
    rule.onNodeWithText(ctx.getString(R.string.tab_day)).performClick()

    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    rule.onNodeWithText(ctx.getString(R.string.today_title_fmt, dateText)).assertIsDisplayed()
  }

  /** FAB should open the Add Task modal from every tab (even if content is TODO). */
  @Test
  fun scheduleScreen_fab_opensModal_inEachTab() {
    val ctx = rule.activity
    rule.setContent { ScheduleScreen() }

    fun assertModalFromHere() {
      rule.onNode(fabMatcher(ctx)).performClick()
      rule.onNodeWithText(ctx.getString(R.string.add_study_task_modal_title)).assertIsDisplayed()

      // Dismiss the modal
      Espresso.pressBack()
      rule.waitForIdle()
    }

    assertModalFromHere() // Day

    rule.onNodeWithText(ctx.getString(R.string.tab_week)).performClick()
    assertModalFromHere()

    rule.onNodeWithText(ctx.getString(R.string.tab_month)).performClick()
    assertModalFromHere()

    rule.onNodeWithText(ctx.getString(R.string.tab_agenda)).performClick()
    assertModalFromHere()
  }

  /** After closing the Add Task modal, UI remains interactive and Day header is still present. */
  @Test
  fun scheduleScreen_headerPersists_afterModalClose() {
    val ctx = rule.activity
    rule.setContent { ScheduleScreen() }

    rule.onNode(fabMatcher(ctx)).performClick()
    rule.onNodeWithText(ctx.getString(R.string.add_study_task_modal_title)).assertIsDisplayed()

    // Dismiss modal
    Espresso.pressBack()
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
    rule.setContent { ScheduleScreen() }

    // Switch to Week tab
    selectWeekTab()

    // Attendre que la composable "WeekContent" apparaisse dans l’arbre
    rule.waitUntil(timeoutMillis = 5_000) {
      rule
          .onAllNodesWithTag("WeekContent", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Vérifier simplement qu’elle existe (pas forcément "isDisplayed" dans ce contexte de test)
    rule.onNodeWithTag("WeekContent", useUnmergedTree = true).assertExists()

    // Idem pour les sections clés
    rule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD, useUnmergedTree = true).assertExists()
    rule.onNodeWithTag(WeekProgDailyObjTags.WEEK_DOTS_ROW, useUnmergedTree = true).assertExists()
  }

  @Test
  fun week_header_prev_next_updates_title() {
    rule.setContent { ScheduleScreen() }

    // Switch to Week tab
    selectWeekTab()

    val today = LocalDate.now()
    val thisWeek = weekTitleFor(today)
    val prevWeek = weekTitleFor(today.minusWeeks(1))

    // Wait for initial header title to appear
    rule.waitUntil(timeoutMillis = 5_000) {
      rule.onAllNodesWithText(thisWeek).fetchSemanticsNodes().isNotEmpty()
    }

    // Initial title (this week) should be visible
    rule.onNodeWithText(thisWeek).assertIsDisplayed()

    // Tap "Previous" -> title should become previous week
    rule.onNodeWithContentDescription("Previous").performClick()
    rule.waitForIdle()
    rule.onNodeWithText(prevWeek).assertIsDisplayed()

    // Tap "Next" -> back to initial title
    rule.onNodeWithContentDescription("Next").performClick()
    rule.waitForIdle()
    rule.onNodeWithText(thisWeek).assertIsDisplayed()
  }
}
