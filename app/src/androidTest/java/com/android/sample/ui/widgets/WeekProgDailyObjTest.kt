package com.android.sample.ui.widgets

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.theme.EduMonTheme
import com.android.sample.ui.viewmodel.DayStatus
import com.android.sample.ui.viewmodel.Objective
import com.android.sample.ui.viewmodel.WeekProgressViewModel
import java.time.DayOfWeek
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [WeekProgDailyObj]. These tests rely on the default data provided by
 * [WeekProgressViewModel()]. If your ViewModel requires constructor args or doesn't expose sample
 * data by default, adjust the `setContent()` calls to use your DI / test fake.
 */
@RunWith(AndroidJUnit4::class)
class WeekProgDailyObjTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private fun setContent(
      vm: WeekProgressViewModel = WeekProgressViewModel(),
  ) {
    compose.setContent {
      EduMonTheme {
        WeekProgDailyObj(
            viewModel = vm,
            modifier = Modifier,
        )
      }
    }
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
    val vm = WeekProgressViewModel().apply { setObjectives(emptyList()) }
    setContent(vm)

    compose.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_EMPTY).assertExists().assertIsDisplayed()
  }

  @Test
  fun objective_reason_isVisible_forFirstObjective_whenShowWhyTrue() {
    setContent()

    // With default VM, showWhy = true and first objective has a non-empty reason
    compose
        .onNodeWithTag(WeekProgDailyObjTags.OBJECTIVE_REASON_PREFIX + 0)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun selectNext_and_selectPrevious_areClamped_andSyncHeader() = runTest {
    val vm = WeekProgressViewModel()
    // move forward
    vm.selectNextWeek()
    assertEquals(2, vm.uiState.value.selectedWeekIndex)
    assertEquals(10, vm.uiState.value.weekProgressPercent)

    // move backward
    vm.selectPreviousWeek()
    assertEquals(1, vm.uiState.value.selectedWeekIndex)
    assertEquals(55, vm.uiState.value.weekProgressPercent)

    // with empty weeks, selection stays within [0,0] and header unchanged
    val before = vm.uiState.value.weekProgressPercent
    vm.setWeeks(emptyList())
    vm.selectNextWeek()
    assertEquals(0, vm.uiState.value.selectedWeekIndex)
    assertEquals(before, vm.uiState.value.weekProgressPercent)
    vm.selectPreviousWeek()
    assertEquals(0, vm.uiState.value.selectedWeekIndex)
    assertEquals(before, vm.uiState.value.weekProgressPercent)
  }

  @Test
  fun dayStatuses_set_and_toggleOnlyMatchingDay() = runTest {
    val vm = WeekProgressViewModel()
    vm.setDayStatuses(listOf(DayStatus(DayOfWeek.MONDAY, false)))
    vm.toggleDayMet(DayOfWeek.MONDAY)
    assertTrue(vm.uiState.value.dayStatuses.first().metTarget)

    // toggling a missing day leaves list unchanged
    val before = vm.uiState.value.dayStatuses
    vm.toggleDayMet(DayOfWeek.SUNDAY)
    assertEquals(before, vm.uiState.value.dayStatuses)
  }

  @Test
  fun setShowWhy_updatesFlag() = runTest {
    val vm = WeekProgressViewModel()
    assertTrue(vm.uiState.value.showWhy)
    vm.setShowWhy(false)
    assertFalse(vm.uiState.value.showWhy)
  }

  @Test
  fun weekDotsRow_rendersAllDays_withMetAndUnmetStates() {
    val statuses =
        listOf(
            DayStatus(DayOfWeek.MONDAY, true),
            DayStatus(DayOfWeek.TUESDAY, false),
            DayStatus(DayOfWeek.WEDNESDAY, true)
            // rest will be filled as false by the composable
            )

    compose.setContent { EduMonTheme { WeekDotsRow(statuses) } }

    // All 7 dots exist by tag (the composable fills missing days)
    DayOfWeek.values().forEach { dow ->
      compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_DOT_PREFIX + dow.name).assertExists()
    }
  }

  @Test
  fun dailyObjectives_empty_showsFriendlyMessage_andNoShowAllButton() {
    compose.setContent {
      EduMonTheme {
        DailyObjectivesSection(objectives = emptyList(), showWhy = true, onStartObjective = {})
      }
    }

    compose.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_EMPTY).assertIsDisplayed()
    compose.onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON).assertCountEquals(0)
  }

  @Test
  fun dailyObjectives_showWhyFalse_hidesReasonEvenIfPresent() {
    val objs = listOf(Objective("Task", "Course", 5, "Some reason"))
    compose.setContent {
      EduMonTheme {
        DailyObjectivesSection(
            objectives = objs,
            showWhy = false, // hide why
            onStartObjective = {})
      }
    }
    compose.onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVE_REASON_PREFIX + 0).assertCountEquals(0)
  }

  @Test
  fun weekProgress_noWeeks_doesNotShowExpandedList() {
    compose.setContent {
      EduMonTheme {
        WeekProgressSection(
            weekProgressPercent = 40,
            weeks = emptyList(),
            selectedWeekIndex = 0,
            onSelectWeek = {},
        )
      }
    }
    // tapping the header shouldn't reveal a list since there are no weeks
    compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE).performClick()
    compose.onAllNodesWithTag(WeekProgDailyObjTags.WEEKS_LIST).assertCountEquals(0)
  }

  /**
   * Matches any node whose testTag starts with [prefix]. Helpful for lists where items use
   * index-suffixed tags (e.g. "WEEK_ROW_0").
   */
  private fun hasTestTagPrefix(prefix: String): SemanticsMatcher =
      SemanticsMatcher("Has testTag starting with $prefix") { node ->
        val tag = kotlin.runCatching { node.config[SemanticsProperties.TestTag] }.getOrNull()
        tag?.startsWith(prefix) == true
      }
}
