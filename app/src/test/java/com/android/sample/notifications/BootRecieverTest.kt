package com.android.sample.notifications

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.android.sample.data.notifications.BootReceiver
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.repos_providors.FakeRepositoriesProvider
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BootReceiverTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `bootReceiver schedules next study task alarm 15min before`() = runTest {
    val repo = FakeRepositoriesProvider.calendarRepository

    val futureDate = LocalDate.now().plusDays(1)
    val futureTime = LocalTime.of(10, 0)
    val eventId = "evt1234"

    val task =
        StudyItem(
            id = eventId,
            title = "Study Session",
            date = futureDate,
            time = futureTime,
            durationMinutes = 60,
            isCompleted = false,
            type = TaskType.STUDY)

    // Injection via API repository
    repo.run { saveTask(task) }

    // Injecte un scope de test contrôlable
    val testScope = TestScope(UnconfinedTestDispatcher())
    val receiver = BootReceiver(repo, testScope)
    val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
    receiver.onReceive(context, intent)

    // Laisse tourner la coroutine dans le scope injecté
    testScope.advanceUntilIdle()

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val shadowAm = Shadows.shadowOf(am)

    val scheduled = shadowAm.nextScheduledAlarm
    assertNotNull("Aucune alarme programmée", scheduled)

    val pendingIntent = scheduled!!.operation
    assertNotNull(pendingIntent)

    val savedIntent = Shadows.shadowOf(pendingIntent).savedIntent
    assertNotNull(savedIntent)

    val extraId = savedIntent.getStringExtra("event_id")
    assertEquals(eventId, extraId)

    val zone = ZoneId.systemDefault()
    val targetMillis =
        futureDate.atTime(futureTime).atZone(zone).toInstant().toEpochMilli() -
            java.time.Duration.ofMinutes(15).toMillis()

    val delta = kotlin.math.abs(scheduled.triggerAtTime - targetMillis)
    assertTrue("Delta trop grand entre l'heure prévue et programmée: $delta", delta < 5000L)
  }
}
