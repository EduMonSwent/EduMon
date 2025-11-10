package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import com.android.sample.ui.schedule.ScheduleScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenAllAndroidTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

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

    // Click FAB
    rule.onNodeWithContentDescription(ctx.getString(R.string.add_event)).performClick()

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
      rule.onNodeWithContentDescription(ctx.getString(R.string.add_event)).performClick()
      rule.onNodeWithText(ctx.getString(R.string.add_study_task_modal_title)).assertIsDisplayed()

      // Dismiss safely on main thread
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

    rule.onNodeWithContentDescription(ctx.getString(R.string.add_event)).performClick()
    rule.onNodeWithText(ctx.getString(R.string.add_study_task_modal_title)).assertIsDisplayed()

    // Dismiss modal
    Espresso.pressBack()
    rule.waitForIdle()

    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    rule.onNodeWithText(ctx.getString(R.string.today_title_fmt, dateText)).assertIsDisplayed()
  }
}
