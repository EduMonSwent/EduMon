package com.android.sample.ui.session

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.data.FakeUserStatsRepository
import com.android.sample.feature.subjects.model.StudySubject
import com.android.sample.feature.subjects.repository.SubjectsRepository
import com.android.sample.session.StudySessionRepository
import com.android.sample.ui.location.FriendMode
import com.android.sample.ui.pomodoro.PomodoroPhase
import com.android.sample.ui.pomodoro.PomodoroState
import com.android.sample.ui.pomodoro.PomodoroViewModelContract
import com.android.sample.ui.stats.repository.FakeStatsRepository
import com.android.sample.util.FirebaseEmulator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Parts of this code were written with the assistance of Copilot tool

/**
 * Instrumented tests for StudySessionViewModel's Firebase integration, specifically the
 * updateUserMode functionality.
 */
@RunWith(AndroidJUnit4::class)
class StudySessionViewModelFirebaseTest {

  private lateinit var auth: FirebaseAuth
  private lateinit var firestore: FirebaseFirestore
  private lateinit var viewModel: StudySessionViewModel

  @Before
  fun setup() {

    val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    // Initialize and connect to emulator using the proper helper
    FirebaseEmulator.initIfNeeded(context)
    FirebaseEmulator.connectIfRunning()

    // Verify emulator is running
    assertTrue(
        "Firebase emulators not reachable (firestore:8080, auth:9099). " +
            "Start with: firebase emulators:start --only firestore,auth",
        FirebaseEmulator.isRunning)

    // Use emulator instances
    auth = FirebaseEmulator.auth
    firestore = FirebaseEmulator.firestore

    // Ensure clean state: sign out and wait
    runBlocking {
      try {
        val uid = auth.currentUser?.uid
        if (uid != null) {
          firestore.collection("profiles").document(uid).delete().await()
        }
      } catch (e: Exception) {
        android.util.Log.w("StudySessionVMFirebaseTest", "Setup cleanup failed", e)
      }
      auth.signOut()
      // Give Firebase time to process the signout
      kotlinx.coroutines.delay(200)
    }
  }

  @After
  fun tearDown() {
    runBlocking {
      // Clean up test user if exists
      val uid = auth.currentUser?.uid
      if (uid != null) {
        try {
          firestore.collection("profiles").document(uid).delete().await()
        } catch (e: Exception) {
          android.util.Log.w("StudySessionVMFirebaseTest", "Teardown cleanup failed", e)
        }
        auth.signOut()
        // Give Firebase time to process
        kotlinx.coroutines.delay(200)
      }
    }
  }

  /**
   * Helper function to wait for Firebase to update the mode field. Uses snapshot listener to react
   * to changes in real-time (pattern from StudyTogetherViewModelEmulatorTest).
   */
  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  private suspend fun waitForModeUpdate(
      uid: String,
      expectedMode: FriendMode,
      timeoutMs: Long = 6_000
  ) =
      withTimeout(timeoutMs) {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
          val ref = firestore.collection("profiles").document(uid)
          val reg =
              ref.addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) {
                  snap,
                  err ->
                if (err != null) {
                  if (cont.isActive) cont.resumeWith(Result.failure(err))
                  return@addSnapshotListener
                }
                if (snap != null && snap.exists() && snap.getString("mode") == expectedMode.name) {
                  if (cont.isActive) {
                    cont.resume(Unit, onCancellation = null)
                  }
                }
              }
          cont.invokeOnCancellation { reg.remove() }
        }
      }

  @Test
  fun updateUserMode_whenNotSignedIn_doesNothing() = runBlocking {
    // Ensure no user is signed in
    auth.signOut()

    viewModel =
        StudySessionViewModel(
            pomodoroViewModel = FakePomodoroViewModel(),
            repository = FakeStudySessionRepository(),
            userStatsRepository = FakeUserStatsRepository(),
            statsRepository = FakeStatsRepository(),
            subjectsRepository = FirebaseTestFakeSubjectsRepository())

    // Simulate starting a study session which triggers updateUserModeToStudy
    val fakePomodoro = viewModel.pomodoroViewModel as FakePomodoroViewModel
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)

    // No user signed in, so no profile document should exist
    // Test passes if no exception is thrown
    assertNull(auth.currentUser)
  }

  @Test
  fun updateUserMode_whenProfileDoesNotExist_doesNothing() = runBlocking {
    // Create an anonymous test user without a profile document
    val result = auth.signInAnonymously().await()
    val uid = result.user?.uid
    assertNotNull("Anonymous auth should create a user", uid)

    // Verify user is actually signed in
    assertNotNull("User should be signed in after anonymous auth", auth.currentUser)
    assertEquals("UID should match", uid, auth.currentUser?.uid)

    // Ensure no profile document exists
    try {
      firestore.collection("profiles").document(uid!!).delete().await()
    } catch (e: Exception) {
      android.util.Log.d(
          "StudySessionVMFirebaseTest", "Profile document didn't exist, nothing to delete", e)
    }

    viewModel =
        StudySessionViewModel(
            pomodoroViewModel = FakePomodoroViewModel(),
            repository = FakeStudySessionRepository(),
            userStatsRepository = FakeUserStatsRepository(),
            statsRepository = FakeStatsRepository(),
            subjectsRepository = FirebaseTestFakeSubjectsRepository())

    // Simulate starting a study session
    val fakePomodoro = viewModel.pomodoroViewModel as FakePomodoroViewModel
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)

    // Give time for any async operations
    kotlinx.coroutines.delay(500)

    // Verify profile document still doesn't exist (updateUserMode should return early)
    val snapshot = firestore.collection("profiles").document(uid!!).get().await()
    assert(!snapshot.exists())
  }

  @Test
  fun updateUserMode_whenProfileExists_updatesMode() = runBlocking {
    // Create an anonymous test user with a profile document
    val result = auth.signInAnonymously().await()
    val uid = result.user?.uid
    assertNotNull(uid)

    // Create profile document with IDLE mode
    firestore
        .collection("profiles")
        .document(uid!!)
        .set(mapOf("uid" to uid, "mode" to FriendMode.IDLE.name))
        .await()

    viewModel =
        StudySessionViewModel(
            pomodoroViewModel = FakePomodoroViewModel(),
            repository = FakeStudySessionRepository(),
            userStatsRepository = FakeUserStatsRepository(),
            statsRepository = FakeStatsRepository(),
            subjectsRepository = FirebaseTestFakeSubjectsRepository())

    // Simulate starting a study session (should update mode to STUDY)
    val fakePomodoro = viewModel.pomodoroViewModel as FakePomodoroViewModel
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)

    // Wait for ViewModel's flow to process the state change
    withTimeout(3_000) { viewModel.uiState.first { it.isSessionActive } }

    // Wait for Firebase to update the mode
    waitForModeUpdate(uid, FriendMode.STUDY)

    // Verify mode was updated to STUDY
    val snapshot = firestore.collection("profiles").document(uid).get().await()
    assertEquals(FriendMode.STUDY.name, snapshot.getString("mode"))

    // Simulate stopping the study session (should update mode to IDLE)
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.PAUSED)

    // Wait for Firebase to update the mode
    waitForModeUpdate(uid, FriendMode.IDLE)

    // Verify mode was updated back to IDLE
    val snapshot2 = firestore.collection("profiles").document(uid).get().await()
    assertEquals(FriendMode.IDLE.name, snapshot2.getString("mode"))
  }

  @Test
  fun updateUserMode_whenSessionStarts_setsModeToStudy() = runBlocking {
    // Create an anonymous test user with a profile
    val result = auth.signInAnonymously().await()
    val uid = result.user?.uid
    assertNotNull(uid)

    firestore
        .collection("profiles")
        .document(uid!!)
        .set(mapOf("uid" to uid, "mode" to FriendMode.IDLE.name))
        .await()

    viewModel =
        StudySessionViewModel(
            pomodoroViewModel = FakePomodoroViewModel(),
            repository = FakeStudySessionRepository(),
            userStatsRepository = FakeUserStatsRepository(),
            statsRepository = FakeStatsRepository(),
            subjectsRepository = FirebaseTestFakeSubjectsRepository())

    val fakePomodoro = viewModel.pomodoroViewModel as FakePomodoroViewModel

    // Start pomodoro timer (transition to RUNNING)
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)

    // Wait for ViewModel's flow to process the state change
    withTimeout(3_000) { viewModel.uiState.first { it.isSessionActive } }

    // Wait for Firebase to update the mode
    waitForModeUpdate(uid, FriendMode.STUDY)

    // Verify mode is STUDY
    val snapshot = firestore.collection("profiles").document(uid).get().await()
    assertEquals(FriendMode.STUDY.name, snapshot.getString("mode"))
  }

  @Test
  fun updateUserMode_whenSessionPauses_setsModeToIdle() = runBlocking {
    // Create an anonymous test user with a profile in STUDY mode
    val result = auth.signInAnonymously().await()
    val uid = result.user?.uid
    assertNotNull(uid)

    firestore
        .collection("profiles")
        .document(uid!!)
        .set(mapOf("uid" to uid, "mode" to FriendMode.STUDY.name))
        .await()

    viewModel =
        StudySessionViewModel(
            pomodoroViewModel = FakePomodoroViewModel(),
            repository = FakeStudySessionRepository(),
            userStatsRepository = FakeUserStatsRepository(),
            statsRepository = FakeStatsRepository(),
            subjectsRepository = FirebaseTestFakeSubjectsRepository())

    val fakePomodoro = viewModel.pomodoroViewModel as FakePomodoroViewModel

    // First set to RUNNING
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)

    // Wait for session to become active
    withTimeout(3_000) { viewModel.uiState.first { it.isSessionActive } }

    // Then pause (transition from RUNNING to PAUSED)
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.PAUSED)

    // Wait for session to become inactive
    withTimeout(3_000) { viewModel.uiState.first { !it.isSessionActive } }

    // Wait for Firebase to update the mode
    waitForModeUpdate(uid, FriendMode.IDLE)

    // Verify mode is IDLE
    val snapshot = firestore.collection("profiles").document(uid).get().await()
    assertEquals(FriendMode.IDLE.name, snapshot.getString("mode"))
  }
}

// Fake implementations for testing
internal class FakePomodoroViewModel : PomodoroViewModelContract {
  private val _timeLeft = MutableStateFlow(1500)
  private val _phase = MutableStateFlow(PomodoroPhase.WORK)
  private val _state = MutableStateFlow(PomodoroState.IDLE)
  private val _cycleCount = MutableStateFlow(0)

  override val timeLeft: StateFlow<Int> = _timeLeft
  override val phase: StateFlow<PomodoroPhase> = _phase
  override val state: StateFlow<PomodoroState> = _state
  override val cycleCount: StateFlow<Int> = _cycleCount

  override fun startTimer() {
    _state.value = PomodoroState.RUNNING
  }

  override fun pauseTimer() {
    _state.value = PomodoroState.PAUSED
  }

  override fun resumeTimer() {
    _state.value = PomodoroState.RUNNING
  }

  override fun resetTimer() {
    _state.value = PomodoroState.IDLE
    _phase.value = PomodoroPhase.WORK
  }

  override fun nextPhase() {
    _phase.value =
        when (_phase.value) {
          PomodoroPhase.WORK -> PomodoroPhase.SHORT_BREAK
          PomodoroPhase.SHORT_BREAK -> PomodoroPhase.LONG_BREAK
          PomodoroPhase.LONG_BREAK -> PomodoroPhase.WORK
        }
  }

  fun simulatePhaseAndState(phase: PomodoroPhase, state: PomodoroState) {
    _phase.value = phase
    _state.value = state
    _timeLeft.value = 1000
    if (state == PomodoroState.FINISHED && phase == PomodoroPhase.WORK) {
      _cycleCount.value += 1
    }
  }
}

internal class FakeStudySessionRepository : StudySessionRepository {
  override suspend fun saveCompletedSession(session: StudySessionUiState) {
    // no-op
  }

  override suspend fun getSuggestedTasks() = emptyList<com.android.sample.data.ToDo>()
}

internal class FirebaseTestFakeSubjectsRepository : SubjectsRepository {
  override val subjects: StateFlow<List<StudySubject>> = MutableStateFlow(emptyList())

  override suspend fun start() {
    // no-op
  }

  override suspend fun createSubject(name: String, colorIndex: Int) {
    // no-op
  }

  override suspend fun renameSubject(id: String, newName: String) {
    // no-op
  }

  override suspend fun deleteSubject(id: String) {
    // no-op
  }

  override suspend fun addStudyMinutesToSubject(id: String, minutes: Int) {
    // no-op
  }
}
