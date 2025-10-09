package com.android.sample.todo.ui

import com.android.sample.todo.*
import org.junit.Assert.*
import org.junit.Test

class AddEditViewModelTest {

  private val repo = ToDoRepositoryLocal()

  /**
   * @OptIn(ExperimentalCoroutinesApi::class)
   * @Test fun save_addsTodo() = runTest { val vm = AddToDoViewModel(repo) vm.title = "New Task"
   *   vm.dueDate = LocalDate.now() vm.priority = Priority.MEDIUM vm.status = Status.TODO
   *
   * vm.save {} advanceUntilIdle()
   *
   * val todos = repo.todos.first() assertTrue(todos.any { it.title == "New Task" }) }
   */
  @Test
  fun cannotSaveWhenTitleBlank() {
    val vm = AddToDoViewModel(repo)
    vm.title = ""
    assertFalse(vm.canSave)
  }

  /**
   * @OptIn(ExperimentalCoroutinesApi::class)
   * @Test fun edit_updatesExistingTodo() = runTest { val seed = ToDo( title = "Initial", dueDate =
   *   LocalDate.now(), priority = Priority.MEDIUM, status = Status.TODO) repo.add(seed)
   *
   * val vm = EditToDoViewModel(repo, seed.id) advanceUntilIdle()
   *
   * vm.title = "Updated" vm.save {} advanceUntilIdle()
   *
   * assertEquals("Updated", repo.getById(seed.id)?.title) }
   */
}
