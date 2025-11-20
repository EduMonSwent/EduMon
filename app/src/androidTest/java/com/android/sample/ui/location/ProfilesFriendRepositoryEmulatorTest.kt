package com.android.sample.ui.location

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.util.FirebaseEmulator
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
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
class ProfilesFriendRepositoryEmulatorTest {

  private lateinit var repo: ProfilesFriendRepository
  private var myUid: String = ""

  @Before
  fun setUp() = runBlocking {
    // Same pattern as your working test
    FirebaseEmulator.initIfNeeded(ApplicationProvider.getApplicationContext())
    FirebaseEmulator.connectIfRunning()

    assertTrue(
        "Firebase emulators not reachable (UI 4000/4400; firestore:8080, auth:9099). " +
            "Start with: firebase emulators:start --only firestore,auth",
        FirebaseEmulator.isRunning)

    FirebaseEmulator.clearAll()
    Tasks.await(FirebaseEmulator.auth.signInAnonymously())
    myUid = FirebaseEmulator.auth.currentUser!!.uid

    repo = ProfilesFriendRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
  }

  @After
  fun tearDown() = runBlocking { if (FirebaseEmulator.isRunning) FirebaseEmulator.clearAll() }

  // --- helpers ---

  private suspend fun seedProfile(
      uid: String,
      name: String,
      username: String? = null,
      lat: Double = 46.5191,
      lon: Double = 6.5668,
      mode: String = "study", // "study" | "break" | "idle"
      withLocation: Boolean = true
  ) {
    val data =
        mutableMapOf<String, Any>(
            "name" to name, "mode" to mode, "updatedAt" to FieldValue.serverTimestamp())
    if (username != null) data["username"] = username
    if (withLocation) data["location"] = GeoPoint(lat, lon)
    FirebaseEmulator.firestore.collection("profiles").document(uid).set(data).await()
  }

  // --- tests ---

  @Test
  fun friendsFlow_initially_empty() = runBlocking {
    val first = withTimeout(4000) { repo.friendsFlow.first() }
    assertTrue(first.isEmpty())
  }
  // --- addFriendByUid tests ---

  @Test
  fun addFriendByUid_persists_and_emits() = runBlocking {
    seedProfile("F1", name = "Bob", username = "bob", mode = "break")
    val status = repo.addFriendByUid("F1")
    assertEquals("Bob", status.name)
    assertEquals(FriendMode.BREAK, status.mode)

    val emitted = withTimeout(4000) { repo.friendsFlow.first { it.size == 1 } }
    assertEquals(1, emitted.size)
    assertEquals("Bob", emitted[0].name)
  }

  @Test
  fun addFriendByUid_duplicate_throws_friendly_message() = runBlocking {
    seedProfile("F2", name = "Alice", mode = "study")
    repo.addFriendByUid("F2")
    try {
      repo.addFriendByUid("F2")
      fail("Expected duplicate add to throw")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message!!.contains("already friends", ignoreCase = true))
    }
  }

  @Test
  fun addFriendByUid_self_forbidden() = runBlocking {
    try {
      repo.addFriendByUid(myUid)
      fail("Expected self-add to throw")
    } catch (e: IllegalArgumentException) {
      assertTrue(
          e.message!!.contains("can’t add yourself", ignoreCase = true) ||
              e.message!!.contains("can't add yourself", ignoreCase = true))
    }
  }

  @Test
  fun addFriendByUid_nonexistent_profile_errors() = runBlocking {
    try {
      repo.addFriendByUid("UNKNOWN_123")
      fail("Expected missing profile to throw")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message!!.contains("No user found", ignoreCase = true))
    }
  }

  @Test
  fun addFriendByUid_profile_without_location_errors() = runBlocking {
    seedProfile("F3", name = "NoLoc", mode = "study", withLocation = false)
    try {
      repo.addFriendByUid("F3")
      fail("Expected missing location to throw")
    } catch (e: IllegalStateException) {
      assertTrue(
          e.message!!.contains("hasn’t shared a location", ignoreCase = true) ||
              e.message!!.contains("hasn't shared a location", ignoreCase = true))
    }
  }

  @Test
  fun removeFriend_updates_flow() = runBlocking {
    seedProfile("F4", name = "Del", mode = "idle")
    repo.addFriendByUid("F4")
    withTimeout(4000) { repo.friendsFlow.first { it.any { f -> f.id == "F4" } } }

    repo.removeFriend("F4")

    val after = withTimeout(4000) { repo.friendsFlow.first { list -> list.none { it.id == "F4" } } }
    assertTrue(after.none { it.id == "F4" })
  }

  @Test
  fun add_many_friends_triggers_chunking_and_sorts() = runBlocking {
    // >10 UIDs triggers multiple whereIn chunks
    for (i in 0 until 12) {
      val modeStr =
          when (i % 3) {
            0 -> "study"
            1 -> "break"
            else -> "idle"
          }
      seedProfile("F$i", name = "Friend%02d".format(i), mode = modeStr)
      repo.addFriendByUid("F$i")
    }
    val list = withTimeout(6000) { repo.friendsFlow.first { it.size == 12 } }
    assertEquals("Friend00", list.first().name)
    assertEquals(12, list.size)
  }
}
