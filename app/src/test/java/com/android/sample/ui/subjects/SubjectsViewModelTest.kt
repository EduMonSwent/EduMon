@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.android.sample.ui.subjects

// Parts of this file have been written using an LLM

import com.android.sample.feature.subjects.repository.FakeSubjectsRepository
import com.android.sample.feature.subjects.viewmodel.SubjectsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SubjectsViewModelTest {

  private val dispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initial_selected_subject_is_null() {
    val repo = FakeSubjectsRepository()
    val vm = SubjectsViewModel(repo)

    assertNull(vm.selectedSubjectId.value)
  }

  @Test
  fun initial_subjects_list_is_empty() {
    val repo = FakeSubjectsRepository()
    val vm = SubjectsViewModel(repo)

    assertEquals(
        emptyList<com.android.sample.feature.subjects.model.StudySubject>(), vm.subjects.value)
  }

  @Test
  fun onSubjectSelected_updates_selected_id() {
    val repo = FakeSubjectsRepository()
    val vm = SubjectsViewModel(repo)

    vm.onSubjectSelected("sub123")

    assertEquals("sub123", vm.selectedSubjectId.value)
  }

  @Test
  fun onSubjectSelected_multiple_times_updates_correctly() {
    val repo = FakeSubjectsRepository()
    val vm = SubjectsViewModel(repo)

    vm.onSubjectSelected("sub1")
    assertEquals("sub1", vm.selectedSubjectId.value)

    vm.onSubjectSelected("sub2")
    assertEquals("sub2", vm.selectedSubjectId.value)

    vm.onSubjectSelected("sub3")
    assertEquals("sub3", vm.selectedSubjectId.value)
  }

  @Test
  fun onCreateSubject_creates_subject_in_repo() = runTest {
    val repo = FakeSubjectsRepository()
    val vm = SubjectsViewModel(repo)

    vm.onCreateSubject("Math", 2)

    val subjects = vm.subjects.value
    assertEquals(1, subjects.size)
    assertEquals("Math", subjects[0].name)
    assertEquals(2, subjects[0].colorIndex)
  }

  @Test
  fun onCreateSubject_multiple_subjects() = runTest {
    val repo = FakeSubjectsRepository()
    val vm = SubjectsViewModel(repo)

    vm.onCreateSubject("Math", 1)
    vm.onCreateSubject("Physics", 2)
    vm.onCreateSubject("Chemistry", 3)

    val subjects = vm.subjects.value
    assertEquals(3, subjects.size)
  }

  @Test
  fun onRenameSubject_updates_subject_name() = runTest {
    val repo = FakeSubjectsRepository()
    val vm = SubjectsViewModel(repo)

    vm.onCreateSubject("Math", 1)
    val id = vm.subjects.value[0].id

    vm.onRenameSubject(id, "Advanced Math")

    val subjects = vm.subjects.value
    assertEquals("Advanced Math", subjects[0].name)
  }

  @Test
  fun onDeleteSubject_removes_subject() = runTest {
    val repo = FakeSubjectsRepository()
    val vm = SubjectsViewModel(repo)

    vm.onCreateSubject("Math", 1)
    vm.onCreateSubject("Physics", 2)
    val idToDelete = vm.subjects.value[0].id

    vm.onDeleteSubject(idToDelete)

    val subjects = vm.subjects.value
    assertEquals(1, subjects.size)
    assertEquals("Physics", subjects[0].name)
  }

  @Test
  fun onDeleteSubject_clears_selection_if_deleted_subject_was_selected() = runTest {
    val repo = FakeSubjectsRepository()
    val vm = SubjectsViewModel(repo)

    vm.onCreateSubject("Math", 1)
    val id = vm.subjects.value[0].id
    vm.onSubjectSelected(id)

    assertEquals(id, vm.selectedSubjectId.value)

    vm.onDeleteSubject(id)

    assertNull(vm.selectedSubjectId.value)
  }

  @Test
  fun onDeleteSubject_keeps_selection_if_different_subject_deleted() = runTest {
    val repo = FakeSubjectsRepository()
    val vm = SubjectsViewModel(repo)

    vm.onCreateSubject("Math", 1)
    vm.onCreateSubject("Physics", 2)

    val id1 = vm.subjects.value[0].id
    val id2 = vm.subjects.value[1].id

    vm.onSubjectSelected(id1)
    vm.onDeleteSubject(id2)

    assertEquals(id1, vm.selectedSubjectId.value)
  }

  @Test
  fun complete_workflow() = runTest {
    val repo = FakeSubjectsRepository()
    val vm = SubjectsViewModel(repo)

    // Create subjects
    vm.onCreateSubject("Math", 1)
    vm.onCreateSubject("Physics", 2)
    vm.onCreateSubject("Chemistry", 3)

    assertEquals(3, vm.subjects.value.size)

    // Select a subject
    val mathId = vm.subjects.value[0].id
    vm.onSubjectSelected(mathId)
    assertEquals(mathId, vm.selectedSubjectId.value)

    // Rename a subject
    vm.onRenameSubject(mathId, "Calculus")
    assertEquals("Calculus", vm.subjects.value[0].name)

    // Delete a different subject
    val chemId = vm.subjects.value[2].id
    vm.onDeleteSubject(chemId)
    assertEquals(2, vm.subjects.value.size)
    assertEquals(mathId, vm.selectedSubjectId.value) // Selection preserved

    // Delete the selected subject
    vm.onDeleteSubject(mathId)
    assertEquals(1, vm.subjects.value.size)
    assertNull(vm.selectedSubjectId.value) // Selection cleared
  }
}
