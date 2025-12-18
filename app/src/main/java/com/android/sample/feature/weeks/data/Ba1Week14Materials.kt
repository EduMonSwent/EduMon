package com.android.sample.feature.weeks.data

object Ba1Week14Materials {

  data class Materials(
      val lecturePdf: String? = null,
      val exercisePdf: String? = null,
      val labPdf: String? = null
  )

  private const val WEEK = 14

  private val materials =
      mapOf(
          "Algèbre linéaire" to
              Materials(
                  lecturePdf = "file:///android_asset/ba1/algebre/lecture_week14.pdf",
                  exercisePdf = "file:///android_asset/ba1/algebre/exercises_week14.pdf",
                  labPdf = null),
          "Analyse I" to
              Materials(
                  lecturePdf = "file:///android_asset/ba1/analyse/lecture.pdf",
                  exercisePdf = "file:///android_asset/ba1/analyse/exercises_week14.pdf",
                  labPdf = null),
          "Advanced information, computation, communication I" to
              Materials(
                  lecturePdf = "file:///android_asset/ba1/aicc/week_14.pdf",
                  exercisePdf = "file:///android_asset/ba1/aicc/exercises.pdf",
                  labPdf = null),
          "Introduction à la programmation" to
              Materials(
                  lecturePdf = "file:///android_asset/ba1/intro_prog/lecture.pdf",
                  exercisePdf = null,
                  labPdf = null),
          "Physique générale : mécanique" to
              Materials(lecturePdf = null, exercisePdf = null, labPdf = null))

  fun forCourse(course: String): Materials? = materials[course]
}
