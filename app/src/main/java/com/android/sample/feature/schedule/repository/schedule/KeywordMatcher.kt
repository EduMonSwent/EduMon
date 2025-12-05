package com.android.sample.feature.schedule.repository.schedule

import android.content.Context
import com.android.sample.R

class KeywordMatcher(private val context: Context) {

  private fun list(id: Int): List<String> =
      context.resources.getStringArray(id).map { it.lowercase() }

  private fun contains(text: String?, keywords: List<String>): Boolean {
    if (text == null) return false
    val lc = text.lowercase()
    return keywords.any { lc.contains(it) }
  }

  fun isExercise(text: String?): Boolean = contains(text, list(R.array.ics_keywords_exercise))

  fun isLab(text: String?): Boolean = contains(text, list(R.array.ics_keywords_lab))

  fun isProject(text: String?): Boolean = contains(text, list(R.array.ics_keywords_project))

  fun isLecture(text: String?): Boolean = contains(text, list(R.array.ics_keywords_lecture))

  fun isExam(categories: List<String>): Boolean {
    val keywords = list(R.array.ics_keywords_exam)
    return categories.any { cat -> contains(cat, keywords) }
  }
}
