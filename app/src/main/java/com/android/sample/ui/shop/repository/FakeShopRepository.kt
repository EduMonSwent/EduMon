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
    /** Default cosmetic items available in the shop. */
    fun defaultCosmetics(): List<CosmeticItem> =
        listOf(
            CosmeticItem("glasses", "Cool Shades", 200, R.drawable.shop_cosmetic_glasses),
            CosmeticItem("hat", "Wizard Hat", 200, R.drawable.shop_cosmetic_hat),
            CosmeticItem("scarf", "Red Scarf", 200, R.drawable.shop_cosmetic_scarf),
            CosmeticItem("wings", "Cyber Wings", 200, R.drawable.shop_cosmetic_wings),
            CosmeticItem("aura", "Epic Aura", 1500, R.drawable.shop_cosmetic_aura),
            CosmeticItem("cape", "Hero Cape", 200, R.drawable.shop_cosmetic_cape))
  }
}
