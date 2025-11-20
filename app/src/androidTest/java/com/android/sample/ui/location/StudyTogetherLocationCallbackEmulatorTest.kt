package com.android.sample.ui.location

import android.Manifest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.sample.util.FirebaseEmulator
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for StudyTogetherScreen location callback logic using Firebase Emulator.
 * These tests verify that location updates (both chosen and actual GPS) work correctly
 * and persist to Firestore when needed.
 */
@RunWith(AndroidJUnit4::class)
class StudyTogetherLocationCallbackEmulatorTest {

  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  private lateinit var firestore: FirebaseFirestore
  private lateinit var repo: ProfilesFriendRepository

  @Before
  fun setUp() = runBlocking {
    // Skip all tests if emulator is not running
    assumeTrue("Firebase Emulator must be running for this test", FirebaseEmulator.isRunning)

    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.clearFirestoreEmulator()

    // Sign in anonymously
    Tasks.await(FirebaseEmulator.auth.signInAnonymously())

    firestore = FirebaseEmulator.firestore
    repo = ProfilesFriendRepository(firestore, FirebaseEmulator.auth)
  }

  @After
  fun tearDown() = runBlocking {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.clearFirestoreEmulator()
      FirebaseEmulator.clearAuthEmulator()
    }
  }

  @Test
  fun locationCallback_withChooseLocation_true_usesChosenCoordinates() {
    val vm = StudyTogetherViewModel(friendRepository = repo)

    // EPFL coordinates
    val epflLat = 46.5191
    val epflLng = 6.5668
    val chosenLocation = LatLng(epflLat, epflLng)

    composeTestRule.setContent {
      StudyTogetherScreen(
          viewModel = vm, showMap = false, chooseLocation = true, chosenLocation = chosenLocation)
    }

    // Wait for location processing
    composeTestRule.waitForIdle()

    // Verify chosen location was used
    composeTestRule.onNodeWithTag("on_campus_indicator").assertExists()
    composeTestRule.onNodeWithText("On EPFL campus").assertExists()

    // Verify ViewModel state matches chosen location
    composeTestRule.runOnUiThread {
      val state = vm.uiState.value
      assert(state.isLocationInitialized) { "Location should be initialized" }
      assert(kotlin.math.abs(state.effectiveUserLatLng.latitude - epflLat) < 0.0001) {
        "Latitude should match chosen location (expected: $epflLat, got: ${state.effectiveUserLatLng.latitude})"
      }
      assert(kotlin.math.abs(state.effectiveUserLatLng.longitude - epflLng) < 0.0001) {
        "Longitude should match chosen location (expected: $epflLng, got: ${state.effectiveUserLatLng.longitude})"
      }
    }
  }


  @Test
  fun locationCallback_multipleUpdates_reflectsLatestLocation() {
    val vm = StudyTogetherViewModel(friendRepository = repo)

    composeTestRule.setContent {
      StudyTogetherScreen(viewModel = vm, showMap = false, chooseLocation = false)
    }

    // First update: EPFL (on campus)
    composeTestRule.runOnUiThread { vm.consumeLocation(46.5191, 6.5668) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("On EPFL campus").assertExists()

    // Second update: Zurich (off campus)
    composeTestRule.runOnUiThread { vm.consumeLocation(47.3769, 8.5417) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Outside of EPFL campus").assertExists()

    // Third update: Back to EPFL
    composeTestRule.runOnUiThread { vm.consumeLocation(46.520, 6.565) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("On EPFL campus").assertExists()

    // Verify final state
    composeTestRule.runOnUiThread {
      val state = vm.uiState.value
      assert(state.isOnCampus) { "Should be on campus after final update" }
    }
  }


  @Test
  fun locationCallback_edgeCaseBoundary_correctlyDetectsOnOffCampus() {
    val vm = StudyTogetherViewModel(friendRepository = repo)

    composeTestRule.setContent {
      StudyTogetherScreen(viewModel = vm, showMap = false, chooseLocation = false)
    }

    // Test exact boundary: just inside EPFL bbox
    // EPFL bbox: lat [46.515, 46.525], lng [6.555, 6.575]
    composeTestRule.runOnUiThread {
      vm.consumeLocation(46.516, 6.556) // Just inside
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("On EPFL campus").assertExists()

    // Test just outside lower boundary
    composeTestRule.runOnUiThread {
      vm.consumeLocation(46.514, 6.556) // Just outside (lat too low)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Outside of EPFL campus").assertExists()

    // Test just outside upper boundary
    composeTestRule.runOnUiThread {
      vm.consumeLocation(46.526, 6.565) // Just outside (lat too high)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Outside of EPFL campus").assertExists()

    // Test center of EPFL
    composeTestRule.runOnUiThread {
      vm.consumeLocation(46.520, 6.565) // Center
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("On EPFL campus").assertExists()
  }
}

