package com.android.sample.ui.schedule

import com.android.sample.feature.weeks.data.Ba1Week14Materials
import org.junit.Assert.*
import org.junit.Test

class Ba1Week14MaterialsTest {

  @Test
  fun forCourse_knownCourse_returnsMaterials() {
    val materials = Ba1Week14Materials.forCourse("Algèbre linéaire")

    assertNotNull(materials)
    assertEquals("file:///android_asset/ba1/algebre/lecture_week14.pdf", materials?.lecturePdf)
    assertEquals("file:///android_asset/ba1/algebre/exercises_week14.pdf", materials?.exercisePdf)
    assertNull(materials?.labPdf)
  }

  @Test
  fun forCourse_courseWithOnlyLecture_returnsExerciseNull() {
    val materials = Ba1Week14Materials.forCourse("Introduction à la programmation")

    assertNotNull(materials)
    assertEquals("file:///android_asset/ba1/intro_prog/lecture.pdf", materials?.lecturePdf)
    assertNull(materials?.exercisePdf)
    assertNull(materials?.labPdf)
  }

  @Test
  fun forCourse_courseWithNoMaterials_returnsAllNull() {
    val materials = Ba1Week14Materials.forCourse("Physique générale : mécanique")

    assertNotNull(materials)
    assertNull(materials?.lecturePdf)
    assertNull(materials?.exercisePdf)
    assertNull(materials?.labPdf)
  }

  @Test
  fun forCourse_unknownCourse_returnsNull() {
    val materials = Ba1Week14Materials.forCourse("Some random course")

    assertNull(materials)
  }
}
