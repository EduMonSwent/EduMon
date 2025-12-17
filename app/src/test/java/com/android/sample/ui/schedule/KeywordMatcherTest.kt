package com.android.sample.ui.schedule

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.feature.schedule.repository.schedule.KeywordMatcher
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlin.test.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class KeywordMatcherTest {

  private lateinit var context: Context
  private lateinit var matcher: KeywordMatcher

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    matcher = KeywordMatcher(context)
  }

  @Test
  fun `detects exercise keywords`() {
    assertTrue(matcher.isExercise("Exercices niveau 1"))
    assertFalse(matcher.isExercise("Cours magistral"))
  }

  @Test
  fun `detects lab keywords`() {
    assertTrue(matcher.isLab("Labo Informatique"))
    assertFalse(matcher.isLab("Projet de maths"))
  }

  @Test
  fun `detects project keywords`() {
    assertTrue(matcher.isProject("Projet final de groupe"))
    assertFalse(matcher.isProject("Laboratoire de chimie"))
  }

  @Test
  fun `detects lecture keywords`() {
    assertTrue(matcher.isLecture("Cours de physique"))
    assertFalse(matcher.isLecture("Exercices"))
  }

  @Test
  fun `detects exam categories`() {
    val cats = listOf("Horaires examinés", "other")
    assertTrue(matcher.isExam(cats))
  }

  @Test
  fun `isExam returns false on empty categories`() {
    assertFalse(matcher.isExam(emptyList()))
  }

  @Test
  fun `isExercise handles null safely`() {
    assertFalse(matcher.isExercise(null))
  }

  @Test
  fun `holiday detection matches mixed input`() {
    assertTrue(matcher.isHoliday("Vacances d'hiver"))
    assertTrue(matcher.isHoliday(listOf("Jour Férié", "Autre")))
    assertFalse(matcher.isHoliday("Regular class"))
  }
}
