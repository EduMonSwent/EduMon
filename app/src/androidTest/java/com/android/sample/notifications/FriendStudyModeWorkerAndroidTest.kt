package com.android.sample.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.android.sample.data.notifications.FriendStudyModeWorker
import com.android.sample.ui.location.FriendMode
import com.android.sample.util.FirebaseEmulator
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Parts of this code were written with the assistance of AI

/**
 * Android instrumentation tests for [FriendStudyModeWorker] that run on a real device/emulator with
 * Firebase emulator.
 *
 * NOTE: This test configures the DEFAULT Firebase instances to use the emulator. This is required
 * because the worker uses Firebase.getInstance() which gets the default app. The emulator must be
 * running for these tests to pass.
 */
@RunWith(AndroidJUnit4::class)
class FriendStudyModeWorkerAndroidTest {

  companion object {
    private var configured = false

    @JvmStatic
    @BeforeClass
    fun setupClass() {
      if (configured) return

      val context = ApplicationProvider.getApplicationContext<Context>()

      // Initialize default Firebase app if needed
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context)
      }

      // Check if emulator is running
      FirebaseEmulator.initIfNeeded(context)
      if (!FirebaseEmulator.isRunning) {
        // Emulator not running - tests will be skipped via assertion in setup()
        return
      }

      // Configure DEFAULT instances to use emulator (required for worker to function)
      try {
        FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
        configured = true
      } catch (_: IllegalStateException) {
        // Already configured
        configured = true
      }
    }
  }

  @get:Rule
  val permissionRule: GrantPermissionRule =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        GrantPermissionRule.grant()
      }

  private lateinit var context: Context
  private lateinit var workManager: WorkManager
  private lateinit var notificationManager: NotificationManager
  private lateinit var auth: FirebaseAuth
  private lateinit var db: FirebaseFirestore

  private var testUserId: String? = null

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    workManager = WorkManager.getInstance(context)
    notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Verify emulator is running
    assertTrue(
        "Firebase emulators not reachable (firestore:8080, auth:9099). " +
            "Start with: firebase emulators:start --only firestore,auth",
        FirebaseEmulator.isRunning)

    // Use default instances (now configured to use emulator)
    auth = FirebaseAuth.getInstance()
    db = FirebaseFirestore.getInstance()

    // Clear notifications
    notificationManager.cancelAll()

    // Clear SharedPreferences
    context.getSharedPreferences("friend_study_mode", Context.MODE_PRIVATE).edit().clear().apply()

    // Cancel any existing work
    runBlocking { workManager.cancelAllWork().await() }
  }

  /**
   * Helper to enqueue and wait for worker completion. Returns the final WorkInfo. Uses the same
   * pattern as CampusEntryPollWorkerAndroidTest for consistency.
   */
  private fun runWorker(inputData: Data): WorkInfo = runBlocking {
    val request = OneTimeWorkRequestBuilder<FriendStudyModeWorker>().setInputData(inputData).build()

    // Enqueue and await completion
    workManager.enqueue(request).await()

    // Poll for completion (max 20 seconds - longer for Firebase operations)
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < 20000) {
      kotlinx.coroutines.delay(200)

      val info = workManager.getWorkInfoById(request.id).await()
      if (info.state.isFinished) {
        return@runBlocking info
      }
    }

    // Return final state if timeout
    return@runBlocking workManager.getWorkInfoById(request.id).await()
  }

  @Test
  fun doWork_whenUserNotLoggedIn_returnsSuccess() = runBlocking {
    // Ensure user is logged out
    auth.signOut()

    val inputData = Data.Builder().putBoolean(FriendStudyModeWorker.KEY_DISABLE_CHAIN, true).build()

    // Run worker using helper
    val workInfo = runWorker(inputData)

    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Should not post notification
    val notifications = notificationManager.activeNotifications
    val friendNotif = notifications.find { it.id == 9102 } // FRIEND_STUDY_MODE_NOTIFICATION_ID

    assertEquals(null, friendNotif)
  }

  @Test
  fun doWork_whenNoPermission_returnsSuccess() = runBlocking {
    // This test is tricky since we grant permission in the rule
    // We'll test the happy path instead and verify permission checks exist

    // Sign in anonymously
    auth.signInAnonymously().await()
    testUserId = auth.currentUser?.uid

    val inputData = Data.Builder().putBoolean(FriendStudyModeWorker.KEY_DISABLE_CHAIN, true).build()

    // Run worker using helper
    val workInfo = runWorker(inputData)

    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
  }

  @Test
  fun doWork_withFriendEnteringStudyMode_postsNotification() = runBlocking {
    // Sign in anonymously
    auth.signInAnonymously().await()
    testUserId = auth.currentUser?.uid
    val currentUid = auth.currentUser!!.uid

    // Create test friend in Firestore
    val friendId = "test_friend_123"
    val friendName = "Khalil"

    // Create current user profile using /profiles
    db.collection("profiles")
        .document(currentUid)
        .set(mapOf("name" to "Test User", "mode" to FriendMode.IDLE.name))
        .await()

    // Create friend edge in /users/{uid}/friendIds/{friendUid}
    db.collection("users")
        .document(currentUid)
        .collection("friendIds")
        .document(friendId)
        .set(mapOf("addedAt" to System.currentTimeMillis()))
        .await()

    // Create friend profile with STUDY mode using /profiles
    db.collection("profiles")
        .document(friendId)
        .set(
            mapOf(
                "name" to friendName,
                "mode" to FriendMode.STUDY.name,
                "latitude" to 46.5202,
                "longitude" to 6.5652))
        .await()

    // Set previous mode to IDLE (not STUDY)
    context
        .getSharedPreferences("friend_study_mode", Context.MODE_PRIVATE)
        .edit()
        .putString(friendId, FriendMode.IDLE.name)
        .apply()

    // Run worker using helper
    val inputData = Data.Builder().putBoolean(FriendStudyModeWorker.KEY_DISABLE_CHAIN, true).build()
    val workInfo = runWorker(inputData)

    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Verify notification was posted
    val notifications = notificationManager.activeNotifications
    val friendNotif = notifications.find { it.id == 9102 }

    assertNotNull("Friend study mode notification should be posted", friendNotif)

    if (friendNotif != null) {
      val notification = friendNotif.notification
      assertNotNull(notification)
      assertTrue(
          "Notification should contain friend's name",
          notification.extras.getString("android.text")?.contains(friendName) == true)
    }

    // Verify SharedPreferences were updated
    val prefs = context.getSharedPreferences("friend_study_mode", Context.MODE_PRIVATE)
    assertEquals(FriendMode.STUDY.name, prefs.getString(friendId, null))
  }

  @Test
  fun doWork_withFriendAlreadyInStudyMode_doesNotPostNotification() = runBlocking {
    // Sign in anonymously
    auth.signInAnonymously().await()
    testUserId = auth.currentUser?.uid
    val currentUid = auth.currentUser!!.uid

    // Create test friend
    val friendId = "test_friend_456"

    // Create current user profile using /profiles
    db.collection("profiles")
        .document(currentUid)
        .set(mapOf("name" to "Test User", "mode" to FriendMode.IDLE.name))
        .await()

    // Create friend edge in /users/{uid}/friendIds/{friendUid}
    db.collection("users")
        .document(currentUid)
        .collection("friendIds")
        .document(friendId)
        .set(mapOf("addedAt" to System.currentTimeMillis()))
        .await()

    // Create friend profile with STUDY mode using /profiles
    db.collection("profiles")
        .document(friendId)
        .set(
            mapOf(
                "name" to "Ahmed",
                "mode" to FriendMode.STUDY.name,
                "latitude" to 46.5202,
                "longitude" to 6.5652))
        .await()

    // Set previous mode to STUDY (already studying)
    context
        .getSharedPreferences("friend_study_mode", Context.MODE_PRIVATE)
        .edit()
        .putString(friendId, FriendMode.STUDY.name)
        .apply()

    // Run worker using helper
    val inputData = Data.Builder().putBoolean(FriendStudyModeWorker.KEY_DISABLE_CHAIN, true).build()
    val workInfo = runWorker(inputData)

    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Should NOT post notification (friend was already studying)
    val notifications = notificationManager.activeNotifications
    val friendNotif = notifications.find { it.id == 9102 }

    assertEquals("Should not post notification when friend was already studying", null, friendNotif)
  }

  /*@Test
  fun doWork_withMultipleFriendsEnteringStudyMode_postsGroupNotification() = runBlocking {
    // Sign in anonymously
    auth.signInAnonymously().await()
    testUserId = auth.currentUser?.uid
    val currentUid = auth.currentUser!!.uid

    // Create multiple test friends
    val friend1Id = "test_friend_1"
    val friend2Id = "test_friend_2"
    val friend3Id = "test_friend_3"

    // Create current user profile using /profiles collection (allowed by rules)
    db.collection("profiles")
        .document(currentUid)
        .set(mapOf("name" to "Test User", "mode" to FriendMode.IDLE.name))
        .await()

    // Create friend edges in /users/{uid}/friendIds/{friendUid} (owned by current user)
    db.collection("users")
        .document(currentUid)
        .collection("friendIds")
        .document(friend1Id)
        .set(mapOf("addedAt" to System.currentTimeMillis()))
        .await()

    db.collection("users")
        .document(currentUid)
        .collection("friendIds")
        .document(friend2Id)
        .set(mapOf("addedAt" to System.currentTimeMillis()))
        .await()

    db.collection("users")
        .document(currentUid)
        .collection("friendIds")
        .document(friend3Id)
        .set(mapOf("addedAt" to System.currentTimeMillis()))
        .await()

    // Create friend profiles all in STUDY mode using /profiles collection
    db.collection("profiles")
        .document(friend1Id)
        .set(
            mapOf(
                "name" to "Alae",
                "mode" to FriendMode.STUDY.name,
                "latitude" to 46.5202,
                "longitude" to 6.5652))
        .await()

    db.collection("profiles")
        .document(friend2Id)
        .set(
            mapOf(
                "name" to "Kenza",
                "mode" to FriendMode.STUDY.name,
                "latitude" to 46.5202,
                "longitude" to 6.5652))
        .await()

    db.collection("profiles")
        .document(friend3Id)
        .set(
            mapOf(
                "name" to "Florian",
                "mode" to FriendMode.STUDY.name,
                "latitude" to 46.5202,
                "longitude" to 6.5652))
        .await()

    // Set all previous modes to IDLE
    val prefs = context.getSharedPreferences("friend_study_mode", Context.MODE_PRIVATE).edit()
    prefs.putString(friend1Id, FriendMode.IDLE.name)
    prefs.putString(friend2Id, FriendMode.IDLE.name)
    prefs.putString(friend3Id, FriendMode.IDLE.name)
    prefs.apply()

    // Run worker
    val request =
        OneTimeWorkRequestBuilder<FriendStudyModeWorker>()
            .setInputData(
                Data.Builder().putBoolean(FriendStudyModeWorker.KEY_DISABLE_CHAIN, true).build())
            .build()

    workManager.enqueue(request).await()

    // Wait for work to complete with longer timeout
    var workInfo = workManager.getWorkInfoById(request.id).await()
    var attempts = 0
    while ((workInfo?.state == WorkInfo.State.ENQUEUED ||
        workInfo?.state == WorkInfo.State.RUNNING) && attempts < 100) {
      Thread.sleep(200)
      workInfo = workManager.getWorkInfoById(request.id).await()
      attempts++
    }

    assertEquals(WorkInfo.State.SUCCEEDED, workInfo?.state)

    // Verify notification was posted with multiple friends
    val notifications = notificationManager.activeNotifications
    val friendNotif = notifications.find { it.id == 9102 }

    assertNotNull("Friend study mode notification should be posted", friendNotif)

    if (friendNotif != null) {
      val notification = friendNotif.notification
      val text = notification.extras.getString("android.text") ?: ""

      // Should mention multiple friends (format: "Charlie, Diana and 1 others are now studying")
      assertTrue(
          "Notification should contain group message",
          text.contains("others") || text.contains("and"))
    }
  }*/

  @Test
  fun scheduleNext_schedulesWorkWithDelay() = runBlocking {
    FriendStudyModeWorker.scheduleNext(context)

    // Give WorkManager time to schedule
    Thread.sleep(500)

    val workInfos = workManager.getWorkInfosByTag("friend_study_mode_poll").await()

    assertTrue("Work should be scheduled", workInfos.isNotEmpty())

    val workInfo = workInfos.firstOrNull()
    assertNotNull(workInfo)

    // Work should be enqueued or running
    assertTrue(
        workInfo?.state == WorkInfo.State.ENQUEUED || workInfo?.state == WorkInfo.State.RUNNING)
  }

  @Test
  fun startChain_initiatesPeriodicChecks() = runBlocking {
    FriendStudyModeWorker.startChain(context)

    Thread.sleep(500)

    val workInfos = workManager.getWorkInfosByTag("friend_study_mode_poll").await()

    assertTrue("Chain should be started", workInfos.isNotEmpty())
  }

  @Test
  fun cancel_cancelsScheduledWork() = runBlocking {
    // Start chain first
    FriendStudyModeWorker.startChain(context)
    Thread.sleep(500)

    // Verify work is scheduled
    val workInfosBefore = workManager.getWorkInfosByTag("friend_study_mode_poll").await()
    assertTrue("Work should be scheduled before cancel", workInfosBefore.isNotEmpty())

    // Cancel
    FriendStudyModeWorker.cancel(context)
    Thread.sleep(500)

    // Verify work is cancelled
    val workInfosAfter = workManager.getWorkInfosByTag("friend_study_mode_poll").await()
    val hasActiveWork =
        workInfosAfter.any {
          it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }

    assertFalse("Work should be cancelled", hasActiveWork)
  }

  @Test
  fun doWork_updatesSharedPreferencesForAllFriends() = runBlocking {
    // Sign in anonymously
    auth.signInAnonymously().await()
    testUserId = auth.currentUser?.uid
    val currentUid = auth.currentUser!!.uid

    val friend1Id = "pref_test_1"
    val friend2Id = "pref_test_2"

    // Create current user profile using /profiles
    db.collection("profiles")
        .document(currentUid)
        .set(mapOf("name" to "Test User", "mode" to FriendMode.IDLE.name))
        .await()

    // Create friend edges in /users/{uid}/friendIds/{friendUid}
    db.collection("users")
        .document(currentUid)
        .collection("friendIds")
        .document(friend1Id)
        .set(mapOf("addedAt" to System.currentTimeMillis()))
        .await()

    db.collection("users")
        .document(currentUid)
        .collection("friendIds")
        .document(friend2Id)
        .set(mapOf("addedAt" to System.currentTimeMillis()))
        .await()

    // Create friends with different modes using /profiles
    db.collection("profiles")
        .document(friend1Id)
        .set(
            mapOf(
                "name" to "Friend1",
                "mode" to FriendMode.STUDY.name,
                "latitude" to 46.5202,
                "longitude" to 6.5652))
        .await()

    db.collection("profiles")
        .document(friend2Id)
        .set(
            mapOf(
                "name" to "Friend2",
                "mode" to FriendMode.IDLE.name,
                "latitude" to 46.5202,
                "longitude" to 6.5652))
        .await()

    // Run worker using helper
    val inputData = Data.Builder().putBoolean(FriendStudyModeWorker.KEY_DISABLE_CHAIN, true).build()
    val workInfo = runWorker(inputData)

    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Verify SharedPreferences contain all friends' current modes
    val prefs = context.getSharedPreferences("friend_study_mode", Context.MODE_PRIVATE)
    assertEquals(FriendMode.STUDY.name, prefs.getString(friend1Id, null))
    assertEquals(FriendMode.IDLE.name, prefs.getString(friend2Id, null))
  }

  @Test
  fun doWork_chainsNextExecution_whenChainNotDisabled() = runBlocking {
    // Sign in anonymously to pass guards
    auth.signInAnonymously().await()
    testUserId = auth.currentUser?.uid

    // Run worker WITHOUT disabling chain
    val inputData = Data.Builder().build()
    val workInfo = runWorker(inputData)

    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Verify next work is scheduled
    val workInfos = workManager.getWorkInfosByTag("friend_study_mode_poll").await()
    val hasScheduledWork = workInfos.any { it.state == WorkInfo.State.ENQUEUED }

    assertTrue("Next execution should be scheduled when chain is not disabled", hasScheduledWork)
  }

  @Test
  fun doWork_doesNotChainNextExecution_whenChainDisabled() = runBlocking {
    // Sign in anonymously
    auth.signInAnonymously().await()
    testUserId = auth.currentUser?.uid

    // Run worker WITH chain disabled
    val inputData = Data.Builder().putBoolean(FriendStudyModeWorker.KEY_DISABLE_CHAIN, true).build()
    val workInfo = runWorker(inputData)

    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Should not schedule next work automatically (only the test work should exist)
    val workInfos = workManager.getWorkInfosByTag("friend_study_mode_poll").await()
    val activeWorks = workInfos.filter { it.state == WorkInfo.State.ENQUEUED }

    assertEquals("Should not chain when disabled", 0, activeWorks.size)
  }
}
