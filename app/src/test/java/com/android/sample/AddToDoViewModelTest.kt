package com.android.sample

import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ui.AddToDoViewModel
import java.time.LocalDate
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddToDoViewModelTest {

  @get:Rule val coroutineRule = MainCoroutineRule()

  private lateinit var fakeRepo: FakeToDoRepository
  private lateinit var viewModel: AddToDoViewModel

  @Before
  fun setUp() {
    fakeRepo = FakeToDoRepository()
    viewModel = AddToDoViewModel(fakeRepo)
  }

  @Test
  fun `canSave is false when title is blank`() {
    viewModel.title = ""
    assertFalse(viewModel.canSave)

    viewModel.title = "   " // whitespace
    assertFalse(viewModel.canSave)
  }

  @Test
  fun `canSave is true when title is not blank`() {
    viewModel.title = "Write tests"
    assertTrue(viewModel.canSave)
  }

  @Test
  fun `save adds correctly formatted ToDo to repository and calls onDone`() = runTest {
    var onDoneCalled = false
    // Configure the ViewModel state
    viewModel.apply {
      title = "  Finish Lab 4  "
      dueDate = LocalDate.of(2025, 10, 14)
      priority = Priority.HIGH
      status = Status.IN_PROGRESS
      location = "INM 202"
      linksText = " https://docs.com, https://moodle.com "
      note = "Pair with Lea"
      notificationsEnabled = true
    }

    // Act
    viewModel.save { onDoneCalled = true }

    // Assert
    val savedItem = fakeRepo.getById(fakeRepo.currentList.first().id)
    assertTrue(onDoneCalled)
    assertEquals(1, fakeRepo.currentList.size)
    assertEquals("Finish Lab 4", savedItem?.title) // Title is trimmed
    assertEquals(LocalDate.of(2025, 10, 14), savedItem?.dueDate)
    assertEquals(Priority.HIGH, savedItem?.priority)
    assertEquals(Status.IN_PROGRESS, savedItem?.status)
    assertEquals("INM 202", savedItem?.location)
    assertEquals(
        listOf("https://docs.com", "https://moodle.com"), savedItem?.links) // Links are trimmed
    assertEquals("Pair with Lea", savedItem?.note)
    assertTrue(savedItem?.notificationsEnabled ?: false)
  }

  @Test
  fun `save does nothing if canSave is false`() = runTest {
    var onDoneCalled = false
    viewModel.title = ""

    viewModel.save { onDoneCalled = true }

    assertFalse(onDoneCalled)
    assertTrue(fakeRepo.currentList.isEmpty())
  }
}
