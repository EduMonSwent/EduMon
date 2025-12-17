// This code was written with the assistance of an AI (LLM).
package com.android.sample.ui.shop.repository

import com.android.sample.core.helpers.DefaultDispatcherProvider
import com.android.sample.core.helpers.DispatcherProvider
import com.android.sample.profile.ProfileRepository
import com.android.sample.ui.shop.CosmeticItem
import com.android.sample.ui.shop.ShopConstants
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class FirestoreShopRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val profileRepository: ProfileRepository? = null,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : ShopRepository {

  private object Collections {
    const val SHOP_ITEMS = "shopItems"
  }

  private object Fields {
    const val ID = "id"
    const val NAME = "name"
    const val PRICE = "price"
    const val IMAGE_RES_NAME = "imageResName"
  }

  private val _items = MutableStateFlow(ShopConstants.defaultCosmetics())
  override val items: StateFlow<List<CosmeticItem>> = _items.asStateFlow()

  private fun shopItemsCol() = db.collection(Collections.SHOP_ITEMS)

  override suspend fun getItems(): List<CosmeticItem> =
      withContext(dispatchers.io) {
        val items = fetchShopItems()
        val owned = getOwnedIdsFromProfile()
        val itemsWithOwnership = items.map { it.copy(owned = owned.contains(it.id)) }
        _items.value = itemsWithOwnership
        itemsWithOwnership
      }

  override suspend fun purchaseItem(itemId: String): Boolean =
      withContext(dispatchers.io) {
        _items.update { list ->
          list.map { item -> if (item.id == itemId) item.copy(owned = true) else item }
        }
        true
      }

  override suspend fun getOwnedItemIds(): Set<String> =
      withContext(dispatchers.io) { getOwnedIdsFromProfile() }

  override suspend fun refreshOwnedStatus() =
      withContext(dispatchers.io) {
        val owned = getOwnedIdsFromProfile()
        _items.update { list -> list.map { it.copy(owned = owned.contains(it.id)) } }
      }

  private fun getOwnedIdsFromProfile(): Set<String> {
    val profile = profileRepository?.profile?.value ?: return emptySet()
    return profile.accessories
        .filter { it.startsWith(ShopConstants.OWNED_PREFIX) }
        .map { it.removePrefix(ShopConstants.OWNED_PREFIX) }
        .toSet()
  }

  private suspend fun fetchShopItems(): List<CosmeticItem> =
      withContext(dispatchers.io) {
        val snap = Tasks.await(shopItemsCol().get())
        if (snap.isEmpty) {
          seedDefaultItems()
          return@withContext ShopConstants.defaultCosmetics()
        }
        snap.documents.mapNotNull { doc ->
          val id = doc.getString(Fields.ID) ?: return@mapNotNull null
          val name = doc.getString(Fields.NAME).orEmpty()
          val price = (doc.getLong(Fields.PRICE) ?: 0L).toInt()
          val imageResName = doc.getString(Fields.IMAGE_RES_NAME).orEmpty()
          val imageRes = ShopConstants.imageResNameToDrawable(imageResName)
          CosmeticItem(id, name, price, imageRes, owned = false)
        }
      }

  private suspend fun seedDefaultItems() =
      withContext(dispatchers.io) {
        val defaults = ShopConstants.defaultCosmetics()
        val batch = db.batch()
        defaults.forEach { item ->
          val data =
              mapOf(
                  Fields.ID to item.id,
                  Fields.NAME to item.name,
                  Fields.PRICE to item.price,
                  Fields.IMAGE_RES_NAME to ShopConstants.drawableToImageResName(item.imageRes))
          batch.set(shopItemsCol().document(item.id), data)
        }
        Tasks.await(batch.commit())
      }

  companion object {
    fun defaultCosmetics(): List<CosmeticItem> = ShopConstants.defaultCosmetics()
  }
}
