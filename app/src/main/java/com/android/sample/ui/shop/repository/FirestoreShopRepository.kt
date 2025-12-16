// This code was written with the assistance of an AI (LLM).
package com.android.sample.ui.shop.repository

import com.android.sample.R
import com.android.sample.core.helpers.DefaultDispatcherProvider
import com.android.sample.core.helpers.DispatcherProvider
import com.android.sample.profile.ProfileRepository
import com.android.sample.ui.shop.CosmeticItem
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

  private object Prefixes {
    const val OWNED = "owned:"
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

  private object DrawableNames {
    const val GLASSES = "shop_cosmetic_glasses"
    const val HAT = "shop_cosmetic_hat"
    const val SCARF = "shop_cosmetic_scarf"
    const val WINGS = "shop_cosmetic_wings"
    const val AURA = "shop_cosmetic_aura"
    const val CAPE = "shop_cosmetic_cape"
  }

  private val _items = MutableStateFlow(defaultCosmetics())
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
        .filter { it.startsWith(Prefixes.OWNED) }
        .map { it.removePrefix(Prefixes.OWNED) }
        .toSet()
  }

  private suspend fun fetchShopItems(): List<CosmeticItem> =
      withContext(dispatchers.io) {
        val snap = Tasks.await(shopItemsCol().get())
        if (snap.isEmpty) {
          seedDefaultItems()
          return@withContext defaultCosmetics()
        }
        snap.documents.mapNotNull { doc ->
          val id = doc.getString(Fields.ID) ?: return@mapNotNull null
          val name = doc.getString(Fields.NAME).orEmpty()
          val price = (doc.getLong(Fields.PRICE) ?: 0L).toInt()
          val imageResName = doc.getString(Fields.IMAGE_RES_NAME).orEmpty()
          val imageRes = imageResNameToDrawable(imageResName)
          CosmeticItem(id, name, price, imageRes, owned = false)
        }
      }

  private suspend fun seedDefaultItems() =
      withContext(dispatchers.io) {
        val defaults = defaultCosmetics()
        val batch = db.batch()
        defaults.forEach { item ->
          val data =
              mapOf(
                  Fields.ID to item.id,
                  Fields.NAME to item.name,
                  Fields.PRICE to item.price,
                  Fields.IMAGE_RES_NAME to drawableToImageResName(item.imageRes))
          batch.set(shopItemsCol().document(item.id), data)
        }
        Tasks.await(batch.commit())
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

    private fun imageResNameToDrawable(name: String): Int =
        when (name) {
          DrawableNames.GLASSES -> R.drawable.shop_cosmetic_glasses
          DrawableNames.HAT -> R.drawable.shop_cosmetic_hat
          DrawableNames.SCARF -> R.drawable.shop_cosmetic_scarf
          DrawableNames.WINGS -> R.drawable.shop_cosmetic_wings
          DrawableNames.AURA -> R.drawable.shop_cosmetic_aura
          DrawableNames.CAPE -> R.drawable.shop_cosmetic_cape
          else -> R.drawable.shop_cosmetic_glasses
        }

    private fun drawableToImageResName(res: Int): String =
        when (res) {
          R.drawable.shop_cosmetic_glasses -> DrawableNames.GLASSES
          R.drawable.shop_cosmetic_hat -> DrawableNames.HAT
          R.drawable.shop_cosmetic_scarf -> DrawableNames.SCARF
          R.drawable.shop_cosmetic_wings -> DrawableNames.WINGS
          R.drawable.shop_cosmetic_aura -> DrawableNames.AURA
          R.drawable.shop_cosmetic_cape -> DrawableNames.CAPE
          else -> DrawableNames.GLASSES
        }
  }
}
