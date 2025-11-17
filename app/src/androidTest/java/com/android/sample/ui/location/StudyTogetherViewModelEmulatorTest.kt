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

  private fun logProfile(uid: String): com.google.firebase.firestore.ListenerRegistration {
    val ref = FirebaseEmulator.firestore.collection("profiles").document(uid)
    return ref.addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { s, e ->
      if (e != null) {
        println("PROFILE[$uid] ERROR: ${e.message}")
      } else if (s != null) {
        val pending = s.metadata.hasPendingWrites()
        println("PROFILE[$uid] SNAP exists=${s.exists()} pending=$pending data=${s.data}")
      } else {
        println("PROFILE[$uid] SNAP null")
      }
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

}
