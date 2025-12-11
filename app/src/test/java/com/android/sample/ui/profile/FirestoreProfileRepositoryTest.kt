package com.android.sample.ui.profile

import com.android.sample.data.UserProfile
import com.android.sample.profile.FirestoreProfileRepository
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [com.android.sample.profile.FirestoreProfileRepository]. Uses Mockito to mock
 * Firebase dependencies.
 *
 * Place in: app/src/test/java/com/android/sample/profile/FirestoreProfileRepositoryTest.kt
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreProfileRepositoryTest {

  @Mock private lateinit var mockDb: FirebaseFirestore
  @Mock private lateinit var mockAuth: FirebaseAuth
  @Mock private lateinit var mockUser: FirebaseUser
  @Mock private lateinit var mockCollection: CollectionReference
  @Mock private lateinit var mockDocument: DocumentReference
  @Mock private lateinit var mockListenerRegistration: ListenerRegistration
  @Mock private lateinit var mockSnapshot: DocumentSnapshot
  @Mock private lateinit var mockTask: Task<Void>

  @Captor
  private lateinit var authStateListenerCaptor: ArgumentCaptor<FirebaseAuth.AuthStateListener>
  @Captor
  private lateinit var snapshotListenerCaptor: ArgumentCaptor<EventListener<DocumentSnapshot>>

  private val testUid = "test-user-123"

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    whenever(mockUser.uid).thenReturn(testUid)
    whenever(mockDb.collection("users")).thenReturn(mockCollection)
    whenever(mockCollection.document(testUid)).thenReturn(mockDocument)
    whenever(mockDocument.addSnapshotListener(any<EventListener<DocumentSnapshot>>()))
        .thenReturn(mockListenerRegistration)
  }

  private fun createRepository(): FirestoreProfileRepository {
    return FirestoreProfileRepository(mockDb, mockAuth)
  }

  // ========== Initialization Tests ==========

  @Test
  fun `init with no user sets isLoaded true`() {
    whenever(mockAuth.currentUser).thenReturn(null)

    val repo = createRepository()

    Assert.assertTrue(repo.isLoaded.value)
    Assert.assertEquals(UserProfile(), repo.profile.value)
  }

  @Test
  fun `init with existing user starts listening`() {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val repo = createRepository()

    verify(mockDocument).addSnapshotListener(any<EventListener<DocumentSnapshot>>())
    Assert.assertFalse(repo.isLoaded.value)
  }

  @Test
  fun `profile StateFlow is not null`() {
    whenever(mockAuth.currentUser).thenReturn(null)

    val repo = createRepository()

    Assert.assertNotNull(repo.profile)
    Assert.assertNotNull(repo.profile.value)
  }

  @Test
  fun `isLoaded StateFlow is not null`() {
    whenever(mockAuth.currentUser).thenReturn(null)

    val repo = createRepository()

    Assert.assertNotNull(repo.isLoaded)
  }

  // ========== AuthStateListener Tests ==========

  @Test
  fun `authStateListener new user login starts listening`() {
    whenever(mockAuth.currentUser).thenReturn(null)

    val repo = createRepository()

    verify(mockAuth).addAuthStateListener(capture(authStateListenerCaptor))

    // Simulate user login
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    authStateListenerCaptor.value.onAuthStateChanged(mockAuth)

    verify(mockDocument).addSnapshotListener(any<EventListener<DocumentSnapshot>>())
  }

  @Test
  fun `authStateListener user sign out stops listening and resets profile`() {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val repo = createRepository()

    verify(mockAuth).addAuthStateListener(capture(authStateListenerCaptor))

    // Simulate sign out
    whenever(mockAuth.currentUser).thenReturn(null)
    authStateListenerCaptor.value.onAuthStateChanged(mockAuth)

    verify(mockListenerRegistration).remove()
    Assert.assertTrue(repo.isLoaded.value)
    Assert.assertEquals(UserProfile(), repo.profile.value)
  }

  @Test
  fun `authStateListener same user does not restart listener`() {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val repo = createRepository()

    verify(mockAuth).addAuthStateListener(capture(authStateListenerCaptor))

    // Trigger with same user
    authStateListenerCaptor.value.onAuthStateChanged(mockAuth)

    // Should only have one listener (from init)
    verify(mockDocument, times(1)).addSnapshotListener(any<EventListener<DocumentSnapshot>>())
  }

  @Test
  fun `authStateListener no user and never had user sets isLoaded true`() {
    whenever(mockAuth.currentUser).thenReturn(null)

    val repo = createRepository()

    verify(mockAuth).addAuthStateListener(capture(authStateListenerCaptor))
    authStateListenerCaptor.value.onAuthStateChanged(mockAuth)

    Assert.assertTrue(repo.isLoaded.value)
  }

  @Test
  fun `authStateListener different user restarts listener`() {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val repo = createRepository()

    verify(mockAuth).addAuthStateListener(capture(authStateListenerCaptor))

    // Simulate different user
    val newUser = org.mockito.Mockito.mock(FirebaseUser::class.java)
    whenever(newUser.uid).thenReturn("different-user-456")
    whenever(mockAuth.currentUser).thenReturn(newUser)
    whenever(mockCollection.document("different-user-456")).thenReturn(mockDocument)

    authStateListenerCaptor.value.onAuthStateChanged(mockAuth)

    verify(mockListenerRegistration).remove()
    verify(mockDocument, times(2)).addSnapshotListener(any<EventListener<DocumentSnapshot>>())
  }

  // ========== Snapshot Listener Tests ==========

  @Test
  fun `snapshotListener with valid data updates profile`() {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val testProfile =
        UserProfile(name = "TestUser", email = "test@test.com", starterId = "pyromon", points = 100)
    whenever(mockSnapshot.exists()).thenReturn(true)
    whenever(mockSnapshot.toObject(UserProfile::class.java)).thenReturn(testProfile)

    val repo = createRepository()

    verify(mockDocument).addSnapshotListener(capture(snapshotListenerCaptor))
    snapshotListenerCaptor.value.onEvent(mockSnapshot, null)

    Assert.assertEquals(testProfile, repo.profile.value)
    Assert.assertTrue(repo.isLoaded.value)
  }

  @Test
  fun `snapshotListener with no document sets empty profile`() {
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockSnapshot.exists()).thenReturn(false)

    val repo = createRepository()

    verify(mockDocument).addSnapshotListener(capture(snapshotListenerCaptor))
    snapshotListenerCaptor.value.onEvent(mockSnapshot, null)

    Assert.assertEquals(UserProfile(), repo.profile.value)
    Assert.assertTrue(repo.isLoaded.value)
  }

  @Test
  fun `snapshotListener with error sets isLoaded true`() {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockError = org.mockito.Mockito.mock(FirebaseFirestoreException::class.java)

    val repo = createRepository()

    verify(mockDocument).addSnapshotListener(capture(snapshotListenerCaptor))
    snapshotListenerCaptor.value.onEvent(null, mockError)

    Assert.assertTrue(repo.isLoaded.value)
  }

  @Test
  fun `snapshotListener with null data does not crash`() {
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockSnapshot.exists()).thenReturn(true)
    whenever(mockSnapshot.toObject(UserProfile::class.java)).thenReturn(null)

    val repo = createRepository()

    verify(mockDocument).addSnapshotListener(capture(snapshotListenerCaptor))
    snapshotListenerCaptor.value.onEvent(mockSnapshot, null)

    Assert.assertTrue(repo.isLoaded.value)
  }

  // ========== updateProfile Tests ==========

  @Test
  fun `updateProfile with logged in user saves to Firestore`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockDocument.set(any(), any<SetOptions>())).thenReturn(Tasks.forResult(null))

    val repo = createRepository()

    val newProfile = UserProfile(name = "Updated", starterId = "aquamon")
    repo.updateProfile(newProfile)

    verify(mockDocument).set(eq(newProfile), any<SetOptions>())
    Assert.assertEquals(newProfile, repo.profile.value)
  }

  @Test
  fun `updateProfile with no user does not save`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(null)

    val repo = createRepository()

    val newProfile = UserProfile(name = "Updated")
    repo.updateProfile(newProfile)

    verify(mockDocument, never()).set(any(), any<SetOptions>())
  }

  @Test
  fun `updateProfile updates local state immediately`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockDocument.set(any(), any<SetOptions>())).thenReturn(Tasks.forResult(null))

    val repo = createRepository()

    val newProfile = UserProfile(name = "Immediate", points = 999)
    repo.updateProfile(newProfile)

    Assert.assertEquals(newProfile, repo.profile.value)
  }

  @Test
  fun `updateProfile with Firestore error throws exception`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    val exception = Exception("Firestore error")
    whenever(mockDocument.set(any(), any<SetOptions>())).thenReturn(Tasks.forException(exception))

    val repo = createRepository()

    try {
      repo.updateProfile(UserProfile(name = "WillFail"))
      assert(false) { "Expected exception" }
    } catch (e: Exception) {
      Assert.assertEquals("Firestore error", e.message)
    }
  }

  @Test
  fun `updateProfile with all fields saves correctly`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockDocument.set(any(), any<SetOptions>())).thenReturn(Tasks.forResult(null))

    val repo = createRepository()

    val fullProfile =
        UserProfile(
            name = "FullTest",
            email = "full@test.com",
            notificationsEnabled = false,
            locationEnabled = false,
            focusModeEnabled = true,
            points = 1000,
            level = 10,
            coins = 500,
            streak = 30,
            avatarAccent = 0xFF00FF00,
            starterId = "floramon",
            accessories = listOf("owned:hat", "head:hat"))

    repo.updateProfile(fullProfile)

    verify(mockDocument).set(eq(fullProfile), any<SetOptions>())
    Assert.assertEquals(fullProfile, repo.profile.value)
  }

  // ========== stopListening Tests ==========

  @Test
  fun `stopListening removes registration`() {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val repo = createRepository()

    verify(mockAuth).addAuthStateListener(capture(authStateListenerCaptor))

    // Sign out to trigger stopListening
    whenever(mockAuth.currentUser).thenReturn(null)
    authStateListenerCaptor.value.onAuthStateChanged(mockAuth)

    verify(mockListenerRegistration).remove()
  }

  // ========== Edge Cases ==========

  @Test
  fun `startListening called twice with same uid only listens once`() {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val repo = createRepository()

    verify(mockAuth).addAuthStateListener(capture(authStateListenerCaptor))

    // Trigger multiple times with same user
    authStateListenerCaptor.value.onAuthStateChanged(mockAuth)
    authStateListenerCaptor.value.onAuthStateChanged(mockAuth)

    verify(mockDocument, times(1)).addSnapshotListener(any<EventListener<DocumentSnapshot>>())
  }
}
