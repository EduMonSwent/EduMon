package com.android.sample.ui.notifications

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.model.notifications.NotificationKind
import com.android.sample.model.notifications.NotificationRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

/**
 * Additional permission-focused tests for NotificationsViewModel to compensate for ignored UI
 * tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Android 13 to exercise POST_NOTIFICATIONS branch
class NotificationsViewModelPermissionsTest {

  private lateinit var ctx: Context
  private lateinit var repo: FakeRepo
  private lateinit var vm: NotificationsViewModel
  private lateinit var shadowApp: ShadowApplication

  private class FakeRepo : NotificationRepository {
    var oneShotCalls = 0
    var canceled = mutableListOf<NotificationKind>()

    override fun scheduleOneMinuteFromNow(context: Context) {
      oneShotCalls++
    }

    override fun setDailyEnabled(
        context: Context,
        kind: NotificationKind,
        enabled: Boolean,
        hour24: Int
    ) {}

    override fun setWeeklySchedule(
        context: Context,
        kind: NotificationKind,
        enabled: Boolean,
        times: Map<Int, Pair<Int, Int>>
    ) {}

    override fun cancel(context: Context, kind: NotificationKind) {
      canceled += kind
    }
  }

  @Before
  fun setup() {
    ctx = ApplicationProvider.getApplicationContext()
    shadowApp = shadowOf(ctx as android.app.Application)
    repo = FakeRepo()
    vm = NotificationsViewModel(repo)
    // Ensure clean permission state
    shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
    shadowApp.denyPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION)
  }

  @Test
  fun needsNotificationPermission_returns_true_when_not_granted_on_api_33() {
    assertTrue(vm.needsNotificationPermission(ctx))
  }

  @Test
  fun needsNotificationPermission_returns_false_when_granted_on_api_33() {
    shadowApp.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    assertFalse(vm.needsNotificationPermission(ctx))
  }

  @Test
  fun requestOrSchedule_invokes_launcher_when_permission_missing() {
    var launched = false
    vm.requestOrSchedule(ctx) { perm -> launched = perm == Manifest.permission.POST_NOTIFICATIONS }
    assertTrue(launched)
    assertEquals("Should not schedule until permission granted", 0, repo.oneShotCalls)
  }

  @Test
  fun requestOrSchedule_schedules_directly_when_permission_granted() {
    shadowApp.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    var launched = false
    vm.requestOrSchedule(ctx) { launched = true }
    assertFalse("Launcher should not be called when already granted", launched)
    assertEquals(1, repo.oneShotCalls)
  }

  @Test
  fun needsBackgroundLocationPermission_false_when_foreground_not_granted() {
    // No foreground permissions => should be false (can't yet ask for background)
    assertFalse(vm.needsBackgroundLocationPermission(ctx))
  }

  @Test
  fun needsBackgroundLocationPermission_true_when_foreground_granted_but_background_missing() {
    shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
    assertTrue(vm.needsBackgroundLocationPermission(ctx))
  }

  @Test
  fun needsBackgroundLocationPermission_false_when_all_location_granted() {
    shadowApp.grantPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    assertFalse(vm.needsBackgroundLocationPermission(ctx))
  }

  @Test
  fun hasBackgroundLocationPermission_reflects_grant() {
    shadowApp.grantPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    assertTrue(vm.hasBackgroundLocationPermission(ctx))
  }

  @Test
  fun hasBackgroundLocationPermission_false_when_not_granted() {
    assertFalse(vm.hasBackgroundLocationPermission(ctx))
  }
}
