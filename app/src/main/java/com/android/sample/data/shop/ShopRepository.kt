package com.android.sample.data.shop

import kotlinx.coroutines.flow.Flow

/**
 * Repository boundary for the Shop feature.
 */
interface ShopRepository {
    data class ShopItem(
        val id: String,
        val title: String,
        val price: Int,
        val rarity: String,
        val auraId: String? = null,
        val drawableName: String? = null
    )

    data class PurchaseResult(
        val success: Boolean,
        val newCoins: Int,
        val ownedItemId: String?,
        val errorMessage: String? = null
    )

    fun observeCoins(uid: String): Flow<Int>
    fun observeOwnedItemIds(uid: String): Flow<Set<String>>
    suspend fun purchase(uid: String, item: ShopItem): PurchaseResult

    /** Dev utility to credit or debit coins, positive adds, negative removes */
    suspend fun addCoins(uid: String, amount: Int)
}
