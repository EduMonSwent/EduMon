package com.android.sample.ui.location

/** Simple location model reused by todo + map. */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String,
)

/** Abstraction over a location search service. */
interface LocationRepository {
  suspend fun search(query: String): List<Location>
}
