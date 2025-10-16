package com.android.sample.ui.games

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import org.junit.Rule
import org.junit.Test

class GameScreenExtendedTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun gameCard_displaysTitleSubtitleAndIcon() {
    composeRule.setContent {
      GameCard(
          title = "Memory Game",
          subtitle = "Train your brain",
          icon = Icons.Default.Memory,
          color = Color.Cyan,
          onClick = {})
    }

    composeRule.onNodeWithText("Memory Game").assertExists()
    composeRule.onNodeWithText("Train your brain").assertExists()
    composeRule.onNodeWithContentDescription("Memory Game").assertExists()
  }

  @Test
  fun gameCard_triggersClickAction() {
    var clicked = false
    composeRule.setContent {
      GameCard(
          title = "Reaction",
          subtitle = "Test reflexes",
          icon = Icons.Default.FlashOn,
          color = Color.Magenta,
          onClick = { clicked = true })
    }

    composeRule.onNodeWithText("Reaction").performClick()
    assert(clicked)
  }

  @Test
  fun eachGameCard_navigatesToCorrectRoute() {
    lateinit var navController: TestNavHostController

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

    val gameRoutes =
        listOf(
            "Memory Game" to "memory",
            "Reaction Test" to "reaction",
            "Focus Breathing" to "focus",
            "EduMon Runner" to "runner")

    gameRoutes.forEach { (label, route) ->
      composeRule.onNodeWithText(label).performClick()
      composeRule.waitForIdle()
      assert(navController.currentDestination?.route == route)
    }
  }
}
