// This code was written with the assistance of an AI (LLM).
package com.android.sample.domain.model

enum class EduMonType(val id: String) {
  PYROMON("pyromon"),
  AQUAMON("aquamon"),
  FLORAMON("floramon");

  companion object {
    private val idMap = values().associateBy { it.id }
    private val DEFAULT = PYROMON

    fun fromId(id: String?): EduMonType = idMap[id] ?: DEFAULT
  }
}
