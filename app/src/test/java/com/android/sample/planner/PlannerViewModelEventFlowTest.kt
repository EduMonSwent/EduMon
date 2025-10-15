package com.android.sample.planner

import com.android.sample.model.planner.*
import com.android.sample.ui.planner.PlannerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlannerViewModelEventFlowTest {

  @get:Rule val testCoroutineRule = TestCoroutineRule()

  private lateinit var viewModel: PlannerViewModel

  @Before
  fun setup() {
    viewModel = PlannerViewModel()
  }

  /*@Test
  fun `eventFlow should emit ShowSnackbar on successful attendance save`() = runTest {
      // Given
      val classes = viewModel.todayClasses.first()
      val testClass = classes[0]

      // When
      viewModel.saveClassAttendance(testClass, AttendanceStatus.YES, CompletionStatus.YES)

      // Then - Wait for the first emission with timeout
      val event = viewModel.eventFlow.first()
      assertTrue(event is PlannerViewModel.UiEvent.ShowSnackbar)
      assertEquals("Attendance saved successfully!", (event as PlannerViewModel.UiEvent.ShowSnackbar).message)
  }*/

  /*@Test
  fun `multiple attendance saves should emit multiple events`() = runTest {
      // Given
      val classes = viewModel.todayClasses.first()

      // When - Save attendance for multiple classes
      val events = mutableListOf<PlannerViewModel.UiEvent>()
      val job = kotlinx.coroutines.launch {
          viewModel.eventFlow.collect { event ->
              events.add(event)
          }
      }

      classes.forEach { classItem ->
          viewModel.saveClassAttendance(classItem, AttendanceStatus.YES, CompletionStatus.YES)
      }

      // Give some time for events to be collected
      kotlinx.coroutines.delay(100)

      // Then
      assertTrue(events.size >= classes.size)
      job.cancel()
  }*/

  @Test
  fun `viewModel initialization should load data`() = runTest {
    // When - ViewModel is initialized in setup

    // Then
    val classes = viewModel.todayClasses.first()
    assertTrue(classes.isNotEmpty())

    val attendanceRecords = viewModel.todayAttendanceRecords.first()
    assertTrue(attendanceRecords.isEmpty()) // Initially empty
  }

  @Test
  fun `modal state management should work correctly`() = runTest {
    // Test initial state
    assertFalse(viewModel.showAddStudyTaskModal.first())
    assertFalse(viewModel.showClassAttendanceModal.first())
    assertNull(viewModel.selectedClassForAttendance.first())

    // Test add study task modal flow
    viewModel.onAddStudyTaskClicked()
    assertTrue(viewModel.showAddStudyTaskModal.first())

    viewModel.onDismissAddStudyTaskModal()
    assertFalse(viewModel.showAddStudyTaskModal.first())

    // Test class attendance modal flow
    val classes = viewModel.todayClasses.first()
    val testClass = classes[0]

    viewModel.onClassClicked(testClass)
    assertTrue(viewModel.showClassAttendanceModal.first())
    assertEquals(testClass, viewModel.selectedClassForAttendance.first())

    viewModel.onDismissClassAttendanceModal()
    assertFalse(viewModel.showClassAttendanceModal.first())
    assertNull(viewModel.selectedClassForAttendance.first())
  }

  @Test
  fun `attendance records should be updated after save`() = runTest {
    // Given
    val classes = viewModel.todayClasses.first()
    val testClass = classes[0]

    // When
    viewModel.saveClassAttendance(
        testClass, AttendanceStatus.ARRIVED_LATE, CompletionStatus.PARTIALLY)

    // Then
    val attendanceRecords = viewModel.todayAttendanceRecords.first()
    assertEquals(1, attendanceRecords.size)

    val record = attendanceRecords[0]
    assertEquals(testClass.id, record.classId)
    assertEquals(AttendanceStatus.ARRIVED_LATE, record.attendance)
    assertEquals(CompletionStatus.PARTIALLY, record.completion)
  }

  @Test
  fun `all class types should be handled correctly`() = runTest {
    // Given
    val classes = viewModel.todayClasses.first()

    // When - Save attendance for each class type
    classes.forEach { classItem ->
      viewModel.saveClassAttendance(classItem, AttendanceStatus.YES, CompletionStatus.YES)
    }

    // Then
    val attendanceRecords = viewModel.todayAttendanceRecords.first()
    assertEquals(classes.size, attendanceRecords.size)

    // Verify each class type has a record
    val classTypes = classes.map { it.type }.distinct()
    classTypes.forEach { classType ->
      val classOfType = classes.find { it.type == classType }
      assertNotNull("Class of type $classType should exist", classOfType)

      val record = attendanceRecords.find { it.classId == classOfType?.id }
      assertNotNull("Attendance record for $classType should exist", record)
    }
  }
}
