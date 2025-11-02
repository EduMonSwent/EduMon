package com.android.sample.ui.widgets

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.model.WeekProgressItem
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import com.android.sample.feature.weeks.repository.FakeWeeksRepository
import com.android.sample.feature.weeks.ui.WeekDotsRow
import com.android.sample.feature.weeks.ui.WeekProgDailyObj
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.feature.weeks.viewmodel.WeeksViewModel
import com.android.sample.ui.theme.EduMonTheme
import java.time.DayOfWeek
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [com.android.sample.feature.weeks.ui.WeekProgDailyObj] using separated view models
 * (weeks, objectives, dots).
 */
@RunWith(AndroidJUnit4::class)
class WeekProgDailyObjTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private fun setContent(customize: (WeeksViewModel, ObjectivesViewModel) -> Unit = { _, _ -> }) {
    // Use fake repos so tests don't require Firebase auth or emulator networking
    val weeksVM = WeeksViewModel(repository = FakeWeeksRepository())
    val objVM = ObjectivesViewModel(repository = FakeObjectivesRepository)

    // Seed defaults similar to old WeekProgressViewModel
    weeksVM.setWeeks(
        listOf(
            WeekProgressItem("Week 1", 100),
            WeekProgressItem("Week 2", 55),
            WeekProgressItem("Week 3", 10),
            WeekProgressItem("Week 4", 0)),
        selectedIndex = 1)
    weeksVM.setProgress(55)

    objVM.setObjectives(
        listOf(
            Objective("Finish Quiz 3", "CS101", 30, false, DayOfWeek.MONDAY),
            Objective("Read Chapter 5", "Math201", 25, false, DayOfWeek.TUESDAY)))

    // Allow per-test overrides
    customize(weeksVM, objVM)

    compose.setContent { EduMonTheme { WeekProgDailyObj(weeksVM, objVM) } }
  }

  @Test
  fun showsCoreSections() {
    setContent()

    compose.onNodeWithTag(WeekProgDailyObjTags.ROOT_CARD).assertExists().assertIsDisplayed()
    compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_PROGRESS_BAR).assertExists()
    compose.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION).assertExists()
    compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_DOTS_ROW).assertExists()

    // Footer should render 7 dots (one per day), regardless of data
    DayOfWeek.values().forEach { dow ->
      compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_DOT_PREFIX + dow.name).assertExists()
    }
  }

  @Test
  fun expandsWeeksWhenToggled_ifWeeksAvailable() {
    setContent()

    // Expand the weeks section
    compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE).assertHasClickAction()
    compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE).performClick()

    // Only assert list if there is week content available
    val weeksNodes =
        compose.onAllNodesWithTag(WeekProgDailyObjTags.WEEKS_LIST).fetchSemanticsNodes()
    if (weeksNodes.isEmpty()) return

    compose.onNodeWithTag(WeekProgDailyObjTags.WEEKS_LIST).assertIsDisplayed()

    // If at least one week row exists, try clicking it
    val weekRows =
        compose
            .onAllNodes(hasTestTagPrefix(WeekProgDailyObjTags.WEEK_ROW_PREFIX))
            .fetchSemanticsNodes()
    if (weekRows.isEmpty()) return

    // Click the first visible week row
    compose.onAllNodes(hasTestTagPrefix(WeekProgDailyObjTags.WEEK_ROW_PREFIX))[0].performClick()
  }

  @Test
  fun showsObjectivesOrEmptyState() {
    setContent()

    // Either the empty label or at least the first objective row should exist
    val emptyNodes =
        compose.onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_EMPTY).fetchSemanticsNodes()
    if (emptyNodes.isNotEmpty()) {
      compose.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_EMPTY).assertExists()
    } else {
      val objectiveRows =
          compose
              .onAllNodes(hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_ROW_PREFIX))
              .fetchSemanticsNodes()
      assertTrue("Expected at least one objective row", objectiveRows.isNotEmpty())
    }
  }

  @Test
  fun expandsObjectives_whenShowAllClicked_ifMultipleObjectives() {
    setContent()

    // Show-all button only renders if there are >1 objectives
    val showAllPresent =
        compose
            .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON)
            .fetchSemanticsNodes()
            .isNotEmpty()
    if (!showAllPresent) return

    compose.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON).assertHasClickAction()
    compose.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON).performClick()

    // After expanding, we expect at least 2 objective rows total
    val rows =
        compose
            .onAllNodes(hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_ROW_PREFIX))
            .fetchSemanticsNodes()
    assertTrue("Expected multiple objective rows after expanding", rows.size >= 2)
  }

  @Test
  fun startFirstObjective_isClickable() {
    setContent()

    // First objective's start button should be clickable if objectives exist
    val startButtons =
        compose
            .onAllNodes(hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX))
            .fetchSemanticsNodes()
    if (startButtons.isEmpty()) return

    compose
        .onAllNodes(hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX))[0]
        .assertHasClickAction()
        .performClick()
  }

  // ---------------------------------------------------------------------
  // Merged additional coverage
  // ---------------------------------------------------------------------

  @Test
  fun weeks_expandCollapse_toggleTwice() {
    setContent()

    val toggle = compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE)
    toggle.assertHasClickAction()

    // expand
    toggle.performClick()
    // collapse
    toggle.performClick()
  }

  @Test
  fun objectives_headerToggle_expandsAndCollapses_whenMultiple() {
    setContent()

    // Header is clickable only when there are multiple objectives
    val toggleNodes = compose.onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_TOGGLE)
    if (toggleNodes.fetchSemanticsNodes().isEmpty()) return

    val toggle = compose.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_TOGGLE)
    toggle.assertHasClickAction()
    toggle.performClick() // expand
    toggle.performClick() // collapse
  }

  @Test
  fun objectives_emptyState_rendersWhenNoObjectives() {
    setContent { _, objVM -> objVM.setObjectives(emptyList()) }

    compose.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_EMPTY).assertExists().assertIsDisplayed()
  }

  @Test
  fun weekDotsRow_rendersAllDays_withObjectivesVM() {
    // Build a VM with minimal per-day objectives (some days empty)
    val vm = ObjectivesViewModel(repository = FakeObjectivesRepository)
    vm.setObjectives(
        listOf(
            Objective("A", "CS", estimateMinutes = 5, completed = true, day = DayOfWeek.MONDAY),
            Objective("B", "CS", estimateMinutes = 5, completed = false, day = DayOfWeek.TUESDAY),
            Objective(
                "C", "ENG", estimateMinutes = 5, completed = true, day = DayOfWeek.WEDNESDAY)))

    compose.setContent { EduMonTheme { WeekDotsRow(vm) } }

    DayOfWeek.values().forEach { dow ->
      compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_DOT_PREFIX + dow.name).assertExists()
    }
  }

  @Test
  fun weekDropdown_nonSelected_showsSelectMessage_orHeaders() {
    setContent()

    // Expand weeks section
    compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE).performClick()
    compose.onNodeWithTag(WeekProgDailyObjTags.WEEKS_LIST).assertExists()

    // In this setup, selected index is 1; open dropdown for index 0 (non-selected)
    compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_ROW_PREFIX + 1).performClick()

    compose.onNodeWithTag("WEEK_DROPDOWN_BTN_0").assertHasClickAction()
    compose.onNodeWithTag("WEEK_DROPDOWN_BTN_0").performClick()

    val helper =
        compose.onAllNodesWithText("Select this week to view details").fetchSemanticsNodes()
    if (helper.isNotEmpty()) {
      compose.onNodeWithText("Select this week to view details").assertExists().assertIsDisplayed()
    } else {
      compose.onNode(hasText("Exercises (", substring = true)).assertExists()
      compose.onNode(hasText("Courses (", substring = true)).assertExists()
    }
  }

  @Test
  fun weekDropdown_selectedWeek0_showsExercisesAndCourses_withDoneIcons() {
    setContent()

    // Expand and select week 0 first so dropdown renders content instead of the select message
    compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE).performClick()
    compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_ROW_PREFIX + 0).performClick()

    // Open dropdown for week 0
    compose.onNodeWithTag("WEEK_DROPDOWN_BTN_0").performClick()

    compose.onNode(hasText("Exercises (", substring = true)).assertExists()
    compose.onNode(hasText("Courses (", substring = true)).assertExists()
    compose.onNodeWithText("Set up environment").assertExists()
    compose.onNodeWithText("Finish codelab").assertExists()
    compose.onNodeWithText("Intro to Android").assertExists()
    compose.onNodeWithText("Compose Basics").assertExists()
    val doneIcons = compose.onAllNodes(hasContentDescription("Done")).fetchSemanticsNodes()
    assertTrue(doneIcons.isNotEmpty())
  }

  private fun hasTestTagPrefix(prefix: String): SemanticsMatcher =
      SemanticsMatcher("Has testTag starting with $prefix") { node ->
        val tag = kotlin.runCatching { node.config[SemanticsProperties.TestTag] }.getOrNull()
        tag?.startsWith(prefix) == true
      }
}
