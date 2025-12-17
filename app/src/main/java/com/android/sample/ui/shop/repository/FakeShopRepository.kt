// This code was written with the assistance of an AI (LLM).
package com.android.sample.ui.shop.repository

import com.android.sample.profile.ProfileRepository
import com.android.sample.ui.shop.CosmeticItem
import com.android.sample.ui.shop.ShopConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeShopRepository(private val profileRepository: ProfileRepository? = null) :
    ShopRepository {

  private val localOwnedIds = mutableSetOf<String>()

  private val _items = MutableStateFlow(ShopConstants.defaultCosmetics())
  override val items: StateFlow<List<CosmeticItem>> = _items.asStateFlow()

  override suspend fun getItems(): List<CosmeticItem> {
    refreshOwnedStatus()
    return _items.value
  }

  override suspend fun purchaseItem(itemId: String): Boolean {
    val currentOwnedIds = getOwnedItemIds()
    if (currentOwnedIds.contains(itemId)) return false

    localOwnedIds.add(itemId)

    _items.update { list ->
      list.map { item -> if (item.id == itemId) item.copy(owned = true) else item }
    }
    return true
  }

  override suspend fun getOwnedItemIds(): Set<String> {
    val profileOwnedIds = getOwnedIdsFromProfile()
    return localOwnedIds + profileOwnedIds
  }

  override suspend fun refreshOwnedStatus() {
    val ownedIds = getOwnedItemIds()
    _items.update { list -> list.map { item -> item.copy(owned = ownedIds.contains(item.id)) } }
  }

  private fun getOwnedIdsFromProfile(): Set<String> {
    val profile = profileRepository?.profile?.value ?: return emptySet()
    return profile.accessories
        .filter { it.startsWith(ShopConstants.OWNED_PREFIX) }
        .map { it.removePrefix(ShopConstants.OWNED_PREFIX) }
        .toSet()
  }

  companion object {
    fun defaultCosmetics(): List<CosmeticItem> = ShopConstants.defaultCosmetics()
  }
}
