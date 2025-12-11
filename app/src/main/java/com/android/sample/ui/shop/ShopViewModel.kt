package com.android.sample.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.R
import com.android.sample.profile.ProfileRepository
import com.android.sample.profile.ProfileRepositoryProvider
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.shop.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// The assistance of an AI tool (ChatGPT) was solicited in writing this file.

/** Represents the result of a purchase attempt. */
sealed class PurchaseResult {
  /** Purchase completed successfully. */
  data class Success(val itemName: String) : PurchaseResult()

  /** Purchase failed due to insufficient coins. */
  data class InsufficientCoins(val itemName: String) : PurchaseResult()

  /** Purchase failed because item is already owned. */
  data class AlreadyOwned(val itemName: String) : PurchaseResult()

  /** Purchase failed due to no network connection. */
  object NoConnection : PurchaseResult()

  /** Purchase failed due to a network/server error. */
  data class NetworkError(val message: String) : PurchaseResult()
}

class ShopViewModel(
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val shopRepository: ShopRepository = AppRepositories.shopRepository
) : ViewModel() {

  // User's coin balance
  val userCoins: StateFlow<Int> =
      profileRepository.profile
          .map { it.coins }
          .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

  // Shop items
  private val _items = MutableStateFlow(initialCosmetics())
  val items: StateFlow<List<CosmeticItem>> = _items.asStateFlow()

  // Network availability (set from UI layer)
  private val _isOnline = MutableStateFlow(true)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  // Last purchase result for UI feedback
  private val _lastPurchaseResult = MutableStateFlow<PurchaseResult?>(null)
  val lastPurchaseResult: StateFlow<PurchaseResult?> = _lastPurchaseResult.asStateFlow()

  // Loading state during purchase
  private val _isPurchasing = MutableStateFlow(false)
  val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

  /** Updates network availability status. Should be called from UI when network state changes. */
  fun setNetworkStatus(isOnline: Boolean) {
    _isOnline.value = isOnline
  }

  /**
   * Attempts to purchase an item. Returns true if purchase was initiated, false if blocked
   * immediately.
   */
  fun buyItem(item: CosmeticItem): Boolean {
    // Block if already purchasing
    if (_isPurchasing.value) return false

    // Block if offline
    if (!_isOnline.value) {
      _lastPurchaseResult.value = PurchaseResult.NoConnection
      return false
    }

    // Block if already owned
    if (item.owned) {
      _lastPurchaseResult.value = PurchaseResult.AlreadyOwned(item.name)
      return false
    }

    val profile = profileRepository.profile.value

    // Block if not enough coins
    if (profile.coins < item.price) {
      _lastPurchaseResult.value = PurchaseResult.InsufficientCoins(item.name)
      return false
    }

    // Start purchase
    _isPurchasing.value = true

    val updatedProfile =
        profile.copy(
            coins = profile.coins - item.price,
            accessories = profile.accessories + "owned:${item.id}")

    viewModelScope.launch {
      try {
        profileRepository.updateProfile(updatedProfile)

        // Update local items state
        _items.update { currentItems ->
          currentItems.map { if (it.id == item.id) it.copy(owned = true) else it }
        }

        _lastPurchaseResult.value = PurchaseResult.Success(item.name)
      } catch (e: Exception) {
        // If update fails, show error (coins not deducted since profile wasn't saved)
        _lastPurchaseResult.value =
            PurchaseResult.NetworkError(e.message ?: "Purchase failed. Please try again.")
      } finally {
        _isPurchasing.value = false
      }
    }

    return true
  }

  /** Clears the last purchase result (call after showing feedback to user). */
  fun clearPurchaseResult() {
    _lastPurchaseResult.value = null
  }
}

/** Initial shop cosmetics list with constants. */
private fun initialCosmetics(): List<CosmeticItem> {
  // Item IDs
  val idGlasses = "glasses"
  val idHat = "hat"
  val idScarf = "scarf"
  val idWings = "wings"
  val idAura = "aura"
  val idCape = "cape"

  // Prices
  val standardPrice = 200
  val epicPrice = 1500

  return listOf(
      CosmeticItem(idGlasses, "Cool Shades", standardPrice, R.drawable.shop_cosmetic_glasses),
      CosmeticItem(idHat, "Wizard Hat", standardPrice, R.drawable.shop_cosmetic_hat),
      CosmeticItem(idScarf, "Red Scarf", standardPrice, R.drawable.shop_cosmetic_scarf),
      CosmeticItem(idWings, "Cyber Wings", standardPrice, R.drawable.shop_cosmetic_wings),
      CosmeticItem(idAura, "Epic Aura", epicPrice, R.drawable.shop_cosmetic_aura),
      CosmeticItem(idCape, "Hero Cape", standardPrice, R.drawable.shop_cosmetic_cape))
}
