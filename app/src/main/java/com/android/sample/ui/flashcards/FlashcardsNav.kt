package com.android.sample.ui.flashcards

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.flashcards.data.StudyScreen

/** Flashcards Navigation */
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

object ImportRoute : FCRoute {
  override val route = "import"
}

fun studyRoute(deckId: String) = "study/$deckId"

@Composable
fun FlashcardsApp() {

  val nav = rememberNavController()

  NavHost(navController = nav, startDestination = DeckListRoute.route) {

    // DECK LIST
    composable(DeckListRoute.route) {
      DeckListScreen(
          onCreateDeck = { nav.navigate(CreateDeckRoute.route) },
          onStudyDeck = { deckId -> nav.navigate(studyRoute(deckId)) },
          onImportDeck = { nav.navigate(ImportRoute.route) })
    }

    // CREATE DECK
    composable(CreateDeckRoute.route) {
      CreateDeckScreen(onSaved = { nav.popBackStack() }, onCancel = { nav.popBackStack() })
    }

    // STUDY DECK
    composable(StudyRoute.route) { backStack ->
      val deckId = backStack.arguments?.getString(StudyRoute.arg) ?: return@composable
      StudyScreen(deckId = deckId, onBack = { nav.popBackStack() })
    }

    // IMPORT SHARED DECK
    composable(ImportRoute.route) {
      ImportDeckScreen(onSuccess = { nav.popBackStack() }, onBack = { nav.popBackStack() })
    }
  }
}
