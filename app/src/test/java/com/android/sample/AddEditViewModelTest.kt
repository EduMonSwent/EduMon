package com.android.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepository
import com.android.sample.todo.ToDoRepositoryLocal
import com.android.sample.todo.ui.AddToDoViewModel
import com.android.sample.todo.ui.EditToDoViewModel
import java.time.LocalDate
import junit.framework.TestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddEditViewModelTest {

  private lateinit var repo: ToDoRepository

  @Before
  fun setUp() {
    repo = ToDoRepositoryLocal()
  }

  // ---------------- AddToDoViewModel ----------------

  @Test
  fun add_canSave_depends_on_title() {
    val vm = AddToDoViewModel(repo)
    TestCase.assertFalse(vm.canSave)
    vm.title = "  Study  "
    TestCase.assertTrue(vm.canSave)
  }

  @Test
  fun add_save_noop_when_title_blank() = runBlocking {
    val vm = AddToDoViewModel(repo)
    vm.title = "   "
    var called = false
    vm.save { called = true }
    val list = (repo.todos as MutableStateFlow<List<ToDo>>).value
    TestCase.assertEquals(0, list.size)
    TestCase.assertFalse(called)
  }

  // ---------------- EditToDoViewModel ----------------

  @Test
  fun edit_init_prefills_from_repository() = runBlocking {
    val existing =
        ToDo(
            id = "E1",
            title = "Original",
            dueDate = LocalDate.of(2025, 2, 3),
            priority = Priority.LOW,
            status = Status.TODO,
            location = "Dorm",
            links = listOf("x.com", "y.com"),
            note = "n",
            notificationsEnabled = true)
    repo.add(existing)

    val vm = EditToDoViewModel(repo, id = "E1")
    waitUntil(timeoutMs = 1_000) { vm.title == "Original" }

    TestCase.assertEquals("Original", vm.title)
    TestCase.assertEquals(LocalDate.of(2025, 2, 3), vm.dueDate)
    TestCase.assertEquals(Priority.LOW, vm.priority)
    TestCase.assertEquals(Status.TODO, vm.status)
    TestCase.assertEquals("Dorm", vm.location)
    TestCase.assertEquals("x.com, y.com", vm.linksText)
    TestCase.assertEquals("n", vm.note)
    TestCase.assertTrue(vm.notificationsEnabled)
  }

  @Test
  fun edit_save_noop_if_item_missing() = runBlocking {
    val vm = EditToDoViewModel(repo, id = "missing")
    vm.title = "X"
    vm.save { /* not expected to run */}
    val list = (repo.todos as MutableStateFlow<List<ToDo>>).value
    TestCase.assertEquals(0, list.size)
  }

  // ---------- tiny polling helper (no dispatcher needed) ----------
  private suspend fun waitUntil(timeoutMs: Long, condition: () -> Boolean) {
    val start = System.currentTimeMillis()
    while (!condition()) {
      if (System.currentTimeMillis() - start > timeoutMs) break
      delay(10)
    }
  }
}
