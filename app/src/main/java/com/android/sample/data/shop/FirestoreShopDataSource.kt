package com.android.sample.data.shop

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreShopDataSource : ShopRepository {

    private val db = Firebase.firestore

    override fun observeCoins(uid: String): Flow<Int> = callbackFlow {
        val ref = db.collection("users").document(uid)
        val reg = ref.addSnapshotListener { snap, _ ->
            val coins = snap?.getLong("coins")?.toInt() ?: 0
            trySend(coins)
        }
        awaitClose { reg.remove() }
    }

    override fun observeOwnedItemIds(uid: String): Flow<Set<String>> = callbackFlow {
        val ref = db.collection("users").document(uid)
        val reg = ref.addSnapshotListener { snap, _ ->
            val list = snap?.get("owned") as? List<*> ?: emptyList<Any>()
            trySend(list.filterIsInstance<String>().toSet())
        }
        awaitClose { reg.remove() }
    }

    override suspend fun purchase(uid: String, item: ShopRepository.ShopItem): ShopRepository.PurchaseResult {
        val ref = db.collection("users").document(uid)
        return try {
            val result = db.runTransaction { tx ->
                val snap = tx.get(ref)
                val coins = snap.getLong("coins")?.toInt() ?: 0
                if (coins < item.price) {
                    return@runTransaction ShopRepository.PurchaseResult(false, coins, null, "Not enough coins")
                }
                val newCoins = coins - item.price
                tx.set(ref, mapOf("coins" to newCoins), SetOptions.merge())
                tx.update(ref, "owned", FieldValue.arrayUnion(item.id))
                ShopRepository.PurchaseResult(true, newCoins, item.id, null)
            }.await()
            result
        } catch (t: Throwable) {
            ShopRepository.PurchaseResult(false, -1, null, t.message ?: "Purchase failed")
        }
    }

    override suspend fun addCoins(uid: String, amount: Int) {
        // Safe increment, creates the field if missing
        val ref = db.collection("users").document(uid)
        ref.update("coins", FieldValue.increment(amount.toLong()))
            .await()
    }
}
