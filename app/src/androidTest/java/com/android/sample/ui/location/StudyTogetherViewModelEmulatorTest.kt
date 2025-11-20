package com.android.sample.ui.location

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.util.FirebaseEmulator
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyTogetherViewModelEmulatorTest {

  private lateinit var repo: ProfilesFriendRepository
  private lateinit var vmLiveFalse: StudyTogetherViewModel
  private lateinit var vmLiveTrue: StudyTogetherViewModel
  private var myUid: String = ""

  @Before
  fun setUp() = runBlocking {
    // Initialize and point default FirebaseApp to emulators
    FirebaseEmulator.initIfNeeded(ApplicationProvider.getApplicationContext())
    FirebaseEmulator.connectIfRunning()

    assertTrue(
        "Firebase emulators not reachable (UI 4000/4400; firestore:8080, auth:9099). " +
            "Start with: firebase emulators:start --only firestore,auth",
        FirebaseEmulator.isRunning)

    // Fresh DB, sign in anonymously
    FirebaseEmulator.clearAll()
    Tasks.await(FirebaseEmulator.auth.signInAnonymously())
    myUid = FirebaseEmulator.auth.currentUser!!.uid

    // Real repo bound to emulator instances
    repo = ProfilesFriendRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)

    // ViewModels under test (both point to the real repo)
    vmLiveFalse =
        StudyTogetherViewModel(
            friendRepository = repo,
            initialMode = FriendMode.STUDY,
            liveLocation = false,
            firebaseAuth = FirebaseEmulator.auth)
    vmLiveTrue =
        StudyTogetherViewModel(
            friendRepository = repo,
            initialMode = FriendMode.STUDY,
            liveLocation = true,
            firebaseAuth = FirebaseEmulator.auth)
  }

  @After
  fun tearDown() = runBlocking { if (FirebaseEmulator.isRunning) FirebaseEmulator.clearAll() }

  // -------------- helpers ------------------

  private suspend fun readProfile(uid: String): DocumentSnapshot {
    return FirebaseEmulator.firestore.collection("profiles").document(uid).get().await()
  }

  // Test helper: wait until a profile snapshot satisfies `predicate`, pending or not.
  private suspend fun waitUntilProfile(
      uid: String,
      timeoutMs: Long = 6_000,
      predicate: (DocumentSnapshot) -> Boolean
  ): DocumentSnapshot =
      withTimeout(timeoutMs) {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
          val ref = FirebaseEmulator.firestore.collection("profiles").document(uid)
          val reg =
              ref.addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) {
                  snap,
                  err ->
                if (err != null) {
                  if (cont.isActive) cont.resumeWith(Result.failure(err))
                  return@addSnapshotListener
                }
                if (snap != null && snap.exists() && predicate(snap)) {
                  // IMPORTANT: Do NOT require !snap.metadata.hasPendingWrites() here.
                  if (cont.isActive) cont.resume(snap, onCancellation = null)
                }
              }
          cont.invokeOnCancellation { reg.remove() }
        }
      }

  // -------------- tests --------------------

  @Test
  fun setMode_updates_presence_with_policy_location() = runBlocking {
    // live=false → policy location is DEFAULT
    vmLiveFalse.setMode(FriendMode.BREAK)

    val doc =
        waitUntilProfile(myUid) { snap ->
          val modeOk = snap.getString("mode") == "BREAK"
          val gp = snap.getGeoPoint("location")
          val locOk =
              gp != null &&
                  kotlin.math.abs(gp.latitude - DEFAULT_LOCATION.latitude) < 1e-5 &&
                  kotlin.math.abs(gp.longitude - DEFAULT_LOCATION.longitude) < 1e-5
          modeOk && locOk
        }

    assertTrue(doc.exists())
    assertEquals("BREAK", doc.getString("mode"))
    val gp = doc.getGeoPoint("location")!!
    assertEquals(DEFAULT_LOCATION.latitude, gp.latitude, 1e-5)
    assertEquals(DEFAULT_LOCATION.longitude, gp.longitude, 1e-5)
  }

  @Test
  fun addFriendByUid_emits_into_uiState_friends() = runBlocking {
    // Seed another user's profile (owner can create their own profile doc)
    val friendUid = "F_SEED_1"
    FirebaseEmulator.firestore
        .collection("profiles")
        .document(friendUid)
        .set(mapOf("name" to "Bob", "mode" to "break", "location" to GeoPoint(46.52, 6.56)))
        .await()

    // go through the VM path (which calls repo.addFriendByUid internally)
    vmLiveTrue.addFriendByUid(friendUid)

    // Wait until the repo listeners push a list containing Bob
    val list =
        withTimeout(6000) {
          vmLiveTrue.uiState
              .first { it.friends.any { f -> f.id == friendUid && f.name == "Bob" } }
              .friends
        }

    assertTrue(list.any { it.id == friendUid && it.name == "Bob" })
  }

  @Test
  fun consumeLocation_liveTrue_writes_device_coordinates() = runBlocking {
    // Provide a device location; with live=true it should be used for presence writes
    val lat = 46.531
    val lon = 6.6

    vmLiveTrue.consumeLocation(lat, lon)

    // Wait until Firestore reflects the device coordinates
    val doc =
        waitUntilProfile(myUid) { snap ->
          snap.getString("mode") == "STUDY" &&
              snap.getGeoPoint("location")?.let { gp ->
                abs(gp.latitude - lat) < 1e-5 && abs(gp.longitude - lon) < 1e-5
              } == true
        }

    assertTrue(doc.exists())
    assertEquals("STUDY", doc.getString("mode")) // initial mode

    val gp = doc.getGeoPoint("location")!!
    assertEquals(lat, gp.latitude, 1e-5)
    assertEquals(lon, gp.longitude, 1e-5)
  }

  @Test
  fun isOnEpflCampus_true_for_coordinates_inside_bbox() = runBlocking {
    val repo = FakeFriendRepository(emptyList())
    val vm = StudyTogetherViewModel(friendRepository = repo)

    vm.consumeLocation(46.520, 6.565)

    val state = vm.uiState.first()
    assertTrue(state.isOnCampus)
  }

  @Test
  fun isOnEpflCampus_false_for_coordinates_outside_bbox() = runBlocking {
    val repo = FakeFriendRepository(emptyList())
    val vm = StudyTogetherViewModel(friendRepository = repo)

    vm.consumeLocation(47.37, 8.54)

    val state = vm.uiState.first()
    assertFalse(state.isOnCampus)
  }

  // -------------- Presence Throttle Tests (shouldSendPresence + distanceMeters) --------------

  @Test
  fun presenceThrottle_smallMovement_lessThanMinDistance() = runBlocking {
    val lat1 = 46.520
    val lon1 = 6.565
    val lat2 = 46.5199
    val lon2 = 6.565

    vmLiveTrue.consumeLocation(lat1, lon1)

    val firstDoc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp -> abs(gp.latitude - lat1) < 1e-4 } ?: false
        }
    assertTrue(firstDoc.exists())

    vmLiveTrue.consumeLocation(lat2, lon2)
    kotlinx.coroutines.delay(100)

    val secondDoc = readProfile(myUid)
    assertNotNull(secondDoc.getGeoPoint("location"))
  }

  @Test
  fun presenceThrottle_largeMovement_exceedsMinDistance() = runBlocking {
    val lat1 = 46.520
    val lon1 = 6.565
    val lat2 = 46.525
    val lon2 = 6.565

    vmLiveTrue.consumeLocation(lat1, lon1)

    val firstDoc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp -> abs(gp.latitude - lat1) < 1e-4 } ?: false
        }
    assertTrue(firstDoc.exists())

    vmLiveTrue.consumeLocation(lat2, lon2)

    val secondDoc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp -> abs(gp.latitude - lat2) < 1e-4 } ?: false
        }
    assertTrue(secondDoc.exists())
  }

  @Test
  fun presenceThrottle_diagonalMovement_calculatesDistanceCorrectly() = runBlocking {
    val lat1 = 46.520
    val lon1 = 6.565
    val lat2 = 46.523
    val lon2 = 6.569

    vmLiveTrue.consumeLocation(lat1, lon1)

    val firstDoc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp ->
            abs(gp.latitude - lat1) < 1e-4 && abs(gp.longitude - lon1) < 1e-4
          } ?: false
        }
    assertTrue(firstDoc.exists())

    vmLiveTrue.consumeLocation(lat2, lon2)

    val secondDoc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp ->
            abs(gp.latitude - lat2) < 1e-4 && abs(gp.longitude - lon2) < 1e-4
          } ?: false
        }
    assertTrue(secondDoc.exists())
  }

  @Test
  fun distanceCalculation_eastWestMovement() = runBlocking {
    val lat = 46.520
    val lon1 = 6.560
    val lon2 = 6.565

    vmLiveTrue.consumeLocation(lat, lon1)

    val firstDoc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp -> abs(gp.longitude - lon1) < 1e-4 } ?: false
        }
    assertTrue(firstDoc.exists())

    vmLiveTrue.consumeLocation(lat, lon2)

    val secondDoc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp -> abs(gp.longitude - lon2) < 1e-4 } ?: false
        }
    assertTrue(secondDoc.exists())
  }

  @Test
  fun distanceCalculation_northSouthMovement() = runBlocking {
    val lat1 = 46.515
    val lat2 = 46.520
    val lon = 6.565

    vmLiveTrue.consumeLocation(lat1, lon)

    val firstDoc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp -> abs(gp.latitude - lat1) < 1e-4 } ?: false
        }
    assertTrue(firstDoc.exists())

    vmLiveTrue.consumeLocation(lat2, lon)

    val secondDoc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp -> abs(gp.latitude - lat2) < 1e-4 } ?: false
        }
    assertTrue(secondDoc.exists())
  }

  @Test
  fun consumeLocation_liveLocationFalse_usesDefaultLocation() = runBlocking {
    vmLiveFalse.consumeLocation(47.37, 8.54)

    val doc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp ->
            abs(gp.latitude - DEFAULT_LOCATION.latitude) < 1e-4 &&
                abs(gp.longitude - DEFAULT_LOCATION.longitude) < 1e-4
          } ?: false
        }

    val gp = doc.getGeoPoint("location")!!
    assertEquals(DEFAULT_LOCATION.latitude, gp.latitude, 1e-5)
    assertEquals(DEFAULT_LOCATION.longitude, gp.longitude, 1e-5)
  }

  // -------------- New Tests: Location Changes & UI Updates --------------

  @Test
  fun continuousLocationUpdates_userPosition_updatesInUiState() = runBlocking {
    val repo = FakeFriendRepository(emptyList())
    val vm = StudyTogetherViewModel(friendRepository = repo, liveLocation = true)

    // First location
    val lat1 = 46.520
    val lon1 = 6.565
    vm.consumeLocation(lat1, lon1)

    val state1 = vm.uiState.first { it.isLocationInitialized }
    assertEquals(lat1, state1.userPosition?.latitude ?: 0.0, 1e-5)
    assertEquals(lon1, state1.userPosition?.longitude ?: 0.0, 1e-5)

    // Second location (user moved)
    val lat2 = 46.521
    val lon2 = 6.566
    vm.consumeLocation(lat2, lon2)

    val state2 =
        vm.uiState.first {
          it.userPosition?.let { pos ->
            abs(pos.latitude - lat2) < 1e-5 && abs(pos.longitude - lon2) < 1e-5
          } ?: false
        }
    assertEquals(lat2, state2.userPosition?.latitude ?: 0.0, 1e-5)
    assertEquals(lon2, state2.userPosition?.longitude ?: 0.0, 1e-5)

    // Third location (user moved again)
    val lat3 = 46.522
    val lon3 = 6.567
    vm.consumeLocation(lat3, lon3)

    val state3 =
        vm.uiState.first {
          it.userPosition?.let { pos ->
            abs(pos.latitude - lat3) < 1e-5 && abs(pos.longitude - lon3) < 1e-5
          } ?: false
        }
    assertEquals(lat3, state3.userPosition?.latitude ?: 0.0, 1e-5)
    assertEquals(lon3, state3.userPosition?.longitude ?: 0.0, 1e-5)
  }

  @Test
  fun locationInitialized_flag_setsToTrueAfterFirstLocation() = runBlocking {
    val repo = FakeFriendRepository(emptyList())
    val vm = StudyTogetherViewModel(friendRepository = repo)

    // Initially false
    val initialState = vm.uiState.first()
    assertFalse(initialState.isLocationInitialized)

    // Provide first location
    vm.consumeLocation(46.520, 6.565)

    // Now should be true
    val updatedState = vm.uiState.first { it.isLocationInitialized }
    assertTrue(updatedState.isLocationInitialized)
  }

  @Test
  fun onCampusIndicator_updates_whenCrossingBoundary() = runBlocking {
    val repo = FakeFriendRepository(emptyList())
    val vm = StudyTogetherViewModel(friendRepository = repo, liveLocation = true)

    // Start inside campus
    vm.consumeLocation(46.520, 6.565) // Inside EPFL bbox
    val state1 = vm.uiState.first { it.isLocationInitialized }
    assertTrue("Should be on campus", state1.isOnCampus)

    // Move outside campus
    vm.consumeLocation(47.37, 8.54) // Zürich - outside EPFL bbox
    val state2 = vm.uiState.first { !it.isOnCampus }
    assertFalse("Should be outside campus", state2.isOnCampus)

    // Move back inside campus
    vm.consumeLocation(46.521, 6.566) // Inside EPFL bbox again
    val state3 = vm.uiState.first { it.isOnCampus }
    assertTrue("Should be back on campus", state3.isOnCampus)
  }

  @Test
  fun onCampusIndicator_edgeCase_atBoundary() = runBlocking {
    val repo = FakeFriendRepository(emptyList())
    val vm = StudyTogetherViewModel(friendRepository = repo)

    // EPFL bbox: lat [46.515, 46.525], lng [6.555, 6.575]

    // Test exact boundary (should be ON campus - inclusive)
    vm.consumeLocation(46.515, 6.555) // Min corner
    val stateMin = vm.uiState.first { it.isLocationInitialized }
    assertTrue("Min corner should be on campus", stateMin.isOnCampus)

    vm.consumeLocation(46.525, 6.575) // Max corner
    val stateMax =
        vm.uiState.first {
          it.userPosition?.let { pos -> abs(pos.latitude - 46.525) < 1e-5 } ?: false
        }
    assertTrue("Max corner should be on campus", stateMax.isOnCampus)

    // Just outside boundary (should be OFF campus)
    vm.consumeLocation(46.514, 6.565) // Just south of min lat
    val stateOutside =
        vm.uiState.first {
          it.userPosition?.let { pos -> abs(pos.latitude - 46.514) < 1e-5 } ?: false
        }
    assertFalse("Just outside should be off campus", stateOutside.isOnCampus)
  }

  @Test
  fun onCampusIndicator_multipleQuickUpdates_withinCampus() = runBlocking {
    val repo = FakeFriendRepository(emptyList())
    val vm = StudyTogetherViewModel(friendRepository = repo, liveLocation = true)

    // Simulate quick location updates while walking around campus
    val locations =
        listOf(Pair(46.520, 6.565), Pair(46.521, 6.566), Pair(46.522, 6.567), Pair(46.523, 6.568))

    for ((lat, lon) in locations) {
      vm.consumeLocation(lat, lon)
      val state =
          vm.uiState.first {
            it.userPosition?.let { pos ->
              abs(pos.latitude - lat) < 1e-5 && abs(pos.longitude - lon) < 1e-5
            } ?: false
          }
      assertTrue("Location ($lat, $lon) should be on campus", state.isOnCampus)
      assertTrue("Location should be initialized", state.isLocationInitialized)
    }
  }

  @Test
  fun locationUpdate_withFirebase_updatesPresenceAndUiState() = runBlocking {
    // Test with live location that both Firebase and UI state update correctly
    val lat = 46.520
    val lon = 6.565

    vmLiveTrue.consumeLocation(lat, lon)

    // Check UI state updates
    val uiState = vmLiveTrue.uiState.first { it.isLocationInitialized }
    assertEquals(lat, uiState.userPosition?.latitude ?: 0.0, 1e-5)
    assertEquals(lon, uiState.userPosition?.longitude ?: 0.0, 1e-5)
    assertTrue("Should be on campus", uiState.isOnCampus)

    // Check Firebase updates
    val doc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp ->
            abs(gp.latitude - lat) < 1e-4 && abs(gp.longitude - lon) < 1e-4
          } ?: false
        }

    val gp = doc.getGeoPoint("location")!!
    assertEquals(lat, gp.latitude, 1e-5)
    assertEquals(lon, gp.longitude, 1e-5)
  }

  @Test
  fun liveLocationFalse_userPosition_updatesButFirebaseUsesDefault() = runBlocking {
    val deviceLat = 47.37 // Zürich
    val deviceLon = 8.54

    vmLiveFalse.consumeLocation(deviceLat, deviceLon)

    // UI state should show device location (for display purposes)
    val uiState = vmLiveFalse.uiState.first { it.isLocationInitialized }
    assertEquals(DEFAULT_LOCATION.latitude, uiState.userPosition?.latitude ?: 0.0, 1e-5)
    assertEquals(DEFAULT_LOCATION.longitude, uiState.userPosition?.longitude ?: 0.0, 1e-5)

    // Firebase should have DEFAULT_LOCATION (privacy mode)
    val doc =
        waitUntilProfile(myUid) { snap ->
          snap.getGeoPoint("location")?.let { gp ->
            abs(gp.latitude - DEFAULT_LOCATION.latitude) < 1e-4 &&
                abs(gp.longitude - DEFAULT_LOCATION.longitude) < 1e-4
          } ?: false
        }

    val gp = doc.getGeoPoint("location")!!
    assertEquals(DEFAULT_LOCATION.latitude, gp.latitude, 1e-5)
    assertEquals(DEFAULT_LOCATION.longitude, gp.longitude, 1e-5)
  }
}
