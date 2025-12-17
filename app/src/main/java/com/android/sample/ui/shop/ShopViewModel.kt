// This code was written with the assistance of an AI (LLM).
package com.android.sample.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.R
import com.android.sample.profile.ProfileRepository
import com.android.sample.repos_providors.AppRepositories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class PurchaseResult {
  data class Success(val itemName: String) : PurchaseResult()

  data class InsufficientCoins(val itemName: String) : PurchaseResult()

  data class AlreadyOwned(val itemName: String) : PurchaseResult()

  object NoConnection : PurchaseResult()

  data class NetworkError(val message: String) : PurchaseResult()
}

class ShopViewModel(
    private val profileRepository: ProfileRepository = AppRepositories.profileRepository
) : ViewModel() {

  private object Constants {
    const val FLOW_TIMEOUT_MS = 5000L
    const val INITIAL_COINS = 0
    const val OWNED_PREFIX = "owned:"
    const val DEFAULT_ERROR_MESSAGE = "Purchase failed. Please try again."
  }

  private object ItemIds {
    const val GLASSES = "glasses"
    const val HAT = "hat"
    const val SCARF = "scarf"
    const val WINGS = "wings"
    const val AURA = "aura"
    const val CAPE = "cape"
  }

  private object ItemNames {
    const val GLASSES = "Cool Shades"
    const val HAT = "Wizard Hat"
    const val SCARF = "Red Scarf"
    const val WINGS = "Cyber Wings"
    const val AURA = "Epic Aura"
    const val CAPE = "Hero Cape"
  }

  private object Prices {
    const val STANDARD = 200
    const val EPIC = 1500
  }

  val userCoins: StateFlow<Int> =
      profileRepository.profile
          .map { it.coins }
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(Constants.FLOW_TIMEOUT_MS),
              Constants.INITIAL_COINS)

  private val _items = MutableStateFlow(initialCosmetics())
  val items: StateFlow<List<CosmeticItem>> = _items.asStateFlow()

  private val _isOnline = MutableStateFlow(true)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private val _lastPurchaseResult = MutableStateFlow<PurchaseResult?>(null)
  val lastPurchaseResult: StateFlow<PurchaseResult?> = _lastPurchaseResult.asStateFlow()

  private val _isPurchasing = MutableStateFlow(false)
  val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

  init {
    viewModelScope.launch {
      profileRepository.profile.collect { profile ->
        val ownedItemIds =
            profile.accessories
                .filter { it.startsWith(Constants.OWNED_PREFIX) }
                .map { it.removePrefix(Constants.OWNED_PREFIX) }
                .toSet()

        _items.update { currentItems ->
          currentItems.map { item -> item.copy(owned = ownedItemIds.contains(item.id)) }
        }
      }
    }
  }

  fun setNetworkStatus(isOnline: Boolean) {
    _isOnline.value = isOnline
  }

  fun buyItem(item: CosmeticItem): Boolean {
    if (_isPurchasing.value) return false

    if (!_isOnline.value) {
      _lastPurchaseResult.value = PurchaseResult.NoConnection
      return false
    }

    if (item.owned) {
      _lastPurchaseResult.value = PurchaseResult.AlreadyOwned(item.name)
      return false
    }

    val profile = profileRepository.profile.value

    if (profile.coins < item.price) {
      _lastPurchaseResult.value = PurchaseResult.InsufficientCoins(item.name)
      return false
    }

    _isPurchasing.value = true

    val ownedEntry = Constants.OWNED_PREFIX + item.id
    val updatedProfile =
        profile.copy(
            coins = profile.coins - item.price, accessories = profile.accessories + ownedEntry)

    viewModelScope.launch {
      try {
        profileRepository.updateProfile(updatedProfile)

        _items.update { currentItems ->
          currentItems.map { if (it.id == item.id) it.copy(owned = true) else it }
        }

        _lastPurchaseResult.value = PurchaseResult.Success(item.name)
      } catch (e: Exception) {
        _lastPurchaseResult.value =
            PurchaseResult.NetworkError(e.message ?: Constants.DEFAULT_ERROR_MESSAGE)
      } finally {
        _isPurchasing.value = false
      }
    }

    return true
  }

  fun clearPurchaseResult() {
    _lastPurchaseResult.value = null
  }

  private fun initialCosmetics(): List<CosmeticItem> =
      listOf(
          CosmeticItem(
              ItemIds.GLASSES,
              ItemNames.GLASSES,
              Prices.STANDARD,
              R.drawable.shop_cosmetic_glasses),
          CosmeticItem(ItemIds.HAT, ItemNames.HAT, Prices.STANDARD, R.drawable.shop_cosmetic_hat),
          CosmeticItem(
              ItemIds.SCARF, ItemNames.SCARF, Prices.STANDARD, R.drawable.shop_cosmetic_scarf),
          CosmeticItem(
              ItemIds.WINGS, ItemNames.WINGS, Prices.STANDARD, R.drawable.shop_cosmetic_wings),
          CosmeticItem(ItemIds.AURA, ItemNames.AURA, Prices.EPIC, R.drawable.shop_cosmetic_aura),
          CosmeticItem(
              ItemIds.CAPE, ItemNames.CAPE, Prices.STANDARD, R.drawable.shop_cosmetic_cape))
}
