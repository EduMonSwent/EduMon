package com.android.sample.ui.session

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
  private val testDispatcher = StandardTestDispatcher()

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    // Configure Firebase emulator BEFORE getting instances
    try {
      FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
      FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
    } catch (e: IllegalStateException) {
      android.util.Log.d("StudySessionVMFirebaseTest", "Firebase emulator already configured", e)
    }

    auth = FirebaseAuth.getInstance()
    firestore = FirebaseFirestore.getInstance()

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
   * Helper function to wait for Firebase to update the mode field. Polls every 100ms for up to 3
   * seconds.
   */
  private suspend fun waitForModeUpdate(uid: String, expectedMode: FriendMode) {
    val maxAttempts = 30 // 3 seconds total (30 * 100ms)
    var attempts = 0

    while (attempts < maxAttempts) {
      val snapshot = firestore.collection("profiles").document(uid).get().await()
      val currentMode = snapshot.getString("mode")

      if (currentMode == expectedMode.name) {
        return // Success!
      }

      kotlinx.coroutines.delay(100)
      attempts++
    }

    // If we get here, timeout occurred
    val snapshot = firestore.collection("profiles").document(uid).get().await()
    val actualMode = snapshot.getString("mode")
    throw AssertionError(
        "Timeout waiting for mode update. Expected: ${expectedMode.name}, Actual: $actualMode")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun updateUserMode_whenNotSignedIn_doesNothing() = runTest {
    // Ensure no user is signed in
    auth.signOut()
    advanceUntilIdle()

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
    advanceUntilIdle()

    // No user signed in, so no profile document should exist
    // Test passes if no exception is thrown
    assertNull(auth.currentUser)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun updateUserMode_whenProfileDoesNotExist_doesNothing() = runTest {
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
    advanceUntilIdle()

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

    // Give time for ViewModel to react
    kotlinx.coroutines.delay(1000)

    // Wait for Firebase to update the mode
    waitForModeUpdate(uid, FriendMode.STUDY)

    // Verify mode was updated to STUDY
    val snapshot = firestore.collection("profiles").document(uid).get().await()
    assertEquals(FriendMode.STUDY.name, snapshot.getString("mode"))

    // Simulate stopping the study session (should update mode to IDLE)
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.PAUSED)

    // Give time for ViewModel to react
    kotlinx.coroutines.delay(1000)

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
    kotlinx.coroutines.delay(1000)

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
    kotlinx.coroutines.delay(1000)

    // Then pause (transition from RUNNING to PAUSED)
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.PAUSED)
    kotlinx.coroutines.delay(1000)

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
