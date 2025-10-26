package com.android.sample.ui.shop

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.android.sample.R

class ShopViewModel : ViewModel() {

    private val _userCoins = MutableStateFlow(1500)
    val userCoins: StateFlow<Int> = _userCoins

    private val _items = MutableStateFlow(initialCosmetics())
    val items: StateFlow<List<CosmeticItem>> = _items

    /**
     * Attempts to buy an item.
     * @return true if the purchase succeeds, false otherwise.
     */
    fun buyItem(item: CosmeticItem): Boolean {
        val coins = _userCoins.value
        return if (coins >= item.price && !item.owned) {
            _userCoins.value = coins - item.price
            _items.value = _items.value.map { currentItem ->
                if (currentItem.id == item.id) currentItem.copy(owned = true)
                else currentItem
            }
            true
        } else {
            false
        }
    }
}

// Initial cosmetics data
private fun initialCosmetics() = listOf(
    CosmeticItem("1", "Cool Shades", 500, R.drawable.cosmetic_glasses),
    CosmeticItem("2", "Wizard Hat", 800, R.drawable.cosmetic_hat),
    CosmeticItem("3", "Red Scarf", 300, R.drawable.cosmetic_scarf),
    CosmeticItem("4", "Cyber Wings", 1200, R.drawable.cosmetic_wings),
    CosmeticItem("5", "Epic Aura", 1000, R.drawable.cosmetic_aura),
    CosmeticItem("6", "Hero Cape", 700, R.drawable.cosmetic_cape)
)
