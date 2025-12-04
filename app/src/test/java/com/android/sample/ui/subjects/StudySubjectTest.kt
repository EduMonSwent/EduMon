package com.android.sample.ui.subjects

// Parts of this file have been written using the help of an LLM.

import com.android.sample.feature.subjects.model.StudySubject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StudySubjectTest {

  @Test
  fun studySubject_creates_with_all_fields() {
    val subject =
        StudySubject(id = "sub1", name = "Mathematics", colorIndex = 3, totalStudyMinutes = 120)

    assertEquals("sub1", subject.id)
    assertEquals("Mathematics", subject.name)
    assertEquals(3, subject.colorIndex)
    assertEquals(120, subject.totalStudyMinutes)
  }

  @Test
  fun studySubject_copy_changes_name() {
    val original = StudySubject(id = "sub1", name = "Math", colorIndex = 1, totalStudyMinutes = 60)

    val updated = original.copy(name = "Advanced Math")

    assertEquals("Advanced Math", updated.name)
    assertEquals(original.id, updated.id)
    assertEquals(original.colorIndex, updated.colorIndex)
    assertEquals(original.totalStudyMinutes, updated.totalStudyMinutes)
  }

  @Test
  fun studySubject_copy_changes_minutes() {
    val original = StudySubject(id = "sub1", name = "Math", colorIndex = 1, totalStudyMinutes = 60)

    val updated = original.copy(totalStudyMinutes = 120)

    assertEquals(120, updated.totalStudyMinutes)
    assertEquals(original.id, updated.id)
    assertEquals(original.name, updated.name)
    assertEquals(original.colorIndex, updated.colorIndex)
  }

  @Test
  fun studySubject_equality() {
    val subject1 = StudySubject("1", "Math", 1, 60)
    val subject2 = StudySubject("1", "Math", 1, 60)

    assertEquals(subject1, subject2)
    assertEquals(subject1.hashCode(), subject2.hashCode())
  }

  @Test
  fun studySubject_inequality_different_id() {
    val subject1 = StudySubject("1", "Math", 1, 60)
    val subject2 = StudySubject("2", "Math", 1, 60)

    assertNotEquals(subject1, subject2)
  }

  @Test
  fun studySubject_inequality_different_name() {
    val subject1 = StudySubject("1", "Math", 1, 60)
    val subject2 = StudySubject("1", "Physics", 1, 60)

    assertNotEquals(subject1, subject2)
  }

  @Test
  fun studySubject_toString_contains_all_fields() {
    val subject = StudySubject("sub1", "Math", 2, 120)
    val string = subject.toString()

    assert(string.contains("sub1"))
    assert(string.contains("Math"))
    assert(string.contains("2"))
    assert(string.contains("120"))
  }
}
