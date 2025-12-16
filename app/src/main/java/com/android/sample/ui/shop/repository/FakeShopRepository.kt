// This code was written with the assistance of an AI (LLM).
package com.android.sample.ui.shop.repository

import com.android.sample.R
import com.android.sample.profile.ProfileRepository
import com.android.sample.ui.shop.CosmeticItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeShopRepository(private val profileRepository: ProfileRepository? = null) :
    ShopRepository {

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

  private object Prefixes {
    const val OWNED = "owned:"
  }

  private val _items = MutableStateFlow(defaultCosmetics())
  override val items: StateFlow<List<CosmeticItem>> = _items.asStateFlow()

  override suspend fun getItems(): List<CosmeticItem> {
    refreshOwnedStatus()
    return _items.value
  }

  override suspend fun purchaseItem(itemId: String): Boolean {
    _items.update { list ->
      list.map { item -> if (item.id == itemId) item.copy(owned = true) else item }
    }
    return true
  }

  override suspend fun getOwnedItemIds(): Set<String> {
    return getOwnedIdsFromProfile()
  }

  override suspend fun refreshOwnedStatus() {
    val ownedIds = getOwnedIdsFromProfile()
    _items.update { list -> list.map { item -> item.copy(owned = ownedIds.contains(item.id)) } }
  }

  private fun getOwnedIdsFromProfile(): Set<String> {
    val profile = profileRepository?.profile?.value ?: return emptySet()
    return profile.accessories
        .filter { it.startsWith(Prefixes.OWNED) }
        .map { it.removePrefix(Prefixes.OWNED) }
        .toSet()
  }

  companion object {
    fun defaultCosmetics(): List<CosmeticItem> =
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
}
