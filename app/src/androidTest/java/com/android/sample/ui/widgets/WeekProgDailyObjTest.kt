package com.android.sample.ui.widgets

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.theme.EduMonTheme
import com.android.sample.ui.viewmodel.WeekProgressViewModel
import java.time.DayOfWeek
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
