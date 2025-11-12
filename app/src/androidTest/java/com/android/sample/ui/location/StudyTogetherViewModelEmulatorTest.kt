package com.android.sample.ui.location

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.util.FirebaseEmulator
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.delay
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
            friendRepository = repo, initialMode = FriendMode.STUDY, liveLocation = false)
    vmLiveTrue =
        StudyTogetherViewModel(
            friendRepository = repo, initialMode = FriendMode.STUDY, liveLocation = true)
  }

  @After
  fun tearDown() = runBlocking { if (FirebaseEmulator.isRunning) FirebaseEmulator.clearAll() }

  // -------------- helpers ------------------

  private suspend fun readProfile(uid: String): DocumentSnapshot {
    return FirebaseEmulator.firestore.collection("profiles").document(uid).get().await()
  }

  private fun GeoPoint.toLatLng() = com.google.android.gms.maps.model.LatLng(latitude, longitude)

  // -------------- tests --------------------

  @Test
  fun setMode_updates_presence_with_policy_location() = runBlocking {
    // Use the live=false VM, so policy location is DEFAULT
    vmLiveFalse.setMode(FriendMode.BREAK)

    // Small wait to let write complete
    delay(150)

    val doc = readProfile(myUid)
    assertTrue(doc.exists())
    assertEquals("BREAK", doc.getString("mode")) // updateMyPresence writes enum name

    val gp = doc.getGeoPoint("location")!!
    // Still DEFAULT since live=false
    assertEquals(DEFAULT_LOCATION.latitude, gp.latitude, 1e-5)
    assertEquals(DEFAULT_LOCATION.longitude, gp.longitude, 1e-5)
  }

  @Test
  fun addFriendByUid_emits_into_uiState_friends() = runBlocking {
    // Seed another user's profile (owner can create their own profile doc)
    // For emulator convenience we allow create by any signed-in user in rules,
    // otherwise seed via a secondary app as that owner.
    val friendUid = "F_SEED_1"
    FirebaseEmulator.firestore
        .collection("profiles")
        .document(friendUid)
        .set(
            mapOf(
                "name" to "Bob",
                "mode" to "break",
                "location" to com.google.firebase.firestore.GeoPoint(46.52, 6.56)))
        .await()

    // go through the VM path (which calls repo.addFriendByUid internally)
    vmLiveTrue.addFriendByUid(friendUid)

    // Wait until the repo listeners push a list containing Bob
    val list =
        withTimeout(4000) {
          vmLiveTrue.uiState
              .first { it.friends.any { f -> f.id == friendUid && f.name == "Bob" } }
              .friends
        }

    assertTrue(list.any { it.id == friendUid && it.name == "Bob" })
  }
}
