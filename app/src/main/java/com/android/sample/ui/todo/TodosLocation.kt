package com.android.sample.ui.todo

import com.google.android.gms.maps.model.LatLng

data class CampusLocation(val id: String, val label: String, val position: LatLng)

object CampusLocations {

  val all: List<CampusLocation> =
      listOf(
          CampusLocation("BC", "BC – BC building", LatLng(46.518955, 6.566923)),
          CampusLocation("INM", "INM – Microcity", LatLng(46.520120, 6.565830)),
          CampusLocation("CM", "CM – CM building", LatLng(46.519650, 6.564900)),
      )

  fun findById(id: String?): CampusLocation? =
      all.firstOrNull { it.id.equals(id, ignoreCase = true) }

  fun filterByQuery(query: String): List<CampusLocation> {
    if (query.isBlank()) return all
    val q = query.trim()
    return all.filter {
      it.id.contains(q, ignoreCase = true) || it.label.contains(q, ignoreCase = true)
    }
  }
}
