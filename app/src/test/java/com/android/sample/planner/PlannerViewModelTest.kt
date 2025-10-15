package com.android.sample.planner

import com.android.sample.model.planner.*
import com.android.sample.ui.planner.PlannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Simple test rule for coroutines
class TestCoroutineRule : TestWatcher() {
  @OptIn(ExperimentalCoroutinesApi::class)
  override fun starting(description: Description) {
    Dispatchers.setMain(Dispatchers.Unconfined)
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlannerViewModelTest {

  @get:Rule val testCoroutineRule = TestCoroutineRule()

  private lateinit var viewModel: PlannerViewModel

  @Before
  fun setup() {
    // Use the real repository with default constructor
    viewModel = PlannerViewModel()
  }

  @Test
  fun `initial state loads today classes`() = runTest {
    // When - ViewModel is initialized

    // Then - Should have today's classes loaded
    val classes = viewModel.todayClasses.first()
    assertTrue(classes.isNotEmpty())
    assertEquals("Algorithms", classes[0].courseName)
  }

  @Test
  fun `show add study task modal when FAB clicked`() = runTest {
    // When
    viewModel.onAddStudyTaskClicked()

    // Then
    assertTrue(viewModel.showAddStudyTaskModal.first())
  }

  @Test
  fun `hide add study task modal when dismissed`() = runTest {
    // Given
    viewModel.onAddStudyTaskClicked()
    assertTrue(viewModel.showAddStudyTaskModal.first())

    // When
    viewModel.onDismissAddStudyTaskModal()

    // Then
    assertFalse(viewModel.showAddStudyTaskModal.first())
  }

  @Test
  fun `select class and show attendance modal`() = runTest {
    // Given
    val classes = viewModel.todayClasses.first()
    val testClass = classes[0]

    // When
    viewModel.onClassClicked(testClass)

    // Then
    assertEquals(testClass, viewModel.selectedClassForAttendance.first())
    assertTrue(viewModel.showClassAttendanceModal.first())
  }

  @Test
  fun `dismiss attendance modal resets selection`() = runTest {
    // Given
    val classes = viewModel.todayClasses.first()
    val testClass = classes[0]
    viewModel.onClassClicked(testClass)

    // When
    viewModel.onDismissClassAttendanceModal()

    // Then
    assertNull(viewModel.selectedClassForAttendance.first())
    assertFalse(viewModel.showClassAttendanceModal.first())
  }

  @Test
  fun `save attendance for lecture class`() = runTest {
    // Given
    val classes = viewModel.todayClasses.first()
    val lectureClass = classes.find { it.type == ClassType.LECTURE }!!

    // When
    viewModel.saveClassAttendance(lectureClass, AttendanceStatus.YES, CompletionStatus.YES)

    // Then - Should save without errors and close modal
    assertFalse(viewModel.showClassAttendanceModal.first())
    assertNull(viewModel.selectedClassForAttendance.first())
  }

  @Test
  fun `save attendance for exercise class`() = runTest {
    // Given
    val classes = viewModel.todayClasses.first()
    val exerciseClass = classes.find { it.type == ClassType.EXERCISE }!!

    // When
    viewModel.saveClassAttendance(
        exerciseClass, AttendanceStatus.ARRIVED_LATE, CompletionStatus.PARTIALLY)

    // Then - Should save without errors
    assertFalse(viewModel.showClassAttendanceModal.first())
  }

  @Test
  fun `save attendance for lab class`() = runTest {
    // Given
    val classes = viewModel.todayClasses.first()
    val labClass = classes.find { it.type == ClassType.LAB }!!

    // When
    viewModel.saveClassAttendance(labClass, AttendanceStatus.NO, CompletionStatus.NO)

    // Then - Should save without errors
    assertFalse(viewModel.showClassAttendanceModal.first())
  }
}
