package com.android.sample.ui.shop.repository

import com.android.sample.ui.shop.CosmeticItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for shop cosmetic items. Provides methods to fetch, purchase, and manage
 * owned cosmetics.
 */
interface ShopRepository {
  /** Observable flow of all cosmetic items with their owned status. */
  val items: StateFlow<List<CosmeticItem>>

  /** Fetches the current list of cosmetic items from the data source. */
  suspend fun getItems(): List<CosmeticItem>

  /** Marks an item as owned after purchase. Returns true if successful. */
  suspend fun purchaseItem(itemId: String): Boolean

  /** Returns the list of item IDs that the user owns. */
  suspend fun getOwnedItemIds(): Set<String>

  /** Syncs owned items from storage and updates the items flow. */
  suspend fun refreshOwnedStatus()
}
