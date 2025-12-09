package com.android.sample.feature.subjects.model

/**
 * This code has been written partially using A.I (LLM).
 *
 * Represents a study subject defined by the user.
 */
data class StudySubject(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val totalStudyMinutes: Int,
)
