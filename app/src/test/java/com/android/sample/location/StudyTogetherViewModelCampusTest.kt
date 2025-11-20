package com.android.sample.location

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.ui.location.FriendMode
import com.android.sample.ui.location.FriendRepository
import com.android.sample.ui.location.FriendStatus
import com.android.sample.ui.location.StudyTogetherViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

// Parts of this code were written with ChatGPT assistance

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class StudyTogetherViewModelCampusTest {

  private lateinit var viewModel: StudyTogetherViewModel
  private lateinit var context: Context
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockFriendRepository: FriendRepository

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()

    // Initialize Firebase (required for Firestore access)
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    // Grant necessary permissions
    val app = context as android.app.Application
    val shadowApp: ShadowApplication = shadowOf(app)
    shadowApp.grantPermissions(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION)

    // Mock Firebase Auth to return a logged-in user
    mockAuth = mockk(relaxed = true)
    val mockUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
    every { mockUser.uid } returns "test_user_123"
    every { mockAuth.currentUser } returns mockUser

    // Mock FriendRepository to avoid Firebase dependencies
    mockFriendRepository = mockk(relaxed = true)
    every { mockFriendRepository.friendsFlow } returns flowOf(emptyList<FriendStatus>())

    viewModel =
        StudyTogetherViewModel(
            friendRepository = mockFriendRepository,
            initialMode = FriendMode.STUDY,
            liveLocation = true,
            firebaseAuth = mockAuth)

    // Attach context for notification support
    viewModel.attachContext(context)

    // Clear prefs
    context.getSharedPreferences("notifications", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test
  fun `consumeLocation posts notification on campus entry when enabled`() = runTest {
    // Given: feature enabled, starting off campus
    context
        .getSharedPreferences("notifications", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("campus_entry_enabled", true)
        .commit()

    // Off campus first
    viewModel.consumeLocation(46.510, 6.550)

    // When: move to on campus
    viewModel.consumeLocation(46.5202, 6.5652)

    // Then: notification posted
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    val notifications = shadowNotificationManager.allNotifications

    assertTrue(
        "Should post campus entry notification",
        notifications.any { it.extras.getString("android.title") == "Welcome to campus" })
  }

  @Test
  fun `consumeLocation does not post notification when feature disabled`() = runTest {
    // Given: feature disabled
    context
        .getSharedPreferences("notifications", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("campus_entry_enabled", false)
        .commit()

    // When: campus entry
    viewModel.consumeLocation(46.510, 6.550)
    viewModel.consumeLocation(46.5202, 6.5652)

    // Then: no notification
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    assertEquals(0, shadowNotificationManager.allNotifications.size)
  }

  @Test
  fun `consumeLocation respects cooldown period`() = runTest {
    // Given: feature enabled
    context
        .getSharedPreferences("notifications", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("campus_entry_enabled", true)
        .commit()

    // When: enter campus twice rapidly
    viewModel.consumeLocation(46.510, 6.550)
    viewModel.consumeLocation(46.5202, 6.5652)
    viewModel.consumeLocation(46.510, 6.550)
    viewModel.consumeLocation(46.5202, 6.5652)

    // Then: only one notification (cooldown prevents second)
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    val campusNotifications =
        shadowNotificationManager.allNotifications.filter {
          it.extras.getString("android.title") == "Welcome to campus"
        }
    assertEquals("Should only post one notification due to cooldown", 1, campusNotifications.size)
  }

  @Test
  fun `consumeLocation updates UI state correctly for on campus`() = runTest {
    // When: on campus location
    viewModel.consumeLocation(46.5202, 6.5652)

    // Then
    assertTrue(viewModel.uiState.value.isOnCampus)
    assertTrue(viewModel.uiState.value.isLocationInitialized)
  }

  @Test
  fun `consumeLocation updates UI state correctly for off campus`() = runTest {
    // When: off campus location
    viewModel.consumeLocation(46.510, 6.550)

    // Then
    assertFalse(viewModel.uiState.value.isOnCampus)
    assertTrue(viewModel.uiState.value.isLocationInitialized)
  }

  @Test
  fun `consumeLocation does not notify when staying on campus`() = runTest {
    // Given: feature enabled, already on campus
    context
        .getSharedPreferences("notifications", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("campus_entry_enabled", true)
        .commit()

    viewModel.consumeLocation(46.5202, 6.5652)

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    val countAfterFirstEntry = shadowNotificationManager.allNotifications.size

    // When: move within campus
    viewModel.consumeLocation(46.5205, 6.5655)

    // Then: no new notification (count should be the same)
    val countAfterSecondCall = shadowNotificationManager.allNotifications.size
    assertEquals(
        "Should not post new notification when staying on campus",
        countAfterFirstEntry,
        countAfterSecondCall)
  }

  @Test
  fun `attachContext stores application context`() {
    // When
    viewModel.attachContext(context)

    // Then: should not throw when consuming location
    viewModel.consumeLocation(46.5202, 6.5652)
  }

  @Test
  fun `campus notification has no action intent`() = runTest {
    // Given: feature enabled
    context
        .getSharedPreferences("notifications", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("campus_entry_enabled", true)
        .commit()
    // Start off campus
    viewModel.consumeLocation(46.510, 6.550)
    // When entering campus
    viewModel.consumeLocation(46.5202, 6.5652)
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    val notification = shadowNotificationManager.allNotifications.firstOrNull()
    assertNotNull("Expected a campus notification", notification)
    assertNull(
        "Campus notification should have no contentIntent (no deep link)",
        notification?.contentIntent)
  }

  @Test
  fun `isOnEpflCampus boundary test - north edge`() = runTest {
    viewModel.consumeLocation(46.525, 6.565) // Just at north boundary
    assertTrue(viewModel.uiState.value.isOnCampus)

    viewModel.consumeLocation(46.526, 6.565) // Just beyond north
    assertFalse(viewModel.uiState.value.isOnCampus)
  }

  @Test
  fun `isOnEpflCampus boundary test - south edge`() = runTest {
    viewModel.consumeLocation(46.515, 6.565) // Just at south boundary
    assertTrue(viewModel.uiState.value.isOnCampus)

    viewModel.consumeLocation(46.514, 6.565) // Just beyond south
    assertFalse(viewModel.uiState.value.isOnCampus)
  }

  @Test
  fun `isOnEpflCampus boundary test - east edge`() = runTest {
    viewModel.consumeLocation(46.520, 6.575) // Just at east boundary
    assertTrue(viewModel.uiState.value.isOnCampus)

    viewModel.consumeLocation(46.520, 6.576) // Just beyond east
    assertFalse(viewModel.uiState.value.isOnCampus)
  }

  @Test
  fun `isOnEpflCampus boundary test - west edge`() = runTest {
    viewModel.consumeLocation(46.520, 6.555) // Just at west boundary
    assertTrue(viewModel.uiState.value.isOnCampus)

    viewModel.consumeLocation(46.520, 6.554) // Just beyond west
    assertFalse(viewModel.uiState.value.isOnCampus)
  }
}
