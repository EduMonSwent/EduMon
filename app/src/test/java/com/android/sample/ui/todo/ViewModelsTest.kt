package com.android.sample.ui.todo

import com.android.sample.ui.todo.data.ToDoRepositoryLocal
import com.android.sample.ui.todo.model.Priority
import com.android.sample.ui.todo.model.Status
import com.android.sample.ui.todo.model.ToDo
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.*

/**
 * Single, simple test suite that covers:
 * - AddToDoViewModel: canSave, save (with trimming & optional fields)
 * - EditToDoViewModel: init loads, save updates (with trimming)
 * - OverviewViewModel: sorting, cycleStatus, delete
 *
 * Uses ToDoRepositoryLocal as an in-memory store.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsSingleTest {

  // Simple Main dispatcher rule inlined so we only need ONE file
  private val testDispatcher: TestDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ---------- AddToDoViewModel ----------

  @Test
  fun add_canSave_and_save_builds_trimmed_and_calls_onDone() = runTest {
    val repo = ToDoRepositoryLocal()
    val vm = AddToDoViewModel(repo)

    // Blank title -> cannot save
    vm.title = "  "
    assertFalse(vm.canSave)

    vm.title = "Hello"
    vm.dueDate = LocalDate.of(2025, 12, 31)
    vm.priority = Priority.HIGH
    vm.status = Status.IN_PROGRESS
    vm.location = "Office"
    vm.linksText = "https://a , , https://b ,"
    vm.note = "note"
    vm.notificationsEnabled = true

    var done = false
    vm.save { done = true }
    advanceUntilIdle()

    val saved = repo.todos.first { it.isNotEmpty() }.single()
    assertTrue(done)
    assertEquals("Hello", saved.title)
    assertEquals(LocalDate.of(2025, 12, 31), saved.dueDate)
    assertEquals(Priority.HIGH, saved.priority)
    assertEquals(Status.IN_PROGRESS, saved.status)
    assertEquals("Office", saved.location)
    assertEquals(listOf("https://a", "https://b"), saved.links)
    assertEquals("note", saved.note)
    assertTrue(saved.notificationsEnabled)
  }

  @Test
  fun add_blank_optional_fields_are_omitted() = runTest {
    val repo = ToDoRepositoryLocal()
    val vm = AddToDoViewModel(repo)

    vm.title = "X"
    vm.location = "   " // -> null
    vm.note = "" // -> null
    vm.linksText = " , " // -> empty list
    vm.save {}
    advanceUntilIdle()

    val saved = repo.todos.first { it.isNotEmpty() }.single()
    assertNull(saved.location)
    assertNull(saved.note)
    assertTrue(saved.links.isEmpty())
  }

  // ---------- EditToDoViewModel ----------

  @Test
  fun edit_init_loads_existing_and_save_updates_with_trimming() = runTest {
    val repo = ToDoRepositoryLocal()
    val id = "todo-1"
    val original =
        ToDo(
            id = id,
            title = "Old",
            dueDate = LocalDate.of(2025, 1, 1),
            priority = Priority.LOW,
            status = Status.TODO,
            location = "Place",
            links = listOf("x"),
            note = "N",
            notificationsEnabled = false)
    repo.add(original)

    val vm = EditToDoViewModel(repo, id)
    advanceUntilIdle()

    // Loaded into fields
    assertEquals("Old", vm.title)
    assertEquals("x", vm.linksText.trim())

    // Update (include trimming & blank optionals to cover branches)
    vm.title = "  New  "
    vm.linksText = " a ,  , b "
    vm.location = "   " // -> null
    vm.note = "" // -> null
    vm.status = Status.DONE
    vm.priority = Priority.HIGH

    var done = false
    vm.save { done = true }
    advanceUntilIdle()

    val updated = repo.getById(id)!!
    assertEquals("New", updated.title)
    assertEquals(listOf("a", "b"), updated.links)
    assertNull(updated.location)
    assertNull(updated.note)
    assertEquals(Status.DONE, updated.status)
    assertEquals(Priority.HIGH, updated.priority)
    assertTrue(done)
  }

  @Test
  fun edit_canSave_is_false_when_title_blank() {
    val vm = EditToDoViewModel(ToDoRepositoryLocal(), "missing-id")
    vm.title = " "
    assertFalse(vm.canSave)
  }

  // ---------- OverviewViewModel ----------

  @Test
  fun overview_sorts_nonDone_first_then_by_dueDate_and_supports_cycle_and_delete() = runTest {
    val repo = ToDoRepositoryLocal()

    // Three items: ensure sorting (non-DONE first, then earliest date)
    val a =
        ToDo(
            id = "a",
            title = "a",
            dueDate = LocalDate.of(2025, 1, 10),
            priority = Priority.MEDIUM,
            status = Status.DONE)
    val b =
        ToDo(
            id = "b",
            title = "b",
            dueDate = LocalDate.of(2025, 1, 1),
            priority = Priority.MEDIUM,
            status = Status.TODO)
    val c =
        ToDo(
            id = "c",
            title = "c",
            dueDate = LocalDate.of(2025, 1, 2),
            priority = Priority.MEDIUM,
            status = Status.IN_PROGRESS)
    repo.add(a)
    repo.add(b)
    repo.add(c)

    val vm = OverviewViewModel(repo)
    advanceUntilIdle()

    val sortedIds = vm.uiState.first { it.items.isNotEmpty() }.items.map { it.id }
    assertEquals(listOf("b", "c", "a"), sortedIds)

    // Cycle status b: TODO -> IN_PROGRESS -> DONE -> TODO
    vm.cycleStatus("b")
    advanceUntilIdle()
    assertEquals(Status.IN_PROGRESS, repo.getById("b")!!.status)
    vm.cycleStatus("b")
    advanceUntilIdle()
    assertEquals(Status.DONE, repo.getById("b")!!.status)
    vm.cycleStatus("b")
    advanceUntilIdle()
    assertEquals(Status.TODO, repo.getById("b")!!.status)

    // Delete c
    vm.delete("c")
    advanceUntilIdle()
    assertNull(repo.getById("c"))
  }
}
