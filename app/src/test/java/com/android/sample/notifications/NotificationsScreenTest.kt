package com.android.sample.notifications

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.notifications.NotificationKind
import com.android.sample.model.notifications.NotificationRepository
import com.android.sample.ui.notifications.NotificationsScreen
import com.android.sample.ui.notifications.NotificationsViewModel
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private class CapturingRepo : NotificationRepository {
  var oneShot = 0
  var weekly = mutableListOf<Triple<NotificationKind, Boolean, Map<Int, Pair<Int, Int>>>>()

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

@RunWith(AndroidJUnit4::class)
class NotificationsScreenTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun renders_updates_time_and_applies_schedule() {
    val repo = CapturingRepo()
    val vm = NotificationsViewModel(repo)
    val ctx = ApplicationProvider.getApplicationContext<Context>()

    compose.setContent { NotificationsScreen(vm = vm, onBack = {}, onGoHome = {}) }

    compose.onNodeWithTag("notifications_title").assertExists()

    // ON d'abord, sinon les contrôles sont désactivés
    vm.setKickoffEnabled(ctx, true)

    // Sélectionne explicitement Monday (liste vide par défaut)
    vm.toggleKickoffDay(Calendar.MONDAY)
    // Définit 07:30 pour Monday
    vm.updateKickoffTime(Calendar.MONDAY, 7, 30)

    // Laisse le temps à la recomposition de refléter "Mon 07:30"
    compose.waitForIdle()
    // L'UI affiche "Mon 07:30" (jour + heure)
    compose.onAllNodesWithText("Mon 07:30")[0].assertExists()

    // Applique le planning
    vm.applyKickoffSchedule(ctx)

    // Vérifie l'appel repo
    assertTrue(repo.weekly.isNotEmpty())
    val (_, enabled, times) = repo.weekly.last()
    assertTrue(enabled)
    assertEquals(1, times.size) // Monday seul
    assertEquals(7 to 30, times[Calendar.MONDAY])
  }

  @Test
  fun test_button_triggers_one_shot() {
    val repo = CapturingRepo()
    val vm = NotificationsViewModel(repo)
    compose.setContent { NotificationsScreen(vm = vm, onBack = {}, onGoHome = {}) }
    compose.onNodeWithTag("btn_test_1_min").assertExists().performClick()
    assertEquals(1, repo.oneShot)
  }
}
