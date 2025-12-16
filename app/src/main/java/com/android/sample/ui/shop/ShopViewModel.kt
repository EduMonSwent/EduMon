// This code was written with the assistance of an AI (LLM).
package com.android.sample.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.R
import com.android.sample.profile.ProfileRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.shop.repository.ShopRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShopViewModel(
    private val profileRepository: ProfileRepository = AppRepositories.profileRepository,
    private val shopRepository: ShopRepository = AppRepositories.shopRepository
) : ViewModel() {

  private object Constants {
    const val FLOW_TIMEOUT_MS = 5000L
    const val INITIAL_COINS = 0
    const val OWNED_PREFIX = "owned:"
  }

  val userCoins: StateFlow<Int> =
      profileRepository.profile
          .map { it.coins }
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(Constants.FLOW_TIMEOUT_MS),
              Constants.INITIAL_COINS)

  val items: StateFlow<List<CosmeticItem>> = shopRepository.items

  init {
    viewModelScope.launch {
      shopRepository.getItems()
      shopRepository.refreshOwnedStatus()
    }
  }

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

    if (profile.coins < item.price || item.owned) return false

    val ownedEntry = Constants.OWNED_PREFIX + item.id
    val alreadyOwned = profile.accessories.contains(ownedEntry)
    if (alreadyOwned) return false

    val updatedAccessories = profile.accessories + ownedEntry
    val updatedProfile =
        profile.copy(coins = profile.coins - item.price, accessories = updatedAccessories)

    viewModelScope.launch {
      profileRepository.updateProfile(updatedProfile)
      shopRepository.refreshOwnedStatus()
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
