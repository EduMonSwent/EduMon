package com.android.sample.ui.location

import com.android.sample.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Contract the VM depends on. */
interface FriendRepository {

  val friendsFlow: kotlinx.coroutines.flow.Flow<List<FriendStatus>>

  suspend fun addFriendByUid(frienduid: String): FriendStatus

  suspend fun removeFriend(frienduid: String): Unit
}

/** Simple in-memory repo useful for previews/tests. */
class FakeFriendRepository(
    seed: List<FriendStatus> =
        listOf(
            FriendStatus("U1", "Alae", 46.5208, 6.5674, FriendMode.STUDY),
            FriendStatus("U2", "Florian", 46.5186, 6.5649, FriendMode.BREAK),
            FriendStatus("U3", "Khalil", 46.5197, 6.5702, FriendMode.IDLE),
        )
) : FriendRepository {

  private val _state = MutableStateFlow(seed.sortedBy { it.name.lowercase() })
  override val friendsFlow: StateFlow<List<FriendStatus>> = _state.asStateFlow()

  override suspend fun addFriendByUid(frienduid: String): FriendStatus {
    require(frienduid.isNotBlank()) { R.string.enter_uid }
    require(!(_state.value.any { it.id == frienduid })) { R.string.friend_exist_already }
    val (lat, lon) = nextDefaultPosition()
    val newFriend =
        FriendStatus(
            id = frienduid,
            name = "Friend $frienduid",
            latitude = lat,
            longitude = lon,
            mode = FriendMode.STUDY,
        )
    _state.value = (_state.value + newFriend).sortedBy { it.name.lowercase() }
    return newFriend
  }

  override suspend fun removeFriend(frienduid: String) {
    _state.value = _state.value.filterNot { it.id == frienduid }
  }

  // Small helper to avoid overlapping markers when auto-creating test friends.
  private fun nextDefaultPosition(): Pair<Double, Double> {
    val baseLat = 46.5191
    val baseLon = 6.5668
    val idx = _state.value.size % 6 // 6 tiny steps around EPFL
    val step = 0.0006 * idx
    return baseLat + step to baseLon + step
  }
}
