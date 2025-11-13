package com.android.sample.ui.flashcards.data

import com.android.sample.core.helpers.DefaultDispatcherProvider
import com.android.sample.core.helpers.DispatcherProvider
import com.android.sample.ui.flashcards.model.Deck
import com.android.sample.ui.flashcards.model.Flashcard
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Firestore implementation aligned with models: Deck(id, title, description, createdAt: Long,
 * cards: List<Flashcard>) Flashcard(id, question, answer)
 *
 * Firestore schema: users/{uid}/decks/{deckId} -> { id, title, description, createdAtMillis,
 * updatedAt } users/{uid}/decks/{deckId}/cards/{cardId} -> { id, q, a, createdAt, updatedAt }
 */
class FirestoreFlashcardsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : FlashcardsRepository {

  // ---------- helpers ----------
  private fun isSignedIn(): Boolean = auth.currentUser != null

  private fun decksCol(): CollectionReference {
    val uid =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to access flashcards")
    return db.collection("users").document(uid).collection("decks")
  }

  private fun deckDoc(deckId: String) = decksCol().document(deckId)

  private fun cardsCol(deckId: String) = deckDoc(deckId).collection("cards")

  // Firestore DTOs (only the stored fields; ids come from document ids)
  private data class DeckFs(
      val title: String = "",
      val description: String = "",
      val createdAtMillis: Long? = null,
  )

  private data class CardFs(
      val q: String = "",
      val a: String = "",
  )

  private fun DocumentSnapshot.toDeckFs(): DeckFs =
      DeckFs(
          title = getString("title") ?: "",
          description = getString("description") ?: "",
          createdAtMillis = getLong("createdAtMillis"),
      )

  private fun DocumentSnapshot.toCardFs(): CardFs =
      CardFs(q = getString("q") ?: "", a = getString("a") ?: "")

  private fun deckFrom(id: String, fs: DeckFs, cards: List<Flashcard>): Deck =
      Deck(
          id = id,
          title = fs.title.ifBlank { "New deck" },
          description = fs.description,
          createdAt = fs.createdAtMillis ?: System.currentTimeMillis(),
          cards = cards.toMutableList())

  private fun cardFrom(id: String, fs: CardFs): Flashcard =
      Flashcard(id = id, question = fs.q, answer = fs.a)

  // ---------- API (Flows) ----------
  override fun observeDecks(): Flow<List<Deck>> = callbackFlow {
    if (!isSignedIn()) {
      trySend(emptyList())
      awaitClose {}
      return@callbackFlow
    }

    val reg =
        decksCol().addSnapshotListener { decksSnap, err ->
          if (err != null) {
            close(err)
            return@addSnapshotListener
          }
          val snap = decksSnap ?: return@addSnapshotListener

          launch(dispatchers.io) {
            try {
              val decks =
                  snap.documents.map { d ->
                    val deckId = d.id
                    val deckFs = d.toDeckFs()
                    val cardsSnap = Tasks.await(cardsCol(deckId).get())
                    val cards =
                        cardsSnap.documents
                            .map { it.id to it.toCardFs() }
                            .filter { (_, c) -> c.q.isNotBlank() && c.a.isNotBlank() }
                            .map { (id, c) -> cardFrom(id, c) }
                    deckFrom(deckId, deckFs, cards)
                  }
              trySend(decks).isSuccess
            } catch (t: Throwable) {
              close(t)
            }
          }
        }
    awaitClose { reg.remove() }
  }

  override fun observeDeck(deckId: String): Flow<Deck?> = callbackFlow {
    if (!isSignedIn()) {
      trySend(null)
      awaitClose {}
      return@callbackFlow
    }

    val reg =
        deckDoc(deckId).addSnapshotListener { docSnap, err ->
          if (err != null) {
            close(err)
            return@addSnapshotListener
          }
          if (docSnap == null || !docSnap.exists()) {
            trySend(null)
            return@addSnapshotListener
          }

          launch(dispatchers.io) {
            try {
              val deckFs = docSnap.toDeckFs()
              val cardsSnap = Tasks.await(cardsCol(deckId).get())
              val cards =
                  cardsSnap.documents
                      .map { it.id to it.toCardFs() }
                      .filter { (_, c) -> c.q.isNotBlank() && c.a.isNotBlank() }
                      .map { (id, c) -> cardFrom(id, c) }

              trySend(deckFrom(deckId, deckFs, cards)).isSuccess
            } catch (t: Throwable) {
              close(t)
            }
          }
        }
    awaitClose { reg.remove() }
  }

  // ---------- Mutations ----------
  override suspend fun createDeck(
      title: String,
      description: String,
      cards: List<Flashcard>
  ): String =
      withContext(dispatchers.io) {
        if (!isSignedIn()) {
          // keep contract: still return an id, but perform no remote write
          return@withContext UUID.randomUUID().toString()
        }

        val newId = UUID.randomUUID().toString()
        val deckRef = deckDoc(newId)
        val nowMs = System.currentTimeMillis()

        db.runBatch { b ->
              // deck doc (store id + createdAtMillis so it fits your model)
              val deckData =
                  hashMapOf(
                      "id" to newId,
                      "title" to title.ifBlank { "New deck" },
                      "description" to description,
                      "createdAtMillis" to nowMs,
                      "updatedAt" to FieldValue.serverTimestamp(),
                  )
              b.set(deckRef, deckData, SetOptions.merge())

              // cards subcollection (use card.id as document id)
              cards
                  .filter { it.question.isNotBlank() && it.answer.isNotBlank() }
                  .forEach { c ->
                    val cardId = c.id.ifEmpty { UUID.randomUUID().toString() }
                    val cardRef = cardsCol(newId).document(cardId)
                    b.set(
                        cardRef,
                        hashMapOf(
                            "id" to cardId,
                            "q" to c.question,
                            "a" to c.answer,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge())
                  }
            }
            .let { Tasks.await(it) }

        newId
      }

  override suspend fun addCard(deckId: String, card: Flashcard) =
      withContext(dispatchers.io) {
        if (!isSignedIn()) return@withContext
        if (card.question.isBlank() || card.answer.isBlank()) return@withContext

        val cardId = card.id.ifEmpty { UUID.randomUUID().toString() }

        Tasks.await(
            cardsCol(deckId)
                .document(cardId)
                .set(
                    mapOf(
                        "id" to cardId,
                        "q" to card.question,
                        "a" to card.answer,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge()))

        // touch deck.updatedAt
        Tasks.await(deckDoc(deckId).update("updatedAt", FieldValue.serverTimestamp()))
      }

  override suspend fun deleteDeck(deckId: String) =
      withContext(dispatchers.io) {
        if (!isSignedIn()) return@withContext

        // delete cards in batches, then the deck doc
        suspend fun deleteCardsBatchOnce(): Int {
          val snap = Tasks.await(cardsCol(deckId).limit(400).get())
          if (snap.isEmpty) return 0
          val batch = db.batch()
          snap.documents.forEach { batch.delete(it.reference) }
          Tasks.await(batch.commit())
          return snap.size()
        }

        while (deleteCardsBatchOnce() > 0) {
          /* keep deleting */
        }
        Tasks.await(deckDoc(deckId).delete())
      }
}
