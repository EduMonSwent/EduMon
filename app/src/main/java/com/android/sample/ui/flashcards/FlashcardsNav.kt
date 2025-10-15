package com.android.sample.ui.flashcards

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.flashcards.data.StudyScreen

/**
 * Sets up navigation for the Flashcards feature using Jetpack Compose Navigation. Defines routes
 * for the deck list, deck creation, and study screens.
 */
sealed interface FCRoute {
  val route: String
}

object DeckListRoute : FCRoute {
  override val route = "flashcards"
}

object CreateDeckRoute : FCRoute {
  override val route = "createDeck"
}

object StudyRoute : FCRoute {
  const val arg = "deckId"
  override val route = "study/{$arg}"
}

fun studyRoute(deckId: String) = "study/$deckId"

@Composable
fun FlashcardsApp() {
  val nav = rememberNavController()
  NavHost(navController = nav, startDestination = DeckListRoute.route) {
    composable(DeckListRoute.route) {
      DeckListScreen(
          onCreateDeck = { nav.navigate(CreateDeckRoute.route) },
          onStudyDeck = { deckId -> nav.navigate(studyRoute(deckId)) })
    }

    composable(CreateDeckRoute.route) {
      CreateDeckScreen(
          onSaved = { nav.popBackStack() }, // 🔁 go back to deck list
          onCancel = { nav.popBackStack() })
    }

    composable(StudyRoute.route) { backStack ->
      val deckId = backStack.arguments?.getString(StudyRoute.arg) ?: return@composable
      StudyScreen(deckId = deckId, onBack = { nav.popBackStack() })
    }
  }
}
