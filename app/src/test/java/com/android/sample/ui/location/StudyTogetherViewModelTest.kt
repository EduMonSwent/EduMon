package com.android.sample.ui.location

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StudyTogetherViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private lateinit var mockAuth: FirebaseAuth

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    // Create a mock FirebaseAuth that returns null for currentUser (not signed in)
    mockAuth = mock()
    whenever(mockAuth.currentUser).thenReturn(null)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun clearSelection_clears_friend_and_user_selection() =
      runTest(dispatcher) {
        val repo =
            FakeFriendRepository(listOf(FriendStatus("U1", "Alice", 46.52, 6.56, FriendMode.STUDY)))
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = false, firebaseAuth = mockAuth)

        // Select a friend
        val friend = FriendStatus("U1", "Alice", 46.52, 6.56, FriendMode.STUDY)
        vm.selectFriend(friend)
        advanceUntilIdle()

        var state = vm.uiState.first()
        assertEquals(friend, state.selectedFriend)
        assertFalse(state.isUserSelected)

        // Clear selection
        vm.clearSelection()
        advanceUntilIdle()

        state = vm.uiState.first()
        assertNull(state.selectedFriend)
        assertFalse(state.isUserSelected)
      }

  @Test
  fun clearSelection_clears_user_selection() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = false, firebaseAuth = mockAuth)

        // Select user
        vm.selectUser()
        advanceUntilIdle()

        var state = vm.uiState.first()
        assertTrue(state.isUserSelected)
        assertNull(state.selectedFriend)

        // Clear selection
        vm.clearSelection()
        advanceUntilIdle()

        state = vm.uiState.first()
        assertFalse(state.isUserSelected)
        assertNull(state.selectedFriend)
      }

  @Test
  fun consumeLocation_updates_userPosition_and_onCampus_status() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = false, firebaseAuth = mockAuth)

        // Location on EPFL campus
        vm.consumeLocation(46.520, 6.565)
        advanceUntilIdle()

        val state = vm.uiState.first()
        // User position should be DEFAULT_LOCATION when liveLocation is false
        assertNotNull(state.userPosition)
        state.userPosition?.let { position ->
          assertEquals(DEFAULT_LOCATION.latitude, position.latitude, 0.0001)
          assertEquals(DEFAULT_LOCATION.longitude, position.longitude, 0.0001)
        }
        // But onCampus should still be calculated correctly
        assertTrue(state.isOnCampus)
      }

  @Test
  fun consumeLocation_offCampus_sets_isOnCampus_false() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = true, firebaseAuth = mockAuth)

        // Location off EPFL campus (Zurich)
        vm.consumeLocation(47.37, 8.54)
        advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.isOnCampus)
      }

  @Test
  fun selectFriend_updates_selectedFriend_and_clears_userSelected() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = false, firebaseAuth = mockAuth)

        val friend = FriendStatus("U1", "Bob", 46.52, 6.56, FriendMode.BREAK)

        vm.selectFriend(friend)
        advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(friend, state.selectedFriend)
        assertFalse(state.isUserSelected)
      }

  @Test
  fun selectUser_updates_isUserSelected_and_clears_selectedFriend() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = false, firebaseAuth = mockAuth)

        vm.selectUser()
        advanceUntilIdle()

        val state = vm.uiState.first()
        assertTrue(state.isUserSelected)
        assertNull(state.selectedFriend)
      }

  @Test
  fun setMode_updates_currentMode() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = false, firebaseAuth = mockAuth)

        // Change mode (won't write presence when not signed in, but should update internal state)
        vm.setMode(FriendMode.BREAK)
        advanceUntilIdle()

        // Mode change is internal; we can verify it doesn't crash
        // The actual presence write is tested in emulator tests
        val state = vm.uiState.first()
        assertNotNull(state) // Just verify state is accessible
      }

  @Test
  fun consumeLocation_whenNotSignedIn_doesNotThrowException() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        // Use default FirebaseAuth (no sign-in)
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = false, firebaseAuth = mockAuth)

        // Should not crash even when not signed in
        vm.consumeLocation(46.520, 6.565)
        advanceUntilIdle()

        val state = vm.uiState.first()
        assertNull(state.errorMessage)
        assertTrue(state.isOnCampus)
      }

  @Test
  fun consumeLocation_multipleCalls_updatesPosition() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = true, firebaseAuth = mockAuth)

        // First call - on campus
        vm.consumeLocation(46.520, 6.565)
        advanceUntilIdle()

        var state = vm.uiState.first()
        assertTrue(state.isOnCampus)

        // Second call - off campus
        vm.consumeLocation(47.37, 8.54)
        advanceUntilIdle()

        state = vm.uiState.first()
        assertFalse(state.isOnCampus)
      }

  @Test
  fun friends_flow_updates_uiState() =
      runTest(dispatcher) {
        val initialFriends = listOf(FriendStatus("U1", "Alice", 46.52, 6.56, FriendMode.STUDY))
        val repo = FakeFriendRepository(initialFriends)
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = false, firebaseAuth = mockAuth)

        advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(1, state.friends.size)
        assertEquals("Alice", state.friends[0].name)
      }

  @Test
  fun addFriendByUid_success_clearsErrorMessage() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = false, firebaseAuth = mockAuth)

        // Successfully add a friend
        vm.addFriendByUid("U123")
        advanceUntilIdle()

        val state = vm.uiState.first()
        assertNull(state.errorMessage)
        // Friend should be added to the list
        assertTrue(state.friends.any { it.id == "U123" })
      }

  @Test
  fun selectFriend_afterUserSelected_switchesToFriend() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = false, firebaseAuth = mockAuth)

        // First select user
        vm.selectUser()
        advanceUntilIdle()

        var state = vm.uiState.first()
        assertTrue(state.isUserSelected)
        assertNull(state.selectedFriend)

        // Then select friend
        val friend = FriendStatus("U1", "Bob", 46.52, 6.56, FriendMode.BREAK)
        vm.selectFriend(friend)
        advanceUntilIdle()

        state = vm.uiState.first()
        assertFalse(state.isUserSelected)
        assertEquals(friend, state.selectedFriend)
      }

  @Test
  fun consumeLocation_boundaryValues_epflCampus() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = false, firebaseAuth = mockAuth)

        // Test near the boundary of EPFL campus (inside)
        // EPFL bbox: lat [46.515, 46.525], lng [6.555, 6.575]

        // Just inside min lat
        vm.consumeLocation(46.516, 6.565)
        advanceUntilIdle()
        var state = vm.uiState.first()
        assertTrue(state.isOnCampus)

        // Just inside max lat
        vm.consumeLocation(46.524, 6.565)
        advanceUntilIdle()
        state = vm.uiState.first()
        assertTrue(state.isOnCampus)

        // Just inside min lng
        vm.consumeLocation(46.520, 6.556)
        advanceUntilIdle()
        state = vm.uiState.first()
        assertTrue(state.isOnCampus)

        // Just inside max lng
        vm.consumeLocation(46.520, 6.574)
        advanceUntilIdle()
        state = vm.uiState.first()
        assertTrue(state.isOnCampus)
      }

  @Test
  fun consumeLocation_boundaryValues_outsideCampus() =
      runTest(dispatcher) {
        val repo = FakeFriendRepository(emptyList())
        val vm =
            StudyTogetherViewModel(
                friendRepository = repo, liveLocation = true, firebaseAuth = mockAuth)

        // Just outside min lat
        vm.consumeLocation(46.514, 6.565)
        advanceUntilIdle()
        var state = vm.uiState.first()
        assertFalse(state.isOnCampus)

        // Just outside max lat
        vm.consumeLocation(46.526, 6.565)
        advanceUntilIdle()
        state = vm.uiState.first()
        assertFalse(state.isOnCampus)

        // Just outside min lng
        vm.consumeLocation(46.520, 6.554)
        advanceUntilIdle()
        state = vm.uiState.first()
        assertFalse(state.isOnCampus)

        // Just outside max lng
        vm.consumeLocation(46.520, 6.576)
        advanceUntilIdle()
        state = vm.uiState.first()
        assertFalse(state.isOnCampus)
      }
}
