// This code was written with the assistance of an AI (LLM).
package com.android.sample.domain.model

enum class AccessoryType(val id: String) {
  HAT("hat"),
  GLASSES("glasses"),
  SCARF("scarf"),
  WINGS("wings"),
  AURA("aura"),
  CAPE("cape");

  companion object {
    private val idMap = values().associateBy { it.id }

    fun fromId(id: String?): AccessoryType? = idMap[id]
  }
}
