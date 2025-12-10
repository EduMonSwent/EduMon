package com.android.sample.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.profile.ProfileRepository
import com.android.sample.profile.ProfileRepositoryProvider
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.shop.repository.ShopRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// The assistance of an AI tool (ChatGPT) was solicited in writing this file.

/**
 * ViewModel for the Shop screen.
 * Manages cosmetic items and purchase logic with Firebase persistence.
 */
class ShopViewModel(
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val shopRepository: ShopRepository = AppRepositories.shopRepository
) : ViewModel() {

  /** User's current coin balance. */
  val userCoins: StateFlow<Int> =
      profileRepository.profile
          .map { it.coins }
          .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

  /** List of cosmetic items with owned status. */
  val items: StateFlow<List<CosmeticItem>> = shopRepository.items

  init {
    // Load items from Firestore (seeds defaults if empty) and refresh owned status
    viewModelScope.launch {
      shopRepository.getItems() // This triggers seeding if shopItems collection is empty
      shopRepository.refreshOwnedStatus()
    }
  }

  /**
   * Attempts to purchase an item.
   * @param item The cosmetic item to purchase.
   * @return true if purchase was successful, false otherwise.
   */
  fun buyItem(item: CosmeticItem): Boolean {
    val profile = profileRepository.profile.value

    // Check if user has enough coins and item is not already owned
    if (profile.coins < item.price || item.owned) return false

    // Deduct coins from profile
    val updatedProfile = profile.copy(coins = profile.coins - item.price)
    viewModelScope.launch { profileRepository.updateProfile(updatedProfile) }

    // Mark item as purchased in Firestore
    viewModelScope.launch { shopRepository.purchaseItem(item.id) }

    return true
  }
}

