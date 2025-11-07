package com.android.sample.notifications

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.core.app.ApplicationProvider
import com.android.sample.model.notifications.NotificationKind
import com.android.sample.model.notifications.NotificationRepository
import com.android.sample.ui.notifications.NotificationsScreen
import com.android.sample.ui.notifications.NotificationsViewModel
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

// Repo factice (aucune dépendance WorkManager)
class CapturingRepo2 : NotificationRepository {
  var oneShot = 0
  val weekly = mutableListOf<Triple<NotificationKind, Boolean, Map<Int, Pair<Int, Int>>>>()

  override fun scheduleOneMinuteFromNow(context: Context) {
    oneShot++
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
  ) {
    weekly += Triple(kind, enabled, times)
  }

  override fun cancel(context: Context, kind: NotificationKind) {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NotificationsScreenTest2 {

  @Test
  fun renders_all_branches_and_dialog_without_compose_rule() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val repo = CapturingRepo2()
    val vm = NotificationsViewModel(repo)

    val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

    activity.runOnUiThread {
      activity.setContent { NotificationsScreen(vm = vm, onBack = {}, onGoHome = {}) }
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    vm.setKickoffEnabled(ctx, true)
    activity.runOnUiThread {
      activity.setContent { NotificationsScreen(vm = vm, onBack = {}, onGoHome = {}) }
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    vm.toggleKickoffDay(Calendar.MONDAY)
    vm.updateKickoffTime(Calendar.MONDAY, 25, -1)
    activity.runOnUiThread {
      activity.setContent {
        NotificationsScreen(
            vm = vm,
            onBack = {},
            onGoHome = {},
            forceDialogForDay = Calendar.MONDAY // couvre la construction du dialog
            )
      }
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    vm.applyKickoffSchedule(ctx)
    assertTrue(repo.weekly.isNotEmpty())
    val (_, enabled, times) = repo.weekly.last()
    assertTrue(enabled)
    assertEquals(1, times.size)
    assertEquals(23 to 0, times[Calendar.MONDAY])

    vm.setStreakEnabled(ctx, true)
    activity.runOnUiThread { activity.setContent { NotificationsScreen(vm = vm) } }
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    vm.scheduleTestNotification(ctx)
    assertEquals(1, repo.oneShot)
  }
}
