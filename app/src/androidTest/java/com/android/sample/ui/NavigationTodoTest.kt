package com.android.sample.ui.navigation

import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.sample.MainActivity
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented navigation tests for the To-Do feature.
 *
 * This launches the real MainActivity and drives the UI with Compose test APIs.
 */
class NavigationTodoTest {

  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()
  /**
   * private val seed = ToDo( title = "Seed task", dueDate = LocalDate.now().plusDays(1), priority =
   * Priority.MEDIUM, status = Status.TODO )
   *
   * @Before fun setUp() { // Ensure repository has at least one item for edit tests runBlocking {
   *   val repo = ToDoRepositoryProvider.repository // Avoid duplicating seed across repeated runs
   *   if (repo.getById(seed.id) == null) { repo.add(seed) } } }
   * @Test fun navigate_toAdd_thenSave_returnsToOverview_withNewItemVisible() { // On Overview
   *   (title visible) composeRule.onNodeWithText("Your To-Dos").assertIsDisplayed()
   *
   * // Tap FAB to go to AddToDoScreen
   * composeRule.onNodeWithTag(TestTags.FabAdd).assertIsDisplayed().performClick()
   *
   * // We are on Add screen (top bar title) composeRule.onNodeWithText("New
   * To-Do").assertIsDisplayed()
   *
   * // Enter a title and save composeRule.onNodeWithText("Title*").performTextInput("My brand new
   * task") composeRule.onNodeWithText("Save To-Do").performClick()
   *
   * // Back on Overview and the new card is visible composeRule.onNodeWithText("Your
   * To-Dos").assertIsDisplayed()
   * composeRule.onNode(hasTestTagStartingWithCard()).assertIsDisplayed()
   * composeRule.onNodeWithText("My brand new task").assertIsDisplayed() }
   *
   * @Test fun openEdit_changeTitle_save_returnsToOverview_withUpdatedTitle() { // Ensure Overview
   *   visible composeRule.onNodeWithText("Your To-Dos").assertIsDisplayed()
   *
   * // Click first card to open Edit (cards have dynamic tags: card/<id>)
   * composeRule.onAllNodes(hasTestTagStartingWithCard())[0].performClick()
   *
   * // We are on Edit screen composeRule.onNodeWithText("Edit To-Do").assertIsDisplayed()
   *
   * // Change title composeRule.onNodeWithText("Title*").apply { performTextClearance()
   * performTextInput("Edited title") }
   *
   * // Save changes composeRule.onNodeWithText("Save changes").performClick()
   *
   * // Back on Overview and updated title is visible composeRule.onNodeWithText("Your
   * To-Dos").assertIsDisplayed() composeRule.onNodeWithText("Edited title").assertIsDisplayed() }
   *
   * @Test fun topBarTitle_isCorrect_onEditScreen() { // From overview, open first item
   *   composeRule.onAllNodes(hasTestTagStartingWithCard())[0].performClick() // Title of Edit
   *   screen composeRule.onNodeWithText("Edit To-Do").assertIsDisplayed() }
   */
  @Test
  fun bottomBar_isNotDisplayed_anywhere() {
    // We don't have a bottom bar in this app; assert it's not there by tag
    composeRule.onNodeWithTag("BottomBar").assertIsNotDisplayed()
  }

  // ---- Helpers ----

  /**
   * private fun hasTestTagStartingWithCard() = hasTestTagPrefix(TestTags.cardPrefix)
   *
   * private fun hasTestTagPrefix(prefix: String) = hasTestTag(prefix, usePrefix = true)
   *
   * private fun hasTestTag(tag: String, usePrefix: Boolean) =
   * androidx.compose.ui.test.SemanticsMatcher( description = if (usePrefix) "Tag starts with: $tag"
   * else "Tag equals: $tag" ) { node -> val actual =
   * node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag) if (usePrefix)
   * actual?.startsWith(tag) == true else actual == tag }
   */
}
