package com.android.sample.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.data.shop.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShopUiState(
    val coins: Int = 0,
    val catalog: List<ShopRepository.ShopItem> = emptyList(),
    val owned: Set<String> = emptySet(),
    val purchasingId: String? = null,
    val message: String? = null
)

class ShopViewModel(
    private val uid: String,
    private val repo: ShopRepository,
    initialCatalog: List<ShopRepository.ShopItem>
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _purchasingId = MutableStateFlow<String?>(null)

    val state: StateFlow<ShopUiState> =
        combine(
            repo.observeCoins(uid),
            repo.observeOwnedItemIds(uid),
            _purchasingId,
            _message
        ) { coins, owned, purchasingId, message ->
            ShopUiState(
                coins = coins,
                catalog = initialCatalog,
                owned = owned,
                purchasingId = purchasingId,
                message = message
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ShopUiState(catalog = initialCatalog))

    fun clearMessage() { _message.value = null }

    fun purchase(item: ShopRepository.ShopItem) {
        viewModelScope.launch {
            _purchasingId.value = item.id
            val result = repo.purchase(uid, item)
            _purchasingId.value = null
            _message.value = if (result.success) "Purchased" else result.errorMessage ?: "Purchase failed"
        }
    }

    // dev utility to seed coins quickly
    fun addCoinsDev(amount: Int) {
        viewModelScope.launch {
            val ds = repo
            if (ds is com.android.sample.data.shop.FirestoreShopDataSource) {
                val cur = state.value.coins
                runCatching { ds.setCoins(uid, cur + amount) }
                _message.value = "Coins +$amount"
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val uid: String,
        private val repo: ShopRepository,
        private val catalog: List<ShopRepository.ShopItem>
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShopViewModel(uid, repo, catalog) as T
        }
    }
}
