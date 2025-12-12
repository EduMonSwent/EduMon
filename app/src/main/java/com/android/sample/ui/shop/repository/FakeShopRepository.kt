package com.android.sample.ui.shop.repository

import com.android.sample.R
import com.android.sample.ui.shop.CosmeticItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory fake implementation of [ShopRepository] for testing purposes. Does not persist data
 * beyond the current session.
 */
class FakeShopRepository : ShopRepository {

  private val ownedIds = mutableSetOf<String>()

  private val _items = MutableStateFlow(defaultCosmetics())
  override val items: StateFlow<List<CosmeticItem>> = _items.asStateFlow()

  override suspend fun getItems(): List<CosmeticItem> = _items.value

  override suspend fun purchaseItem(itemId: String): Boolean {
    if (ownedIds.contains(itemId)) return false
    ownedIds.add(itemId)
    updateItemsOwnedStatus()
    return true
  }

  override suspend fun getOwnedItemIds(): Set<String> = ownedIds.toSet()

  override suspend fun refreshOwnedStatus() {
    updateItemsOwnedStatus()
  }

  private fun updateItemsOwnedStatus() {
    _items.update { list -> list.map { item -> item.copy(owned = ownedIds.contains(item.id)) } }
  }

  companion object {
    // Item IDs
    private const val ID_GLASSES = "glasses"
    private const val ID_HAT = "hat"
    private const val ID_SCARF = "scarf"
    private const val ID_WINGS = "wings"
    private const val ID_AURA = "aura"
    private const val ID_CAPE = "cape"

    // Item names
    private const val NAME_GLASSES = "Cool Shades"
    private const val NAME_HAT = "Wizard Hat"
    private const val NAME_SCARF = "Red Scarf"
    private const val NAME_WINGS = "Cyber Wings"
    private const val NAME_AURA = "Epic Aura"
    private const val NAME_CAPE = "Hero Cape"

    // Prices
    private const val STANDARD_PRICE = 200
    private const val EPIC_PRICE = 1500

    /** Default cosmetic items available in the shop. */
    fun defaultCosmetics(): List<CosmeticItem> =
        listOf(
            CosmeticItem(
                ID_GLASSES, NAME_GLASSES, STANDARD_PRICE, R.drawable.shop_cosmetic_glasses),
            CosmeticItem(ID_HAT, NAME_HAT, STANDARD_PRICE, R.drawable.shop_cosmetic_hat),
            CosmeticItem(ID_SCARF, NAME_SCARF, STANDARD_PRICE, R.drawable.shop_cosmetic_scarf),
            CosmeticItem(ID_WINGS, NAME_WINGS, STANDARD_PRICE, R.drawable.shop_cosmetic_wings),
            CosmeticItem(ID_AURA, NAME_AURA, EPIC_PRICE, R.drawable.shop_cosmetic_aura),
            CosmeticItem(ID_CAPE, NAME_CAPE, STANDARD_PRICE, R.drawable.shop_cosmetic_cape))
  }
}
