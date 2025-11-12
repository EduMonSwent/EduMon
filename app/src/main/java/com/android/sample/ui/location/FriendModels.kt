package com.android.sample.ui.location

import com.google.android.gms.maps.model.LatLng

// Friend presence mode shown on the map.
enum class FriendMode {
  STUDY,
  BREAK,
  IDLE
}

// Minimal data the map needs per friend.
data class FriendStatus(
    val id: String, // uid (stable key)
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val mode: FriendMode
)

// UI state for the Study Together screen.
data class StudyTogetherUiState(
    val friends: List<FriendStatus> = emptyList(),
    val userPosition: LatLng? = null,
    val selectedFriend: FriendStatus? = null,
    val isUserSelected: Boolean = false,
    // Optional ephemeral UI error message (one-shot). Set by ViewModel on failures.
    val errorMessage: String? = null
) {
  val effectiveUserLatLng: LatLng = userPosition ?: DEFAULT_LOCATION
}

// EPFL default center (optional fallback).
internal val DEFAULT_LOCATION = LatLng(46.5191, 6.5668)
