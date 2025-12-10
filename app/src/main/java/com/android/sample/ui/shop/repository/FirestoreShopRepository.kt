package com.android.sample.ui.shop.repository

import com.android.sample.R
import com.android.sample.core.helpers.DefaultDispatcherProvider
import com.android.sample.core.helpers.DispatcherProvider
import com.android.sample.core.helpers.setMerged
import com.android.sample.ui.shop.CosmeticItem
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Firestore-backed implementation of [ShopRepository].
 *
 * Stores:
 * - Shop items in /shopItems collection (shared across all users)
 * - Owned items per user in /users/{uid}/ownedCosmetics collection
 */
class FirestoreShopRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : ShopRepository {

  private val _items = MutableStateFlow(defaultCosmetics())
  override val items: StateFlow<List<CosmeticItem>> = _items.asStateFlow()

  // ---------- Helpers ----------

  private fun isSignedIn(): Boolean = auth.currentUser != null

  private fun userOwnedCol() =
      auth.currentUser?.uid?.let {
        db.collection("users").document(it).collection("ownedCosmetics")
      }

  private fun shopItemsCol() = db.collection("shopItems")

  // ---------- API ----------

  override suspend fun getItems(): List<CosmeticItem> =
      withContext(dispatchers.io) {
        val items = fetchShopItems()
        val owned = if (isSignedIn()) fetchOwnedIds() else emptySet()
        val itemsWithOwnership = items.map { it.copy(owned = owned.contains(it.id)) }

        // Update the flow so UI stays in sync
        _items.value = itemsWithOwnership

        itemsWithOwnership
      }

  override suspend fun purchaseItem(itemId: String): Boolean =
      withContext(dispatchers.io) {
        if (!isSignedIn()) return@withContext false
        val col = userOwnedCol() ?: return@withContext false

        // Check if already owned
        val existingSnap = Tasks.await(col.document(itemId).get())
        if (existingSnap.exists()) return@withContext false

        // Add to owned collection
        val data = mapOf("itemId" to itemId, "purchasedAt" to FieldValue.serverTimestamp())
        col.document(itemId).setMerged(data)

        // Update local state
        _items.update { list ->
          list.map { item -> if (item.id == itemId) item.copy(owned = true) else item }
        }
        true
      }

  override suspend fun getOwnedItemIds(): Set<String> =
      withContext(dispatchers.io) {
        if (!isSignedIn()) return@withContext emptySet()
        fetchOwnedIds()
      }

  override suspend fun refreshOwnedStatus() =
      withContext(dispatchers.io) {
        val owned = if (isSignedIn()) fetchOwnedIds() else emptySet()
        _items.update { list -> list.map { it.copy(owned = owned.contains(it.id)) } }
      }

  // ---------- Private Fetch Methods ----------

  /** Fetches shop items from Firestore; seeds defaults if collection is empty. */
  private suspend fun fetchShopItems(): List<CosmeticItem> =
      withContext(dispatchers.io) {
        val snap = Tasks.await(shopItemsCol().get())
        if (snap.isEmpty) {
          seedDefaultItems()
          return@withContext defaultCosmetics()
        }
        snap.documents.mapNotNull { doc ->
          val id = doc.getString("id") ?: return@mapNotNull null
          val name = doc.getString("name") ?: ""
          val price = (doc.getLong("price") ?: 0L).toInt()
          val imageResName = doc.getString("imageResName") ?: ""
          val imageRes = imageResNameToDrawable(imageResName)
          CosmeticItem(id, name, price, imageRes, owned = false)
        }
      }

  /** Fetches owned item IDs for the current user. */
  private suspend fun fetchOwnedIds(): Set<String> =
      withContext(dispatchers.io) {
        val col = userOwnedCol() ?: return@withContext emptySet()
        val snap = Tasks.await(col.get())
        snap.documents.mapNotNull { it.getString("itemId") }.toSet()
      }

  /** Seeds default cosmetic items into Firestore if the collection is empty. */
  private suspend fun seedDefaultItems() =
      withContext(dispatchers.io) {
        val defaults = defaultCosmetics()
        val batch = db.batch()
        defaults.forEach { item ->
          val data =
              mapOf(
                  "id" to item.id,
                  "name" to item.name,
                  "price" to item.price,
                  "imageResName" to drawableToImageResName(item.imageRes))
          batch.set(shopItemsCol().document(item.id), data)
        }
        Tasks.await(batch.commit())
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

    /** Maps image resource name string to actual drawable resource ID. */
    private fun imageResNameToDrawable(name: String): Int =
        when (name) {
          "shop_cosmetic_glasses" -> R.drawable.shop_cosmetic_glasses
          "shop_cosmetic_hat" -> R.drawable.shop_cosmetic_hat
          "shop_cosmetic_scarf" -> R.drawable.shop_cosmetic_scarf
          "shop_cosmetic_wings" -> R.drawable.shop_cosmetic_wings
          "shop_cosmetic_aura" -> R.drawable.shop_cosmetic_aura
          "shop_cosmetic_cape" -> R.drawable.shop_cosmetic_cape
          else -> R.drawable.shop_cosmetic_glasses // fallback
        }

    /** Maps drawable resource ID to its string name for storage. */
    private fun drawableToImageResName(res: Int): String =
        when (res) {
          R.drawable.shop_cosmetic_glasses -> "shop_cosmetic_glasses"
          R.drawable.shop_cosmetic_hat -> "shop_cosmetic_hat"
          R.drawable.shop_cosmetic_scarf -> "shop_cosmetic_scarf"
          R.drawable.shop_cosmetic_wings -> "shop_cosmetic_wings"
          R.drawable.shop_cosmetic_aura -> "shop_cosmetic_aura"
          R.drawable.shop_cosmetic_cape -> "shop_cosmetic_cape"
          else -> "shop_cosmetic_glasses"
        }
  }
}
