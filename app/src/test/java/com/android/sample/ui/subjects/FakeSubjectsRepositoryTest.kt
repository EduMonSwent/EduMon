package com.android.sample.ui.subjects

// The help of an LLM has been used to write this test file.
import com.android.sample.feature.subjects.model.StudySubject
import com.android.sample.feature.subjects.repository.FakeSubjectsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeSubjectsRepositoryTest {

  @Test
  fun initial_subjects_list_is_empty() {
    val repo = FakeSubjectsRepository()

    assertEquals(emptyList<StudySubject>(), repo.subjects.value)
  }

  @Test
  fun start_is_noop() = runTest {
    val repo = FakeSubjectsRepository()

    repo.start()

    assertEquals(emptyList<StudySubject>(), repo.subjects.value)
  }

  @Test
  fun createSubject_adds_subject_to_list() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 2)

    val subjects = repo.subjects.value
    assertEquals(1, subjects.size)
    assertEquals("Math", subjects[0].name)
    assertEquals(2, subjects[0].colorIndex)
    assertEquals(0, subjects[0].totalStudyMinutes)
    assertEquals("1", subjects[0].id)
  }

  @Test
  fun createSubject_multiple_subjects_increments_id() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 1)
    repo.createSubject("Physics", 2)
    repo.createSubject("Chemistry", 3)

    val subjects = repo.subjects.value
    assertEquals(3, subjects.size)
    assertEquals("1", subjects[0].id)
    assertEquals("2", subjects[1].id)
    assertEquals("3", subjects[2].id)
  }

  @Test
  fun renameSubject_updates_subject_name() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 1)
    repo.renameSubject("1", "Advanced Math")

    val subjects = repo.subjects.value
    assertEquals(1, subjects.size)
    assertEquals("Advanced Math", subjects[0].name)
  }

  @Test
  fun renameSubject_non_existent_id_does_nothing() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 1)
    repo.renameSubject("999", "New Name")

    val subjects = repo.subjects.value
    assertEquals(1, subjects.size)
    assertEquals("Math", subjects[0].name)
  }

  @Test
  fun deleteSubject_removes_subject() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 1)
    repo.createSubject("Physics", 2)
    repo.deleteSubject("1")

    val subjects = repo.subjects.value
    assertEquals(1, subjects.size)
    assertEquals("Physics", subjects[0].name)
  }

  @Test
  fun deleteSubject_non_existent_id_does_nothing() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 1)
    repo.deleteSubject("999")

    val subjects = repo.subjects.value
    assertEquals(1, subjects.size)
  }

  @Test
  fun addStudyMinutesToSubject_increments_total() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 1)
    repo.addStudyMinutesToSubject("1", 30)

    val subjects = repo.subjects.value
    assertEquals(30, subjects[0].totalStudyMinutes)
  }

  @Test
  fun addStudyMinutesToSubject_multiple_times_accumulates() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 1)
    repo.addStudyMinutesToSubject("1", 30)
    repo.addStudyMinutesToSubject("1", 20)
    repo.addStudyMinutesToSubject("1", 15)

    val subjects = repo.subjects.value
    assertEquals(65, subjects[0].totalStudyMinutes)
  }

  @Test
  fun addStudyMinutesToSubject_zero_minutes_does_nothing() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 1)
    repo.addStudyMinutesToSubject("1", 0)

    val subjects = repo.subjects.value
    assertEquals(0, subjects[0].totalStudyMinutes)
  }

  @Test
  fun addStudyMinutesToSubject_negative_minutes_does_nothing() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 1)
    repo.addStudyMinutesToSubject("1", 50)
    repo.addStudyMinutesToSubject("1", -10)

    val subjects = repo.subjects.value
    assertEquals(50, subjects[0].totalStudyMinutes)
  }

  @Test
  fun addStudyMinutesToSubject_non_existent_id_does_nothing() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 1)
    repo.addStudyMinutesToSubject("999", 30)

    val subjects = repo.subjects.value
    assertEquals(0, subjects[0].totalStudyMinutes)
  }

  @Test
  fun multiple_operations_maintain_correct_state() = runTest {
    val repo = FakeSubjectsRepository()

    repo.createSubject("Math", 1)
    repo.createSubject("Physics", 2)
    repo.createSubject("Chemistry", 3)

    repo.addStudyMinutesToSubject("1", 60)
    repo.addStudyMinutesToSubject("2", 45)

    repo.renameSubject("1", "Advanced Math")
    repo.deleteSubject("3")

    val subjects = repo.subjects.value
    assertEquals(2, subjects.size)
    assertEquals("Advanced Math", subjects[0].name)
    assertEquals(60, subjects[0].totalStudyMinutes)
    assertEquals("Physics", subjects[1].name)
    assertEquals(45, subjects[1].totalStudyMinutes)
  }
}
