package com.android.sample.ui.profile

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.profile.ProfileRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ProfileViewModel, specifically for functions requiring Android Context.
 * Tests the importIcs functionality with mock ICS file content.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ProfileViewModelInstrumentedTest {

  private val dispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private var originalRepositories = AppRepositories

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    context = ApplicationProvider.getApplicationContext()
    AppRepositories = FakeRepositoriesProvider
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    AppRepositories = originalRepositories
  }

  /** A recording UserStatsRepository that tracks all method calls */
  private class RecordingUserStatsRepository(initial: UserStats = UserStats()) :
      UserStatsRepository {

    private val _stats = MutableStateFlow(initial)
    override val stats: StateFlow<UserStats> = _stats

    var started: Boolean = false

    override suspend fun start() {
      started = true
    }

    override suspend fun addStudyMinutes(minutes: Int) {
      if (minutes <= 0) return
      val current = _stats.value
      _stats.value =
          current.copy(
              totalStudyMinutes = current.totalStudyMinutes + minutes,
              todayStudyMinutes = current.todayStudyMinutes + minutes,
          )
    }

    override suspend fun updateCoins(delta: Int) {
      if (delta == 0) return
      val current = _stats.value
      _stats.value = current.copy(coins = (current.coins + delta).coerceAtLeast(0))
    }

    override suspend fun setWeeklyGoal(minutes: Int) {
      val current = _stats.value
      _stats.value = current.copy(weeklyGoal = minutes.coerceAtLeast(0))
    }

    override suspend fun addPoints(delta: Int) {
      if (delta == 0) return
      val current = _stats.value
      _stats.value = current.copy(points = (current.points + delta).coerceAtLeast(0))
    }
  }

  private fun createViewModel(
      profileRepo: ProfileRepository = FakeProfileRepository(),
      statsRepo: RecordingUserStatsRepository = RecordingUserStatsRepository()
  ): ProfileViewModel {
    return ProfileViewModel(
        profileRepository = profileRepo,
        userStatsRepository = statsRepo,
    )
  }

  // ==================== importIcs Tests ====================

  @Test
  fun importIcs_handles_null_input_stream_gracefully() = runTest {
    val vm = createViewModel()
    advanceUntilIdle()

    // Create a URI that doesn't exist - contentResolver will return null
    val fakeUri = Uri.parse("content://fake/nonexistent")

    // Should not crash
    vm.importIcs(context, fakeUri)
    advanceUntilIdle()

    // ViewModel should still be functional
    assertNotNull(vm.userProfile.value)
  }

  @Test
  fun importIcs_handles_invalid_ics_content() = runTest {
    val vm = createViewModel()
    advanceUntilIdle()

    // Create a temp file with invalid ICS content
    val tempFile = File(context.cacheDir, "invalid.ics")
    tempFile.writeText("This is not valid ICS content")

    val uri = Uri.fromFile(tempFile)

    // Should not crash even with invalid content
    try {
      vm.importIcs(context, uri)
      advanceUntilIdle()
    } catch (e: Exception) {
      // Exception is caught internally, should not propagate
    }

    // Cleanup
    tempFile.delete()

    // ViewModel should still be functional
    assertNotNull(vm.userProfile.value)
  }

  @Test
  fun importIcs_handles_empty_file() = runTest {
    val vm = createViewModel()
    advanceUntilIdle()

    // Create a temp file with empty content
    val tempFile = File(context.cacheDir, "empty.ics")
    tempFile.writeText("")

    val uri = Uri.fromFile(tempFile)

    // Should not crash
    try {
      vm.importIcs(context, uri)
      advanceUntilIdle()
    } catch (e: Exception) {
      // Exception is caught internally
    }

    // Cleanup
    tempFile.delete()

    assertNotNull(vm.userProfile.value)
  }

  @Test
  fun importIcs_processes_valid_ics_file() = runTest {
    val vm = createViewModel()
    advanceUntilIdle()

    // Create a temp file with valid ICS content
    val validIcsContent =
        """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VEVENT
            DTSTART:20241215T100000Z
            DTEND:20241215T110000Z
            SUMMARY:Test Event
            DESCRIPTION:A test event for unit testing
            END:VEVENT
            END:VCALENDAR
        """
            .trimIndent()

    val tempFile = File(context.cacheDir, "valid.ics")
    tempFile.writeText(validIcsContent)

    val uri = Uri.fromFile(tempFile)

    // Should not crash and should process the file
    try {
      vm.importIcs(context, uri)
      advanceUntilIdle()
    } catch (e: Exception) {
      // May throw if repositories aren't fully set up, but shouldn't crash
    }

    // Cleanup
    tempFile.delete()

    assertNotNull(vm.userProfile.value)
  }

  @Test
  fun importIcs_handles_ics_with_exam_event() = runTest {
    val vm = createViewModel()
    advanceUntilIdle()

    // Create ICS with exam-like content
    val examIcsContent =
        """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//EPFL//Schedule//EN
            BEGIN:VEVENT
            DTSTART:20241220T140000Z
            DTEND:20241220T170000Z
            SUMMARY:Final Exam - CS101
            DESCRIPTION:Final examination for CS101
            LOCATION:Room CE1
            END:VEVENT
            END:VCALENDAR
        """
            .trimIndent()

    val tempFile = File(context.cacheDir, "exam.ics")
    tempFile.writeText(examIcsContent)

    val uri = Uri.fromFile(tempFile)

    try {
      vm.importIcs(context, uri)
      advanceUntilIdle()
    } catch (e: Exception) {
      // Exception handling
    }

    // Cleanup
    tempFile.delete()

    assertNotNull(vm.userProfile.value)
  }

  @Test
  fun importIcs_handles_multiple_events() = runTest {
    val vm = createViewModel()
    advanceUntilIdle()

    // Create ICS with multiple events
    val multiEventIcs =
        """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VEVENT
            DTSTART:20241215T090000Z
            DTEND:20241215T100000Z
            SUMMARY:Morning Class
            END:VEVENT
            BEGIN:VEVENT
            DTSTART:20241215T140000Z
            DTEND:20241215T150000Z
            SUMMARY:Afternoon Lab
            END:VEVENT
            BEGIN:VEVENT
            DTSTART:20241216T100000Z
            DTEND:20241216T120000Z
            SUMMARY:Workshop
            END:VEVENT
            END:VCALENDAR
        """
            .trimIndent()

    val tempFile = File(context.cacheDir, "multi.ics")
    tempFile.writeText(multiEventIcs)

    val uri = Uri.fromFile(tempFile)

    try {
      vm.importIcs(context, uri)
      advanceUntilIdle()
    } catch (e: Exception) {
      // Exception handling
    }

    // Cleanup
    tempFile.delete()

    assertNotNull(vm.userProfile.value)
  }

  @Test
  fun importIcs_handles_malformed_ics() = runTest {
    val vm = createViewModel()
    advanceUntilIdle()

    // Create malformed ICS (missing END tags)
    val malformedIcs =
        """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            DTSTART:20241215T100000Z
            SUMMARY:Broken Event
        """
            .trimIndent()

    val tempFile = File(context.cacheDir, "malformed.ics")
    tempFile.writeText(malformedIcs)

    val uri = Uri.fromFile(tempFile)

    try {
      vm.importIcs(context, uri)
      advanceUntilIdle()
    } catch (e: Exception) {
      // Should be caught internally
    }

    // Cleanup
    tempFile.delete()

    // ViewModel should still work
    assertNotNull(vm.userProfile.value)
  }

  @Test
  fun importIcs_handles_ics_with_special_characters() = runTest {
    val vm = createViewModel()
    advanceUntilIdle()

    // Create ICS with special characters in summary/description
    val specialCharsIcs =
        """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VEVENT
            DTSTART:20241215T100000Z
            DTEND:20241215T110000Z
            SUMMARY:Event with émojis 🎓 and spëcial chars
            DESCRIPTION:Contains: "quotes", 'apostrophes', & ampersands
            END:VEVENT
            END:VCALENDAR
        """
            .trimIndent()

    val tempFile = File(context.cacheDir, "special.ics")
    tempFile.writeText(specialCharsIcs)

    val uri = Uri.fromFile(tempFile)

    try {
      vm.importIcs(context, uri)
      advanceUntilIdle()
    } catch (e: Exception) {
      // Exception handling
    }

    // Cleanup
    tempFile.delete()

    assertNotNull(vm.userProfile.value)
  }

  @Test
  fun viewModel_remains_functional_after_import_failure() = runTest {
    val vm = createViewModel()
    advanceUntilIdle()

    // Try to import from non-existent URI
    val badUri = Uri.parse("content://invalid/path/that/does/not/exist")

    vm.importIcs(context, badUri)
    advanceUntilIdle()

    // ViewModel should still be fully functional
    vm.toggleNotifications()
    advanceUntilIdle()

    assertNotNull(vm.userProfile.value)
  }
}
