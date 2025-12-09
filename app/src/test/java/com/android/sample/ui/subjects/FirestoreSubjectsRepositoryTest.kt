package com.android.sample.ui.subjects

// Parts of this code have been written using an LLM

import com.android.sample.feature.subjects.model.StudySubject
import com.android.sample.feature.subjects.repository.FirestoreSubjectsRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FirestoreSubjectsRepositoryTest {

  private lateinit var auth: FirebaseAuth
  private lateinit var firestore: FirebaseFirestore
  private lateinit var user: FirebaseUser
  private lateinit var usersCollection: CollectionReference
  private lateinit var userDoc: DocumentReference
  private lateinit var subjectsCollection: CollectionReference

  @Before
  fun setup() {
    auth = mock()
    firestore = mock()
    user = mock()
    usersCollection = mock()
    userDoc = mock()
    subjectsCollection = mock()

    whenever(user.uid).thenReturn("test-uid")
    whenever(auth.currentUser).thenReturn(user)

    whenever(firestore.collection("users")).thenReturn(usersCollection)
    whenever(usersCollection.document("test-uid")).thenReturn(userDoc)
    whenever(userDoc.collection("subjects")).thenReturn(subjectsCollection)
  }

  @Test
  fun initial_subjects_list_is_empty() {
    val repo = FirestoreSubjectsRepository(auth, firestore)

    assertEquals(emptyList<StudySubject>(), repo.subjects.value)
  }

  @Test
  fun start_registers_snapshot_listener() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    val listenerReg: ListenerRegistration = mock()

    whenever(subjectsCollection.addSnapshotListener(any<EventListener<QuerySnapshot>>()))
        .thenReturn(listenerReg)

    repo.start()

    verify(subjectsCollection).addSnapshotListener(any<EventListener<QuerySnapshot>>())
  }

  @Test
  fun start_multiple_times_only_registers_once() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    val listenerReg: ListenerRegistration = mock()

    whenever(subjectsCollection.addSnapshotListener(any<EventListener<QuerySnapshot>>()))
        .thenReturn(listenerReg)

    repo.start()
    repo.start()
    repo.start()

    verify(subjectsCollection).addSnapshotListener(any<EventListener<QuerySnapshot>>())
  }

  @Test
  fun snapshot_listener_updates_subjects_list() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    var capturedListener: EventListener<QuerySnapshot>? = null

    whenever(subjectsCollection.addSnapshotListener(any<EventListener<QuerySnapshot>>()))
        .thenAnswer {
          @Suppress("UNCHECKED_CAST")
          capturedListener = it.getArgument(0) as EventListener<QuerySnapshot>
          mock<ListenerRegistration>()
        }

    repo.start()

    val snapshot: QuerySnapshot = mock()
    val doc1: DocumentSnapshot = mock()
    val doc2: DocumentSnapshot = mock()

    whenever(snapshot.documents).thenReturn(listOf(doc1, doc2))
    whenever(doc1.id).thenReturn("sub1")
    whenever(doc1.getString("name")).thenReturn("Math")
    whenever(doc1.getLong("colorIndex")).thenReturn(2L)
    whenever(doc1.getLong("totalStudyMinutes")).thenReturn(120L)

    whenever(doc2.id).thenReturn("sub2")
    whenever(doc2.getString("name")).thenReturn("Physics")
    whenever(doc2.getLong("colorIndex")).thenReturn(4L)
    whenever(doc2.getLong("totalStudyMinutes")).thenReturn(90L)

    capturedListener!!.onEvent(snapshot, null)

    val subjects = repo.subjects.value
    assertEquals(2, subjects.size)
    assertEquals("Math", subjects[0].name)
    assertEquals("Physics", subjects[1].name)
  }

  @Test
  fun snapshot_listener_filters_out_invalid_documents() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    var capturedListener: EventListener<QuerySnapshot>? = null

    whenever(subjectsCollection.addSnapshotListener(any<EventListener<QuerySnapshot>>()))
        .thenAnswer {
          @Suppress("UNCHECKED_CAST")
          capturedListener = it.getArgument(0) as EventListener<QuerySnapshot>
          mock<ListenerRegistration>()
        }

    repo.start()

    val snapshot: QuerySnapshot = mock()
    val doc1: DocumentSnapshot = mock()
    val doc2: DocumentSnapshot = mock()

    whenever(snapshot.documents).thenReturn(listOf(doc1, doc2))
    whenever(doc1.id).thenReturn("sub1")
    whenever(doc1.getString("name")).thenReturn(null) // Invalid

    whenever(doc2.id).thenReturn("sub2")
    whenever(doc2.getString("name")).thenReturn("Physics")
    whenever(doc2.getLong("colorIndex")).thenReturn(1L)
    whenever(doc2.getLong("totalStudyMinutes")).thenReturn(60L)

    capturedListener!!.onEvent(snapshot, null)

    val subjects = repo.subjects.value
    assertEquals(1, subjects.size)
    assertEquals("Physics", subjects[0].name)
  }

  @Test
  fun snapshot_listener_handles_null_snapshot() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    var capturedListener: EventListener<QuerySnapshot>? = null

    whenever(subjectsCollection.addSnapshotListener(any<EventListener<QuerySnapshot>>()))
        .thenAnswer {
          @Suppress("UNCHECKED_CAST")
          capturedListener = it.getArgument(0) as EventListener<QuerySnapshot>
          mock<ListenerRegistration>()
        }

    repo.start()

    capturedListener!!.onEvent(null, null)

    assertEquals(emptyList<StudySubject>(), repo.subjects.value)
  }

  @Test
  fun createSubject_creates_document_with_correct_data() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    val newDoc: DocumentReference = mock()

    whenever(subjectsCollection.document()).thenReturn(newDoc)
    whenever(newDoc.set(any<Map<String, Any>>())).thenReturn(Tasks.forResult(null))

    repo.createSubject("Chemistry", 3)

    verify(newDoc).set(any<Map<String, Any>>())
  }

  @Test
  fun renameSubject_updates_document() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    val doc: DocumentReference = mock()

    whenever(subjectsCollection.document("sub1")).thenReturn(doc)
    whenever(doc.update("name", "New Name")).thenReturn(Tasks.forResult(null))

    repo.renameSubject("sub1", "New Name")

    verify(doc).update("name", "New Name")
  }

  @Test
  fun deleteSubject_deletes_document() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    val doc: DocumentReference = mock()

    whenever(subjectsCollection.document("sub1")).thenReturn(doc)
    whenever(doc.delete()).thenReturn(Tasks.forResult(null))

    repo.deleteSubject("sub1")

    verify(doc).delete()
  }

  @Test
  fun addStudyMinutesToSubject_updates_total() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    val doc: DocumentReference = mock()
    val transaction: Transaction = mock()
    val snapshot: DocumentSnapshot = mock()

    whenever(subjectsCollection.document("sub1")).thenReturn(doc)
    whenever(firestore.runTransaction(any<Transaction.Function<Any?>>())).thenAnswer { invocation ->
      @Suppress("UNCHECKED_CAST")
      val function = invocation.getArgument(0) as Transaction.Function<Any?>
      whenever(transaction.get(doc)).thenReturn(snapshot)
      whenever(snapshot.id).thenReturn("sub1")
      whenever(snapshot.getString("name")).thenReturn("Math")
      whenever(snapshot.getLong("colorIndex")).thenReturn(1L)
      whenever(snapshot.getLong("totalStudyMinutes")).thenReturn(100L)

      function.apply(transaction)
      Tasks.forResult(null)
    }

    repo.addStudyMinutesToSubject("sub1", 30)

    verify(transaction).update(doc, "totalStudyMinutes", 130)
  }

  @Test
  fun addStudyMinutesToSubject_non_positive_does_nothing() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    val doc: DocumentReference = mock()

    whenever(subjectsCollection.document("sub1")).thenReturn(doc)

    repo.addStudyMinutesToSubject("sub1", 0)
    repo.addStudyMinutesToSubject("sub1", -10)

    // Transaction should never be called
    verify(firestore, org.mockito.kotlin.never()).runTransaction(any<Transaction.Function<Any?>>())
  }

  @Test
  fun addStudyMinutesToSubject_handles_null_subject() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    val doc: DocumentReference = mock()
    val transaction: Transaction = mock()
    val snapshot: DocumentSnapshot = mock()

    whenever(subjectsCollection.document("sub1")).thenReturn(doc)
    whenever(firestore.runTransaction(any<Transaction.Function<Any?>>())).thenAnswer { invocation ->
      @Suppress("UNCHECKED_CAST")
      val function = invocation.getArgument(0) as Transaction.Function<Any?>
      whenever(transaction.get(doc)).thenReturn(snapshot)
      whenever(snapshot.id).thenReturn("sub1")
      whenever(snapshot.getString("name")).thenReturn(null) // Invalid subject

      function.apply(transaction)
      Tasks.forResult(null)
    }

    repo.addStudyMinutesToSubject("sub1", 30)

    // Should not call update when subject is invalid
    verify(transaction, org.mockito.kotlin.never()).update(any(), any<String>(), any())
  }

  @Test
  fun toStudySubject_handles_missing_colorIndex() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    var capturedListener: EventListener<QuerySnapshot>? = null

    whenever(subjectsCollection.addSnapshotListener(any<EventListener<QuerySnapshot>>()))
        .thenAnswer {
          @Suppress("UNCHECKED_CAST")
          capturedListener = it.getArgument(0) as EventListener<QuerySnapshot>
          mock<ListenerRegistration>()
        }

    repo.start()

    val snapshot: QuerySnapshot = mock()
    val doc: DocumentSnapshot = mock()

    whenever(snapshot.documents).thenReturn(listOf(doc))
    whenever(doc.id).thenReturn("sub1")
    whenever(doc.getString("name")).thenReturn("Math")
    whenever(doc.getLong("colorIndex")).thenReturn(null)
    whenever(doc.getLong("totalStudyMinutes")).thenReturn(100L)

    capturedListener!!.onEvent(snapshot, null)

    val subjects = repo.subjects.value
    assertEquals(1, subjects.size)
    assertEquals(0, subjects[0].colorIndex)
  }

  @Test
  fun toStudySubject_handles_missing_totalStudyMinutes() = runTest {
    val repo = FirestoreSubjectsRepository(auth, firestore)
    var capturedListener: EventListener<QuerySnapshot>? = null

    whenever(subjectsCollection.addSnapshotListener(any<EventListener<QuerySnapshot>>()))
        .thenAnswer {
          @Suppress("UNCHECKED_CAST")
          capturedListener = it.getArgument(0) as EventListener<QuerySnapshot>
          mock<ListenerRegistration>()
        }

    repo.start()

    val snapshot: QuerySnapshot = mock()
    val doc: DocumentSnapshot = mock()

    whenever(snapshot.documents).thenReturn(listOf(doc))
    whenever(doc.id).thenReturn("sub1")
    whenever(doc.getString("name")).thenReturn("Math")
    whenever(doc.getLong("colorIndex")).thenReturn(2L)
    whenever(doc.getLong("totalStudyMinutes")).thenReturn(null)

    capturedListener!!.onEvent(snapshot, null)

    val subjects = repo.subjects.value
    assertEquals(1, subjects.size)
    assertEquals(0, subjects[0].totalStudyMinutes)
  }

  @Test(expected = IllegalStateException::class)
  fun uid_throws_when_no_user() {
    whenever(auth.currentUser).thenReturn(null)
    val repo = FirestoreSubjectsRepository(auth, firestore)

    // Access a property that uses uid
    runTest { repo.createSubject("Test", 0) }
  }
}
