package com.android.sample.ui.flashcards.model

import com.android.sample.ui.flashcards.data.FlashcardsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Fake repo that fully controls importSharedDeck behavior. */
private class FakeImportRepo : FlashcardsRepository {

  var nextResult: String? = null
  var lastToken: String? = null
  var delayMs: Long = 0

  override fun observeDecks() = throw UnsupportedOperationException()

  override fun observeDeck(deckId: String) = throw UnsupportedOperationException()

  override suspend fun createDeck(title: String, description: String, cards: List<Flashcard>) =
      throw UnsupportedOperationException()

  override suspend fun addCard(deckId: String, card: Flashcard) =
      throw UnsupportedOperationException()

  override suspend fun deleteDeck(deckId: String) = throw UnsupportedOperationException()

  override suspend fun importSharedDeck(token: String): String {
    lastToken = token
    if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
    return nextResult ?: ""
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ImportDeckViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private lateinit var repo: FakeImportRepo
  private lateinit var vm: ImportDeckViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(dispatcher)
    repo = FakeImportRepo()
    vm = ImportDeckViewModel(repo)
  }

  @Test
  fun invalidToken_setsError() = runTest {
    repo.nextResult = "" // invalid

    vm.importToken("wrong")
    advanceUntilIdle()

    val state = vm.state.value
    assertFalse(state.success)
    assertEquals("Share code not valid", state.error)
    assertFalse(state.loading)
    assertEquals("wrong", repo.lastToken)
  }

  @Test
  fun validToken_setsSuccess() = runTest {
    repo.nextResult = "deck123"

    vm.importToken("tok")
    advanceUntilIdle()

    val state = vm.state.value
    assertTrue(state.success)
    assertNull(state.error)
  }

  @Test
  fun tokenIsTrimmed() = runTest {
    repo.nextResult = "deck999"

    vm.importToken("   abc   ")
    advanceUntilIdle()

    assertEquals("abc", repo.lastToken)
    assertTrue(vm.state.value.success)
  }

  @Test
  fun loadingIsTrueDuringImport() = runTest {
    repo.delayMs = 500
    repo.nextResult = "id"

    vm.importToken("x")

    advanceTimeBy(1) // start coroutine
    assertTrue(vm.state.value.loading)

    advanceUntilIdle()
    assertFalse(vm.state.value.loading)
  }
}
