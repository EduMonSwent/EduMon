package com.android.sample.todo.ui

/**
 * import com.android.sample.todo.* import kotlinx.coroutines.ExperimentalCoroutinesApi import
 * java.time.LocalDate import kotlinx.coroutines.test.advanceUntilIdle import
 * kotlinx.coroutines.test.runTest import org.junit.Assert.* import org.junit.Test
 *
 * class OverviewViewModelTest {
 *
 * private val repo = ToDoRepositoryLocal()
 *
 * @OptIn(ExperimentalCoroutinesApi::class)
 * @Test fun todos_sortedByStatusAndDate() = runTest { val done = ToDo( title = "Done", dueDate =
 *   LocalDate.now().plusDays(2), priority = Priority.LOW, status = Status.DONE) val todo = ToDo(
 *   title = "Todo", dueDate = LocalDate.now(), priority = Priority.HIGH, status = Status.TODO)
 *   repo.add(done) repo.add(todo)
 *
 * val vm = OverviewViewModel(repo) advanceUntilIdle()
 *
 * val list = vm.uiState.value.items assertEquals("Todo", list.first().title) }
 *
 * @OptIn(ExperimentalCoroutinesApi::class)
 * @Test fun cycleStatus_changesStatusCorrectly() = runTest { val t = ToDo( title = "A", dueDate =
 *   LocalDate.now(), priority = Priority.MEDIUM, status = Status.TODO) repo.add(t)
 *
 * val vm = OverviewViewModel(repo) vm.cycleStatus(t.id) advanceUntilIdle()
 *
 * assertEquals(Status.IN_PROGRESS, repo.getById(t.id)?.status) }
 *
 * @OptIn(ExperimentalCoroutinesApi::class)
 * @Test fun delete_removesItem() = runTest { val t = ToDo( title = "A", dueDate = LocalDate.now(),
 *   priority = Priority.MEDIUM, status = Status.TODO) repo.add(t)
 *
 * val vm = OverviewViewModel(repo) vm.delete(t.id) advanceUntilIdle()
 *
 * assertNull(repo.getById(t.id)) } }
 */
