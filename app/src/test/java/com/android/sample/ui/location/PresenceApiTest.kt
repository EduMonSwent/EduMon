package com.android.sample.ui.location

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for PresenceApi functions: updateMyPresence and ensureMyProfile.
 * Uses Mockito to mock FirebaseAuth and FirebaseFirestore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PresenceApiTest {

  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference

  companion object {
    private const val TEST_UID = "test_user_uid_123"
    private const val TEST_NAME = "Test User"
    private const val TEST_LAT = 46.5191
    private const val TEST_LON = 6.5668
  }

  @Before
  fun setUp() {
    // Setup mock FirebaseAuth
    mockAuth = mock()
    mockUser = mock()
    whenever(mockUser.uid).thenReturn(TEST_UID)

    // Setup mock Firestore
    mockDb = mock()
    mockCollection = mock()
    mockDocument = mock()

    whenever(mockDb.collection("profiles")).thenReturn(mockCollection)
    whenever(mockCollection.document(TEST_UID)).thenReturn(mockDocument)
  }

  // ======================== updateMyPresence Tests ========================

  @Test
  fun updateMyPresence_succeeds_whenUserIsLoggedIn() = runTest {
    // Given: user is logged in
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: update presence
    updateMyPresence(
        name = TEST_NAME,
        mode = FriendMode.STUDY,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: document set is called with correct data
    verify(mockDocument).set(argThat<Map<String, Any>> { map ->
      map["name"] == TEST_NAME &&
          map["mode"] == FriendMode.STUDY.name &&
          (map["location"] as? GeoPoint)?.let { it.latitude == TEST_LAT && it.longitude == TEST_LON } == true
    }, eq(SetOptions.merge()))
  }

  @Test
  fun updateMyPresence_throwsException_whenUserNotLoggedIn() = runTest {
    // Given: user is not logged in
    whenever(mockAuth.currentUser).thenReturn(null)

    // When/Then: should throw IllegalStateException
    try {
      updateMyPresence(
          name = TEST_NAME,
          mode = FriendMode.STUDY,
          lat = TEST_LAT,
          lon = TEST_LON,
          db = mockDb,
          auth = mockAuth)
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertEquals("User must be logged in to update presence.", e.message)
    }
  }

  @Test
  fun updateMyPresence_withBreakMode_setsCorrectMode() = runTest {
    // Given: user is logged in
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: update presence with BREAK mode
    updateMyPresence(
        name = TEST_NAME,
        mode = FriendMode.BREAK,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: document set is called with BREAK mode
    verify(mockDocument).set(argThat<Map<String, Any>> { map ->
      map["mode"] == FriendMode.BREAK.name
    }, eq(SetOptions.merge()))
  }

  @Test
  fun updateMyPresence_withIdleMode_setsCorrectMode() = runTest {
    // Given: user is logged in
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: update presence with IDLE mode
    updateMyPresence(
        name = TEST_NAME,
        mode = FriendMode.IDLE,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: document set is called with IDLE mode
    verify(mockDocument).set(argThat<Map<String, Any>> { map ->
      map["mode"] == FriendMode.IDLE.name
    }, eq(SetOptions.merge()))
  }

  // ======================== ensureMyProfile Tests ========================

  @Test
  fun ensureMyProfile_throwsException_whenUserNotLoggedIn() = runTest {
    // Given: user is not logged in
    whenever(mockAuth.currentUser).thenReturn(null)

    // When/Then: should throw IllegalStateException
    try {
      ensureMyProfile(
          name = TEST_NAME,
          mode = FriendMode.STUDY,
          lat = TEST_LAT,
          lon = TEST_LON,
          db = mockDb,
          auth = mockAuth)
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertEquals("User must be logged in to ensure profile.", e.message)
    }
  }

  @Test
  fun ensureMyProfile_createsProfile_whenDocumentDoesNotExist() = runTest {
    // Given: user is logged in, document does not exist
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockSnapshot = mock<DocumentSnapshot>()
    whenever(mockSnapshot.exists()).thenReturn(false)
    whenever(mockDocument.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: ensure profile
    val result = ensureMyProfile(
        name = TEST_NAME,
        mode = FriendMode.STUDY,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: document is created and returns true
    assertTrue(result)
    verify(mockDocument).set(argThat<Map<String, Any>> { map ->
      map["name"] == TEST_NAME &&
          map["mode"] == FriendMode.STUDY.name &&
          (map["location"] as? GeoPoint) != null
    }, eq(SetOptions.merge()))
  }

  @Test
  fun ensureMyProfile_returnsTrue_whenDocumentExistsButMissingName() = runTest {
    // Given: user is logged in, document exists but name is missing
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockSnapshot = mock<DocumentSnapshot>()
    whenever(mockSnapshot.exists()).thenReturn(true)
    whenever(mockSnapshot.getString("name")).thenReturn(null) // missing name
    whenever(mockSnapshot.getString("mode")).thenReturn(FriendMode.STUDY.name)
    whenever(mockSnapshot.getGeoPoint("location")).thenReturn(GeoPoint(TEST_LAT, TEST_LON))
    whenever(mockDocument.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: ensure profile
    val result = ensureMyProfile(
        name = TEST_NAME,
        mode = FriendMode.STUDY,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: document is updated and returns true
    assertTrue(result)
    verify(mockDocument).set(any<Map<String, Any>>(), eq(SetOptions.merge()))
  }

  @Test
  fun ensureMyProfile_returnsTrue_whenDocumentExistsButMissingMode() = runTest {
    // Given: user is logged in, document exists but mode is missing
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockSnapshot = mock<DocumentSnapshot>()
    whenever(mockSnapshot.exists()).thenReturn(true)
    whenever(mockSnapshot.getString("name")).thenReturn(TEST_NAME)
    whenever(mockSnapshot.getString("mode")).thenReturn(null) // missing mode
    whenever(mockSnapshot.getGeoPoint("location")).thenReturn(GeoPoint(TEST_LAT, TEST_LON))
    whenever(mockDocument.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: ensure profile
    val result = ensureMyProfile(
        name = TEST_NAME,
        mode = FriendMode.STUDY,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: document is updated and returns true
    assertTrue(result)
    verify(mockDocument).set(any<Map<String, Any>>(), eq(SetOptions.merge()))
  }

  @Test
  fun ensureMyProfile_returnsTrue_whenDocumentExistsButMissingLocation() = runTest {
    // Given: user is logged in, document exists but location is missing
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockSnapshot = mock<DocumentSnapshot>()
    whenever(mockSnapshot.exists()).thenReturn(true)
    whenever(mockSnapshot.getString("name")).thenReturn(TEST_NAME)
    whenever(mockSnapshot.getString("mode")).thenReturn(FriendMode.STUDY.name)
    whenever(mockSnapshot.getGeoPoint("location")).thenReturn(null) // missing location
    whenever(mockDocument.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: ensure profile
    val result = ensureMyProfile(
        name = TEST_NAME,
        mode = FriendMode.STUDY,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: document is updated and returns true
    assertTrue(result)
    verify(mockDocument).set(any<Map<String, Any>>(), eq(SetOptions.merge()))
  }

  @Test
  fun ensureMyProfile_returnsFalse_whenDocumentExistsWithAllFields() = runTest {
    // Given: user is logged in, document exists with all required fields
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockSnapshot = mock<DocumentSnapshot>()
    whenever(mockSnapshot.exists()).thenReturn(true)
    whenever(mockSnapshot.getString("name")).thenReturn(TEST_NAME)
    whenever(mockSnapshot.getString("mode")).thenReturn(FriendMode.STUDY.name)
    whenever(mockSnapshot.getGeoPoint("location")).thenReturn(GeoPoint(TEST_LAT, TEST_LON))
    whenever(mockDocument.get()).thenReturn(Tasks.forResult(mockSnapshot))

    // When: ensure profile
    val result = ensureMyProfile(
        name = TEST_NAME,
        mode = FriendMode.STUDY,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: no write needed, returns false
    assertFalse(result)
    verify(mockDocument, never()).set(any<Map<String, Any>>(), any<SetOptions>())
  }

  @Test
  fun ensureMyProfile_returnsTrue_whenNameIsBlank() = runTest {
    // Given: user is logged in, document exists but name is blank
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockSnapshot = mock<DocumentSnapshot>()
    whenever(mockSnapshot.exists()).thenReturn(true)
    whenever(mockSnapshot.getString("name")).thenReturn("") // blank name
    whenever(mockSnapshot.getString("mode")).thenReturn(FriendMode.STUDY.name)
    whenever(mockSnapshot.getGeoPoint("location")).thenReturn(GeoPoint(TEST_LAT, TEST_LON))
    whenever(mockDocument.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: ensure profile
    val result = ensureMyProfile(
        name = TEST_NAME,
        mode = FriendMode.STUDY,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: document is updated (blank name treated as missing)
    assertTrue(result)
    verify(mockDocument).set(any<Map<String, Any>>(), eq(SetOptions.merge()))
  }

  @Test
  fun ensureMyProfile_returnsTrue_whenModeIsBlank() = runTest {
    // Given: user is logged in, document exists but mode is blank
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockSnapshot = mock<DocumentSnapshot>()
    whenever(mockSnapshot.exists()).thenReturn(true)
    whenever(mockSnapshot.getString("name")).thenReturn(TEST_NAME)
    whenever(mockSnapshot.getString("mode")).thenReturn("   ") // blank mode
    whenever(mockSnapshot.getGeoPoint("location")).thenReturn(GeoPoint(TEST_LAT, TEST_LON))
    whenever(mockDocument.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: ensure profile
    val result = ensureMyProfile(
        name = TEST_NAME,
        mode = FriendMode.STUDY,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: document is updated (blank mode treated as missing)
    assertTrue(result)
    verify(mockDocument).set(any<Map<String, Any>>(), eq(SetOptions.merge()))
  }

  @Test
  fun ensureMyProfile_setsCorrectFields_whenCreatingNewProfile() = runTest {
    // Given: user is logged in, document does not exist
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockSnapshot = mock<DocumentSnapshot>()
    whenever(mockSnapshot.exists()).thenReturn(false)
    whenever(mockDocument.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: ensure profile with BREAK mode
    val result = ensureMyProfile(
        name = "John Doe",
        mode = FriendMode.BREAK,
        lat = 47.0,
        lon = 8.0,
        db = mockDb,
        auth = mockAuth)

    // Then: verify all fields are set correctly
    assertTrue(result)
    verify(mockDocument).set(argThat<Map<String, Any>> { map ->
      map["name"] == "John Doe" &&
          map["mode"] == FriendMode.BREAK.name &&
          (map["location"] as? GeoPoint)?.let {
            it.latitude == 47.0 && it.longitude == 8.0
          } == true &&
          map.containsKey("createdOrEnsuredAt")
    }, eq(SetOptions.merge()))
  }

  @Test
  fun updateMyPresence_usesCorrectCollectionAndDocument() = runTest {
    // Given: user is logged in
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: update presence
    updateMyPresence(
        name = TEST_NAME,
        mode = FriendMode.STUDY,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: correct collection and document are used
    verify(mockDb).collection("profiles")
    verify(mockCollection).document(TEST_UID)
  }

  @Test
  fun ensureMyProfile_usesCorrectCollectionAndDocument() = runTest {
    // Given: user is logged in, document does not exist
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockSnapshot = mock<DocumentSnapshot>()
    whenever(mockSnapshot.exists()).thenReturn(false)
    whenever(mockDocument.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    // When: ensure profile
    ensureMyProfile(
        name = TEST_NAME,
        mode = FriendMode.STUDY,
        lat = TEST_LAT,
        lon = TEST_LON,
        db = mockDb,
        auth = mockAuth)

    // Then: correct collection and document are used
    verify(mockDb).collection("profiles")
    verify(mockCollection).document(TEST_UID)
  }
}

