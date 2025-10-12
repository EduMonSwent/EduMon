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
class PlannerViewModelCoroutineTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private lateinit var viewModel: PlannerViewModel

    @Before
    fun setup() {
        viewModel = PlannerViewModel()
    }

    @Test
    fun `initial state should have empty attendance records`() = runTest {
        val attendanceRecords = viewModel.todayAttendanceRecords.first()
        assertTrue(attendanceRecords.isEmpty())
    }

    @Test
    fun `showAddStudyTaskModal should be false initially`() = runTest {
        assertFalse(viewModel.showAddStudyTaskModal.first())
    }

    @Test
    fun `showClassAttendanceModal should be false initially`() = runTest {
        assertFalse(viewModel.showClassAttendanceModal.first())
    }

    @Test
    fun `selectedClassForAttendance should be null initially`() = runTest {
        assertNull(viewModel.selectedClassForAttendance.first())
    }

    @Test
    fun `onAddStudyTaskClicked should set showAddStudyTaskModal to true`() = runTest {
        // When
        viewModel.onAddStudyTaskClicked()

        // Then
        assertTrue(viewModel.showAddStudyTaskModal.first())
    }

    @Test
    fun `onDismissAddStudyTaskModal should set showAddStudyTaskModal to false`() = runTest {
        // Given
        viewModel.onAddStudyTaskClicked()

        // When
        viewModel.onDismissAddStudyTaskModal()

        // Then
        assertFalse(viewModel.showAddStudyTaskModal.first())
    }

    @Test
    fun `onClassClicked should set selected class and show modal`() = runTest {
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
    fun `onDismissClassAttendanceModal should reset selection and hide modal`() = runTest {
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
    fun `saveClassAttendance should update attendance records`() = runTest {
        // Given
        val classes = viewModel.todayClasses.first()
        val testClass = classes[0]

        // When
        viewModel.saveClassAttendance(testClass, AttendanceStatus.YES, CompletionStatus.YES)

        // Then
        val attendanceRecords = viewModel.todayAttendanceRecords.first()
        assertEquals(1, attendanceRecords.size)
        assertEquals(testClass.id, attendanceRecords[0].classId)
        assertEquals(AttendanceStatus.YES, attendanceRecords[0].attendance)
        assertEquals(CompletionStatus.YES, attendanceRecords[0].completion)
    }

    @Test
    fun `saveClassAttendance with different status combinations`() = runTest {
        // Given
        val classes = viewModel.todayClasses.first()
        val testClass = classes[0]

        // Test various combinations
        val testCases = listOf(
            Triple(AttendanceStatus.YES, CompletionStatus.YES, "YES-YES"),
            Triple(AttendanceStatus.NO, CompletionStatus.NO, "NO-NO"),
            Triple(AttendanceStatus.ARRIVED_LATE, CompletionStatus.PARTIALLY, "LATE-PARTIAL"),
            Triple(AttendanceStatus.YES, CompletionStatus.PARTIALLY, "YES-PARTIAL"),
            Triple(AttendanceStatus.ARRIVED_LATE, CompletionStatus.YES, "LATE-YES")
        )

        testCases.forEach { (attendance, completion, description) ->
            // When
            viewModel.saveClassAttendance(testClass, attendance, completion)

            // Then
            val attendanceRecords = viewModel.todayAttendanceRecords.first()
            val record = attendanceRecords.find { it.classId == testClass.id }
            assertNotNull("Record should exist for $description", record)
            assertEquals("Attendance should match for $description", attendance, record?.attendance)
            assertEquals("Completion should match for $description", completion, record?.completion)
        }
    }

    @Test
    fun `saveClassAttendance for multiple classes`() = runTest {
        // Given
        val classes = viewModel.todayClasses.first()

        // When - Save attendance for all classes
        classes.forEach { classItem ->
            viewModel.saveClassAttendance(classItem, AttendanceStatus.YES, CompletionStatus.YES)
        }

        // Then
        val attendanceRecords = viewModel.todayAttendanceRecords.first()
        assertEquals(classes.size, attendanceRecords.size)

        classes.forEach { classItem ->
            val record = attendanceRecords.find { it.classId == classItem.id }
            assertNotNull("Record should exist for ${classItem.courseName}", record)
            assertEquals(AttendanceStatus.YES, record?.attendance)
            assertEquals(CompletionStatus.YES, record?.completion)
        }
    }

    @Test
    fun `saveClassAttendance should update existing record`() = runTest {
        // Given
        val classes = viewModel.todayClasses.first()
        val testClass = classes[0]

        // Save initial attendance
        viewModel.saveClassAttendance(testClass, AttendanceStatus.NO, CompletionStatus.NO)

        // When - Update the attendance
        viewModel.saveClassAttendance(testClass, AttendanceStatus.YES, CompletionStatus.YES)

        // Then
        val attendanceRecords = viewModel.todayAttendanceRecords.first()
        assertEquals(1, attendanceRecords.size) // Should still be only one record
        assertEquals(AttendanceStatus.YES, attendanceRecords[0].attendance)
        assertEquals(CompletionStatus.YES, attendanceRecords[0].completion)
    }
}