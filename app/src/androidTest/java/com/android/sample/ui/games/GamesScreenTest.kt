package com.android.sample.ui.games

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.

class GamesScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var navController: TestNavHostController

  @Before
  fun setup() {
    composeRule.setContent {
      val context = LocalContext.current
      navController = TestNavHostController(context)
      navController.navigatorProvider.addNavigator(ComposeNavigator())

      val graph =
          navController.createGraph(startDestination = "games") {
            composable("games") {}
            composable("memory") {}
            composable("reaction") {}
            composable("focus") {}
            composable("runner") {}
          }
      navController.graph = graph

      GamesScreen(navController = navController)
    }
  }

  @Test
  fun displaysMainTitle() {
    composeRule.onNodeWithText("EduMon Games").assertIsDisplayed()
  }

  @Test
  fun displaysAllGameCards() {
    composeRule.onNodeWithText("Memory Game").assertIsDisplayed()
    composeRule.onNodeWithText("Reaction Test").assertIsDisplayed()
    composeRule.onNodeWithText("Focus Breathing").assertIsDisplayed()
    composeRule.onNodeWithText("EduMon Runner").assertIsDisplayed()
  }

  @Test
  fun clickingMemoryGame_navigatesToMemory() {
    composeRule.onNodeWithText("Memory Game").performClick()
    assert(navController.currentBackStackEntry?.destination?.route == "memory")
  }

  @Test
  fun clickingReactionTest_navigatesToReaction() {
    composeRule.onNodeWithText("Reaction Test").performClick()
    assert(navController.currentBackStackEntry?.destination?.route == "reaction")
  }

  @Test
  fun clickingFocusBreathing_navigatesToFocus() {
    composeRule.onNodeWithText("Focus Breathing").performClick()
    assert(navController.currentBackStackEntry?.destination?.route == "focus")
  }

  @Test
  fun clickingEduMonRunner_navigatesToRunner() {
    composeRule.onNodeWithText("EduMon Runner").performClick()
    assert(navController.currentBackStackEntry?.destination?.route == "runner")
  }
}
